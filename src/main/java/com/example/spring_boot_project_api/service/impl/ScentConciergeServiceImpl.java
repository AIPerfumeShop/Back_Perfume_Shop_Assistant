package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ai.ScentConciergeRequest;
import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.ScentConciergeResponse;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.repository.FragranceProfileRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ProductRepository.ProductRatingStat;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.repository.specification.ProductSpecification;
import com.example.spring_boot_project_api.service.OpenRouterService;
import com.example.spring_boot_project_api.service.ScentConciergeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class ScentConciergeServiceImpl implements ScentConciergeService {

    private static final Logger log = LoggerFactory.getLogger(ScentConciergeServiceImpl.class);

    private static final int CATALOG_CAP = 80;
    private static final int FALLBACK_LIMIT = 6;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-zA-Z0-9]+");

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final FragranceProfileRepository fragranceProfileRepository;
    private final OpenRouterService openRouterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final boolean aiEnabled;

    public ScentConciergeServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            FragranceProfileRepository fragranceProfileRepository,
            OpenRouterService openRouterService,
            @Value("${scent-concierge.ai-enabled:true}") boolean aiEnabled) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.fragranceProfileRepository = fragranceProfileRepository;
        this.openRouterService = openRouterService;
        this.aiEnabled = aiEnabled;
    }

    @Override
    public ScentConciergeResponse recommend(Long userId, ScentConciergeRequest request) {
        int limit = clampLimit(request.getLimit() == null ? 5 : request.getLimit());
        String description = request.getDescription().trim();

        List<Product> catalog = findCatalog();
        if (catalog.isEmpty()) {
            return emptyResponse("No perfumes are currently available in the catalog.");
        }

        if (aiEnabled) {
            ScentConciergeResponse ai = tryAiMatch(description, limit, catalog);
            if (ai != null) {
                return ai;
            }
        }

        return fallbackMatch(description, limit, catalog);
    }

    private List<Product> findCatalog() {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setInStock(Boolean.TRUE);
        Page<Product> page = productRepository.findAll(
                ProductSpecification.fromFilter(filter),
                PageRequest.of(0, CATALOG_CAP));
        return page.getContent();
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(10, limit));
    }

    private CatalogContext buildCatalogContext(List<Product> products) {
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, FragranceProfile> profileById = fragranceProfileRepository
                .findAllByProductIdIn(ids).stream()
                .collect(Collectors.toMap(fp -> fp.getProduct().getId(), Function.identity()));
        Map<Long, BigDecimal> priceById = prices(products);

        StringBuilder catalog = new StringBuilder("Product catalog (use the EXACT product names below):");
        for (Product product : products) {
            FragranceProfile profile = profileById.get(product.getId());
            BigDecimal price = priceById.get(product.getId());
            catalog.append("\n- ")
                    .append(product.getName())
                    .append(" (brand: ")
                    .append(product.getBrand() != null ? product.getBrand().getName() : "unknown")
                    .append(", price: ").append(price != null ? "$" + price : "unknown")
                    .append(getProfileLine(profile))
                    .append(")");
        }
        return new CatalogContext(catalog.toString(), profileById, priceById,
                products.stream().collect(Collectors.toMap(Product::getId, Function.identity())));
    }

    private String getProfileLine(FragranceProfile profile) {
        if (profile == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (profile.getGender() != null) {
            sb.append(", gender: ").append(profile.getGender().name().toLowerCase());
        }
        if (profile.getFragranceFamily() != null) {
            sb.append(", family: ").append(profile.getFragranceFamily());
        }
        if (profile.getFragNotes() != null) {
            sb.append(", notes: ").append(profile.getFragNotes());
        }
        return sb.toString();
    }

    private ScentConciergeResponse tryAiMatch(String description, int limit, List<Product> catalog) {
        CatalogContext context = buildCatalogContext(catalog);
        String aiText;
        try {
            AIMessage userMessage = new AIMessage();
            userMessage.setSender(MessageSender.USER);
            userMessage.setMessage(buildAiPrompt(description, context.catalog(), limit));
            aiText = openRouterService.generateResponse(List.of(userMessage), null);
        } catch (Exception ex) {
            log.warn("Scent concierge AI matching failed, falling back to keyword scoring", ex);
            return null;
        }
        if (aiText == null || aiText.isBlank()) {
            return null;
        }

        List<AIPick> picks = parsePicks(aiText);
        if (picks.isEmpty()) {
            return null;
        }

        Map<String, Product> byName = new LinkedHashMap<>();
        for (Product product : catalog) {
            byName.put(normalize(product.getName()), product);
        }

        List<Product> matched = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        for (AIPick pick : picks) {
            if (matched.size() >= limit) {
                break;
            }
            Product product = byName.get(normalize(pick.name()));
            if (product != null && !matched.contains(product)) {
                matched.add(product);
                reasons.add(pick.reason());
            }
        }
        if (matched.isEmpty()) {
            return null;
        }
        return buildResponse("I matched your needs to the closest perfumes in our collection.",
                matched, reasons, context);
    }

    private String buildAiPrompt(String description, String catalog, int limit) {
        return "You are the Scent Concierge of Blossom Fragrance perfume shop. "
                + "A customer has described what they are looking for in natural language. "
                + "Choose up to " + limit + " perfumes from the catalog below that best match. "
                + "Respond with STRICT JSON only, in this exact shape:\n"
                + "{\"matches\":[{\"name\":\"<exact product name>\",\"reason\":\"<one short sentence>\"}]}\n"
                + catalog + "\n\nCustomer request: \"" + description + "\"";
    }

    private List<AIPick> parsePicks(String aiText) {
        String trimmed = aiText.trim();
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        try {
            Map<String, List<Map<String, String>>> root =
                    objectMapper.readValue(trimmed, new TypeReference<>() {});
            List<Map<String, String>> matches = root.get("matches");
            if (matches == null) {
                return List.of();
            }
            List<AIPick> picks = new ArrayList<>();
            for (Map<String, String> entry : matches) {
                String name = entry.get("name");
                String reason = entry.get("reason");
                if (name != null && !name.isBlank()) {
                    picks.add(new AIPick(name.trim(), reason == null ? "" : reason.trim()));
                }
            }
            return picks;
        } catch (Exception ex) {
            log.warn("Scent concierge AI output was not valid JSON, falling back", ex);
            return List.of();
        }
    }

    private ScentConciergeResponse fallbackMatch(String description, int limit, List<Product> catalog) {
        CatalogContext context = buildCatalogContext(catalog);
        Map<Long, Double> avgRateById = ratingById(catalog);
        List<String> tokens = tokens(description);

        List<Product> scored = catalog.stream()
                .filter(product -> isBuyable(product, context.priceById()))
                .sorted(Comparator
                        .comparingDouble((Product p) -> score(tokens, context.profileById().get(p.getId()),
                                p, avgRateById.getOrDefault(p.getId(), 0.0))).reversed()
                        .thenComparing(p -> p.getId()))
                .toList();

        List<Product> matches = scored.subList(0, Math.min(limit, scored.size()));
        if (matches.isEmpty()) {
            return emptyResponse("No perfumes are currently available that match your description.");
        }
        List<String> reasons = new ArrayList<>();
        for (Product product : matches) {
            if (avgRateById.getOrDefault(product.getId(), 0.0) >= 4.0) {
                reasons.add("Highly rated and matches the vibe you described.");
            } else {
                reasons.add("Close match based on your description.");
            }
        }
        return buildResponse("Here are the perfumes I found for you based on your description.",
                matches, reasons, context);
    }

    private double score(List<String> tokens, FragranceProfile profile, Product product, double avgRate) {
        if (tokens.isEmpty()) {
            return avgRate / 5.0;
        }
        double matched = 0;
        String family = profile == null || profile.getFragranceFamily() == null
                ? "" : profile.getFragranceFamily().toLowerCase(Locale.ROOT);
        String notes = profile == null || profile.getFragNotes() == null
                ? "" : profile.getFragNotes().toLowerCase(Locale.ROOT);
        String name = product.getName() == null ? "" : product.getName().toLowerCase(Locale.ROOT);
        String brand = product.getBrand() == null || product.getBrand().getName() == null
                ? "" : product.getBrand().getName().toLowerCase(Locale.ROOT);
        String gender = profile == null || profile.getGender() == null
                ? "" : profile.getGender().name().toLowerCase(Locale.ROOT);

        String haystack = family + " " + notes + " " + name + " " + brand + " " + gender;
        for (String token : tokens) {
            if (haystack.contains(token)) {
                matched++;
            }
        }
        double keywordPart = matched / Math.max(1, tokens.size());
        double ratingPart = (avgRate / 5.0) * 0.3;
        return keywordPart + ratingPart;
    }

    private List<String> tokens(String text) {
        return TOKEN_SPLIT.splitAsStream(text.toLowerCase(Locale.ROOT))
                .filter(t -> t.length() > 2)
                .filter(t -> !STOPWORDS.contains(t))
                .distinct()
                .toList();
    }

    private static final java.util.Set<String> STOPWORDS = java.util.Set.of(
            "the", "and", "for", "with", "that", "this", "from", "which", "want", "would",
            "have", "some", "what", "like", "you", "your", "look", "smell", "best", "perfume");

    private boolean isBuyable(Product product, Map<Long, BigDecimal> priceById) {
        return priceById.containsKey(product.getId()) && Boolean.TRUE.equals(product.getIsActive());
    }

    private Map<Long, BigDecimal> prices(List<Product> products) {
        List<Long> ids = products.stream().map(Product::getId).toList();
        return productVariantRepository.findAllByProductIdIn(ids).stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive())
                        && v.getStock() != null && v.getStock() > 0)
                .collect(Collectors.groupingBy(v -> v.getProduct().getId(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(ProductVariant::getPrice,
                                        Collectors.filtering(Objects::nonNull,
                                                Collectors.minBy(Comparator.naturalOrder()))),
                                o -> o.orElse(null))));
    }

    private Map<Long, Double> ratingById(List<Product> products) {
        List<Long> ids = products.stream().map(Product::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productRepository.findRatingStats(ids).stream()
                .collect(Collectors.toMap(
                        ProductRatingStat::getProductId,
                        s -> s.getAvgRate() == null ? 0.0 : s.getAvgRate()));
    }

    private ScentConciergeResponse buildResponse(String summary, List<Product> products,
                                                 List<String> reasons, CatalogContext context) {
        ScentConciergeResponse response = new ScentConciergeResponse();
        response.setSummary(summary);
        List<ScentConciergeResponse.Match> matches = new ArrayList<>();
        Map<Long, Double> avgRateById = ratingById(products);
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            ScentConciergeResponse.Match match = new ScentConciergeResponse.Match();
            match.setProductId(product.getId());
            match.setProductName(product.getName());
            match.setBrand(product.getBrand() != null ? product.getBrand().getName() : null);
            match.setPrice(context.priceById().get(product.getId()));
            match.setAverageRate(avgRateById.get(product.getId()));
            FragranceProfile profile = context.profileById().get(product.getId());
            if (profile != null) {
                match.setFragranceFamily(profile.getFragranceFamily());
                match.setFragNotes(profile.getFragNotes());
            }
            match.setReason(i < reasons.size() ? reasons.get(i) : null);
            match.setPosition(i + 1);
            matches.add(match);
        }
        response.setMatches(matches);
        response.setTotalMatches(matches.size());
        return response;
    }

    private ScentConciergeResponse emptyResponse(String message) {
        ScentConciergeResponse response = new ScentConciergeResponse();
        response.setSummary(message);
        response.setMatches(List.of());
        response.setTotalMatches(0);
        return response;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record AIPick(String name, String reason) {
    }

    private record CatalogContext(
            String catalog,
            Map<Long, FragranceProfile> profileById,
            Map<Long, BigDecimal> priceById,
            Map<Long, Product> byId) {
    }
}