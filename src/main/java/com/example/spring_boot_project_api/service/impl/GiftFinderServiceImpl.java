package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.gift.GiftFinderRequest;
import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.response.gift.GiftFinderAIExplanationDTO;
import com.example.spring_boot_project_api.dto.response.gift.GiftFinderResponse;
import com.example.spring_boot_project_api.dto.response.gift.GiftRecommendationDTO;
import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.enums.GiftConfidence;
import com.example.spring_boot_project_api.enums.GiftKnowledgeLevel;
import com.example.spring_boot_project_api.enums.GiftOccasion;
import com.example.spring_boot_project_api.enums.GiftPersonalityVibe;
import com.example.spring_boot_project_api.enums.GiftRecipientType;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.enums.ScentPreference;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.repository.FragranceProfileRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ProductRepository.ProductRatingStat;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.repository.specification.ProductSpecification;
import com.example.spring_boot_project_api.service.GiftFinderService;
import com.example.spring_boot_project_api.service.OpenRouterService;

@Service
@Transactional(readOnly = true)
public class GiftFinderServiceImpl implements GiftFinderService {

    private static final Logger log = LoggerFactory.getLogger(GiftFinderServiceImpl.class);

    private static final int CANDIDATE_CAP = 60;
    private static final int ALTERNATIVES = 2;

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final FragranceProfileRepository fragranceProfileRepository;
    private final OpenRouterService openRouterService;
    private final boolean aiExplanationEnabled;

    public GiftFinderServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            FragranceProfileRepository fragranceProfileRepository,
            OpenRouterService openRouterService,
            @Value("${gift-finder.ai-explanation-enabled:true}") boolean aiExplanationEnabled) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.fragranceProfileRepository = fragranceProfileRepository;
        this.openRouterService = openRouterService;
        this.aiExplanationEnabled = aiExplanationEnabled;
    }

    @Override
    public GiftFinderResponse recommend(GiftFinderRequest request) {
        validate(request);

        List<ScoredGift> exact = scoreCandidates(request, findCandidates(request.getBudgetMin(), request.getBudgetMax()));
        if (!exact.isEmpty()) {
            return buildResponse(request, exact, true, null);
        }

        List<ScoredGift> relaxed = scoreCandidates(request, findCandidates(null, null));
        if (relaxed.isEmpty()) {
            GiftFinderResponse empty = new GiftFinderResponse();
            empty.setExactMatchFound(false);
            empty.setMessage("No perfumes are currently available for these preferences.");
            return empty;
        }

        return buildResponse(request, relaxed, false,
                "No perfumes were found within the selected budget. Showing the closest alternatives instead.");
    }

    private void validate(GiftFinderRequest request) {
        if (request.getBudgetMin() != null && request.getBudgetMax() != null
                && request.getBudgetMin().compareTo(request.getBudgetMax()) > 0) {
            throw new BadRequestException("budgetMin cannot be greater than budgetMax");
        }
    }

    private List<Product> findCandidates(BigDecimal minPrice, BigDecimal maxPrice) {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setInStock(Boolean.TRUE);
        Page<Product> page = productRepository.findAll(
                ProductSpecification.fromFilter(filter),
                PageRequest.of(0, CANDIDATE_CAP));
        return page.getContent();
    }

    private List<ScoredGift> scoreCandidates(GiftFinderRequest request, List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> ids = products.stream().map(Product::getId).toList();

        Map<Long, List<ProductVariant>> variantsByProduct = productVariantRepository
                .findAllByProductIdIn(ids).stream()
                .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        Map<Long, FragranceProfile> profileByProduct = fragranceProfileRepository
                .findAllByProductIdIn(ids).stream()
                .collect(Collectors.toMap(fp -> fp.getProduct().getId(), Function.identity()));

        Map<Long, Double> avgRateById = Collections.emptyMap();
        Map<Long, Long> reviewCountById = Collections.emptyMap();
        if (!ids.isEmpty()) {
            Map<Long, ProductRatingStat> stats = productRepository.findRatingStats(ids).stream()
                    .collect(Collectors.toMap(
                            ProductRatingStat::getProductId,
                            Function.identity(),
                            (a, b) -> a));
            avgRateById = stats.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                        Double avg = e.getValue().getAvgRate();
                        return avg == null ? 0.0 : avg;
                    }));
            reviewCountById = stats.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                        Long c = e.getValue().getReviewCount();
                        return c == null ? 0L : c;
                    }));
        }

        List<ScoredGift> scored = new ArrayList<>();
        for (Product product : products) {
            List<ProductVariant> buyable = variantsByProduct
                    .getOrDefault(product.getId(), List.of()).stream()
                    .filter(GiftFinderServiceImpl::isPurchasable)
                    .toList();
            if (buyable.isEmpty()) {
                continue;
            }
            BigDecimal price = buyable.stream()
                    .map(ProductVariant::getPrice)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            FragranceProfile profile = profileByProduct.get(product.getId());
            GiftScores scores = score(request, profile, price,
                    avgRateById.getOrDefault(product.getId(), 0.0),
                    reviewCountById.getOrDefault(product.getId(), 0L));
            scored.add(new ScoredGift(
                    product,
                    price,
                    scores,
                    avgRateById.getOrDefault(product.getId(), 0.0),
                    reviewCountById.getOrDefault(product.getId(), 0L)));
        }

        scored.sort(Comparator
                .comparingInt((ScoredGift s) -> s.scores.total).reversed()
                .thenComparing(Comparator.comparingDouble((ScoredGift s) -> s.avgRate).reversed())
                .thenComparingLong((ScoredGift s) -> -s.reviewCount)
                .thenComparingLong(s -> s.product.getId()));
        return scored;
    }

    private static boolean isPurchasable(ProductVariant variant) {
        return Boolean.TRUE.equals(variant.getIsActive())
                && variant.getStock() != null
                && variant.getStock() > 0;
    }

    private GiftFinderResponse buildResponse(GiftFinderRequest request, List<ScoredGift> scored,
                                             boolean exactMatch, String message) {
        GiftFinderResponse response = new GiftFinderResponse();
        response.setExactMatchFound(exactMatch);
        response.setMessage(message);

        List<GiftRecommendationDTO> recommendations = new ArrayList<>();
        for (ScoredGift gift : scored) {
            recommendations.add(toRecommendation(request, gift));
        }

        if (recommendations.isEmpty()) {
            return response;
        }

        if (exactMatch) {
            response.setTopRecommendation(recommendations.get(0));
            int from = Math.min(1, recommendations.size());
            int to = Math.min(recommendations.size(), from + ALTERNATIVES);
            response.setAlternatives(new ArrayList<>(recommendations.subList(from, to)));
            response.setAiExplanation(explain(request,
                    recommendations.get(0), scored.get(0).product, scored.get(0).price));
        } else {
            int to = Math.min(recommendations.size(), ALTERNATIVES + 1);
            response.setAlternatives(new ArrayList<>(recommendations.subList(0, to)));
        }

        return response;
    }

    private GiftRecommendationDTO toRecommendation(GiftFinderRequest request, ScoredGift gift) {
        GiftRecommendationDTO dto = new GiftRecommendationDTO();
        dto.setProductId(gift.product.getId());
        dto.setProductName(gift.product.getName());
        dto.setBrand(gift.product.getBrand() != null ? gift.product.getBrand().getName() : null);
        dto.setPrice(gift.price);
        dto.setMatchScore(gift.scores.total);
        dto.setGiftConfidence(confidence(gift.scores.total));
        dto.setReasons(buildReasons(request, gift));
        return dto;
    }

    // =========================================================
    // SCORING (deterministic, weights sum to 100)
    // =========================================================

    private GiftScores score(GiftFinderRequest request, FragranceProfile profile,
                             BigDecimal price, double avgRate, long reviewCount) {
        String family = profile == null || profile.getFragranceFamily() == null
                ? null : profile.getFragranceFamily().toLowerCase();
        String notes = profile == null || profile.getFragNotes() == null
                ? null : profile.getFragNotes().toLowerCase();
        Gender gender = profile == null ? null : profile.getGender();

        int budget = budgetScore(request.getBudgetMin(), request.getBudgetMax(), price);
        int recipient = recipientScore(request.getRecipientType(), gender);
        int occasion = occasionScore(request.getOccasion(), family, notes);
        int personality = personalityScore(request.getPersonalityVibes(), family, notes);
        int scent = scentScore(request, family, notes);
        int performance = performanceScore(avgRate, reviewCount);

        return new GiftScores(budget, recipient, occasion, personality, scent, performance,
                budget + recipient + occasion + personality + scent + performance);
    }

    private int budgetScore(BigDecimal min, BigDecimal max, BigDecimal price) {
        if (price == null) {
            return 5;
        }
        if (price.compareTo(min) >= 0 && price.compareTo(max) <= 0) {
            return 10;
        }
        // Only reached via relaxed search: near-budget products keep partial credit.
        BigDecimal ceiling = max.max(min);
        if (price.compareTo(ceiling) <= 0) {
            return 8;
        }
        BigDecimal base = ceiling.max(BigDecimal.ONE);
        double overshoot = price.subtract(ceiling).divide(base, 4, RoundingMode.HALF_UP).doubleValue();
        return (int) Math.max(3, Math.round(10 - overshoot * 10));
    }

    private Gender expectedGender(GiftRecipientType recipientType) {
        return switch (recipientType) {
            case MOTHER -> Gender.WOMEN;
            case FATHER -> Gender.MEN;
            default -> Gender.UNISEX;
        };
    }

    private int recipientScore(GiftRecipientType recipientType, Gender actual) {
        Gender expected = expectedGender(recipientType);
        if (actual == null) {
            return 15;
        }
        if (expected == Gender.UNISEX) {
            return 15;
        }
        return actual == expected ? 25 : 10;
    }

    private static final Map<GiftOccasion, List<String>> OCCASION_KEYWORDS = Map.of(
            GiftOccasion.BIRTHDAY, List.of("floral", "gourmand", "sweet", "fruity"),
            GiftOccasion.ANNIVERSARY, List.of("floral", "oriental", "amber", "rose"),
            GiftOccasion.VALENTINE, List.of("floral", "rose", "gourmand", "sweet"),
            GiftOccasion.GRADUATION, List.of("fresh", "citrus", "aquatic", "green"),
            GiftOccasion.WEDDING, List.of("powder", "floral", "amber"),
            GiftOccasion.HOLIDAY, List.of("woody", "amber", "fresh", "spicy"),
            GiftOccasion.JUST_BECAUSE, List.of(),
            GiftOccasion.OTHER, List.of());

    private int occasionScore(GiftOccasion occasion, String family, String notes) {
        List<String> keywords = OCCASION_KEYWORDS.getOrDefault(occasion, List.of());
        if (keywords.isEmpty()) {
            return 10;
        }
        return matchesAny(family, notes, keywords) ? 15 : 3;
    }

    private static final Map<GiftPersonalityVibe, List<String>> VIBE_KEYWORDS = Map.of(
            GiftPersonalityVibe.ELEGANT, List.of("powder", "floral", "oriental", "amber"),
            GiftPersonalityVibe.CUTE, List.of("sweet", "fruity", "gourmand"),
            GiftPersonalityVibe.PLAYFUL, List.of("fruity", "gourmand", "sweet"),
            GiftPersonalityVibe.CLEAN, List.of("fresh", "aquatic", "green"),
            GiftPersonalityVibe.FRESH, List.of("fresh", "citrus", "aquatic", "green"),
            GiftPersonalityVibe.CONFIDENT, List.of("woody", "spicy", "leather", "amber"),
            GiftPersonalityVibe.MYSTERIOUS, List.of("oriental", "spicy", "leather", "woody"),
            GiftPersonalityVibe.ROMANTIC, List.of("floral", "rose", "powder", "gourmand"),
            GiftPersonalityVibe.CALM, List.of("green", "aquatic", "powder", "lavender"),
            GiftPersonalityVibe.LUXURIOUS, List.of("amber", "leather", "oriental", "oud"));

    private int personalityScore(List<GiftPersonalityVibe> vibes, String family, String notes) {
        List<GiftPersonalityVibe> selected = vibes == null ? List.of()
                : vibes.stream().filter(Objects::nonNull).toList();
        if (selected.isEmpty()) {
            return 18;
        }
        long matched = selected.stream()
                .filter(v -> matchesAny(family, notes, VIBE_KEYWORDS.getOrDefault(v, List.of())))
                .count();
        return (int) Math.round(30.0 * matched / selected.size());
    }

    private static final Map<ScentPreference, List<String>> SCENT_KEYWORDS = Map.of(
            ScentPreference.SWEET, List.of("sweet", "gourmand", "vanilla"),
            ScentPreference.FLORAL, List.of("floral", "rose"),
            ScentPreference.FRUITY, List.of("fruity"),
            ScentPreference.FRESH, List.of("fresh", "citrus", "aquatic", "green"),
            ScentPreference.WOODY, List.of("woody"),
            ScentPreference.VANILLA, List.of("vanilla", "gourmand", "amber"),
            ScentPreference.MUSKY, List.of("musk", "woody"),
            ScentPreference.CITRUS, List.of("citrus"));

    private int scentScore(GiftFinderRequest request, String family, String notes) {
        if (request.getKnowledgeLevel() == GiftKnowledgeLevel.NOT_REALLY) {
            return 6;
        }
        List<ScentPreference> prefs = request.getScentPreference() == null ? List.of()
                : request.getScentPreference().stream()
                        .filter(Objects::nonNull)
                        .filter(p -> p != ScentPreference.NO_PREFERENCE)
                        .toList();
        if (prefs.isEmpty()) {
            return 6;
        }
        boolean matched = prefs.stream().anyMatch(p ->
                matchesAny(family, notes, SCENT_KEYWORDS.getOrDefault(p, List.of())));
        return matched ? 10 : 0;
    }

    private int performanceScore(double avgRate, long reviewCount) {
        if (reviewCount <= 0) {
            return 5;
        }
        double ratingPart = (avgRate / 5.0) * 0.5;
        double countPart = Math.min(1.0, reviewCount / 20.0) * 0.5;
        return (int) Math.max(0, Math.min(10, Math.round(10 * (ratingPart + countPart))));
    }

    private boolean matchesAny(String family, String notes, List<String> keywords) {
        String haystack = (family == null ? "" : family) + " " + (notes == null ? "" : notes);
        if (haystack.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (haystack.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private GiftConfidence confidence(int score) {
        if (score >= 80) {
            return GiftConfidence.HIGH;
        }
        if (score >= 60) {
            return GiftConfidence.MEDIUM;
        }
        return GiftConfidence.LOW;
    }

    private List<String> buildReasons(GiftFinderRequest request, ScoredGift gift) {
        GiftScores s = gift.scores;
        List<String> reasons = new ArrayList<>();
        if (s.budget >= 8) {
            reasons.add("Fits the selected budget");
        }
        if (s.recipient >= 20) {
            reasons.add("Suitable for the recipient's taste");
        }
        if (s.occasion == 15) {
            reasons.add("Great for a " + occasionPhrase(request.getOccasion()) + " gift");
        }
        if (request.getPersonalityVibes() != null && !request.getPersonalityVibes().isEmpty()
                && s.personality >= 15) {
            reasons.add("Matches " + vibePhrase(request.getPersonalityVibes()) + " style");
        }
        if (s.scent == 10) {
            reasons.add("Matches the selected scent preference");
        }
        if (request.getKnowledgeLevel() == GiftKnowledgeLevel.NOT_REALLY) {
            reasons.add("Picked for broad appeal when the recipient's taste is unknown");
        }
        if (reasons.isEmpty()) {
            reasons.add("A well-rounded option for this request");
        }
        return reasons.stream().distinct().limit(4).toList();
    }

    private String occasionPhrase(GiftOccasion occasion) {
        return switch (occasion) {
            case BIRTHDAY -> "birthday";
            case ANNIVERSARY -> "anniversary";
            case VALENTINE -> "romantic occasion";
            case GRADUATION -> "graduation";
            case WEDDING -> "wedding";
            case HOLIDAY -> "holiday";
            case JUST_BECAUSE -> "just-because";
            case OTHER -> "special";
        };
    }

    private String vibePhrase(List<GiftPersonalityVibe> vibes) {
        return vibes.stream()
                .filter(Objects::nonNull)
                .map(v -> v.name().toLowerCase())
                .collect(Collectors.joining(" and "));
    }

    // =========================================================
    // OPTIONAL AI EXPLANATION
    // =========================================================

    private GiftFinderAIExplanationDTO explain(GiftFinderRequest request, GiftRecommendationDTO top,
                                               Product product, BigDecimal price) {
        List<String> reasons = top == null ? List.of() : top.getReasons();
        if (!aiExplanationEnabled) {
            return null;
        }
        String fallback = "This perfume is a strong match for a "
                + occasionPhrase(request.getOccasion()) + " gift"
                + (hasVibes(request)
                        ? " with a " + vibePhrase(request.getPersonalityVibes()) + " style."
                        : ".")
                + " It fits the selected budget.";
        try {
            String prompt = buildAiPrompt(request, product, price);
            AIMessage userMessage = new AIMessage();
            userMessage.setSender(MessageSender.USER);
            userMessage.setMessage(prompt);
            String aiText = openRouterService.generateResponse(List.of(userMessage), null);
            if (aiText != null && !aiText.isBlank()) {
                GiftFinderAIExplanationDTO dto = new GiftFinderAIExplanationDTO();
                dto.setSummary(aiText);
                dto.setReasons(reasons);
                return dto;
            }
        } catch (Exception ex) {
            log.warn("Gift Finder AI explanation failed, using deterministic summary", ex);
        }
        GiftFinderAIExplanationDTO dto = new GiftFinderAIExplanationDTO();
        dto.setSummary(fallback);
        dto.setReasons(reasons);
        return dto;
    }

    private boolean hasVibes(GiftFinderRequest request) {
        return request.getPersonalityVibes() != null && !request.getPersonalityVibes().isEmpty();
    }

    private String buildAiPrompt(GiftFinderRequest request, Product product, BigDecimal price) {
        FragranceProfile profile = product.getFragranceProfile();
        String seenPreferences = request.getKnowledgeLevel() == GiftKnowledgeLevel.NOT_REALLY
                || request.getScentPreference() == null || request.getScentPreference().isEmpty()
                        ? "none selected"
                        : request.getScentPreference().stream()
                                .filter(p -> p != null && p != ScentPreference.NO_PREFERENCE)
                                .map(Enum::name)
                                .map(String::toLowerCase)
                                .collect(Collectors.joining(", "));
        if (seenPreferences.isBlank()) {
            seenPreferences = "none selected";
        }
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a perfume gift advisor. Explain why the recommended perfume suits this gift. ")
                .append("Use ONLY the facts provided below. Never invent notes, prices, brands, or qualities that are not listed. ")
                .append("Reply in 2-3 short sentences.\n");
        prompt.append("Recipient type: ").append(request.getRecipientType()).append('\n');
        prompt.append("Occasion: ").append(request.getOccasion()).append('\n');
        prompt.append("Personality styles: ").append(hasVibes(request)
                ? vibePhrase(request.getPersonalityVibes()) : "none selected").append('\n');
        prompt.append("Buyer knows the recipient's perfume taste: ")
                .append(request.getKnowledgeLevel().name().replace('_', ' ').toLowerCase()).append('\n');
        prompt.append("Budget range: $").append(request.getBudgetMin()).append(" - $")
                .append(request.getBudgetMax()).append('\n');
        prompt.append("Scent preference: ").append(seenPreferences).append('\n');
        prompt.append("Recommended perfume: '").append(product.getName()).append("'")
                .append(product.getBrand() != null ? " by " + product.getBrand().getName() : "")
                .append(", price $").append(price);
        if (profile != null) {
            if (profile.getFragranceFamily() != null) {
                prompt.append(", fragrance family ").append(profile.getFragranceFamily());
            }
            if (profile.getFragNotes() != null) {
                prompt.append(", notes: ").append(profile.getFragNotes());
            }
        }
        return prompt.toString();
    }

    private record GiftScores(
            int budget,
            int recipient,
            int occasion,
            int personality,
            int scent,
            int performance,
            int total) {
    }

    private static final class ScoredGift {
        private final Product product;
        private final BigDecimal price;
        private final GiftScores scores;
        private final double avgRate;
        private final long reviewCount;

        private ScoredGift(Product product, BigDecimal price, GiftScores scores,
                           double avgRate, long reviewCount) {
            this.product = product;
            this.price = price;
            this.scores = scores;
            this.avgRate = avgRate;
            this.reviewCount = reviewCount;
        }
    }
}