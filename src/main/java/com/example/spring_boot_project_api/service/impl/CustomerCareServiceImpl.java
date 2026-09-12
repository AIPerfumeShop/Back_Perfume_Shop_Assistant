package com.example.spring_boot_project_api.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.cs.CsChatRequest;
import com.example.spring_boot_project_api.dto.request.cs.HandoffRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.dto.response.cs.CsChatResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportQueueResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketDetailResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketNoteResponse;
import com.example.spring_boot_project_api.dto.response.cs.SupportTicketResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderItemResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.TicketPriority;
import com.example.spring_boot_project_api.enums.TicketStatus;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.AIMapper;
import com.example.spring_boot_project_api.mapper.OrderMapper;
import com.example.spring_boot_project_api.mapper.SupportTicketMapper;
import com.example.spring_boot_project_api.model.AIConversation;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.SupportTicket;
import com.example.spring_boot_project_api.model.SupportTicketNote;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AIConversationRepository;
import com.example.spring_boot_project_api.repository.AIMessageRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.SupportTicketNoteRepository;
import com.example.spring_boot_project_api.repository.SupportTicketRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.CustomerCareService;
import com.example.spring_boot_project_api.service.OpenRouterService;
import com.example.spring_boot_project_api.service.OrderService;
import com.example.spring_boot_project_api.service.TelegramService;

@Service
@Transactional
public class CustomerCareServiceImpl implements CustomerCareService {

    private static final int MAX_HISTORY_SIZE = 30;
    private static final int SUMMARY_MESSAGE_COUNT = 6;
    private static final Pattern ORDER_ID_PATTERN =
            Pattern.compile("(?i)(?:ord[- ]?|order\\s*#?|order id(?:\\s+is)?|#)\\s*(\\d+)");
    private static final List<TicketStatus> ACTIVE_STATUSES =
            List.of(TicketStatus.OPEN, TicketStatus.PENDING, TicketStatus.IN_PROGRESS);

    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketNoteRepository noteRepository;
    private final AIConversationRepository aiConversationRepository;
    private final AIMessageRepository aiMessageRepository;
    private final UserRepository userRepository;
    private final AIMapper aiMapper;
    private final SupportTicketMapper supportTicketMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OpenRouterService openRouterService;
    private final TelegramService telegramService;

    @Value("${customer-care.offline:false}")
    private boolean teamOffline;

    public CustomerCareServiceImpl(
            SupportTicketRepository supportTicketRepository,
            SupportTicketNoteRepository noteRepository,
            AIConversationRepository aiConversationRepository,
            AIMessageRepository aiMessageRepository,
            UserRepository userRepository,
            AIMapper aiMapper,
            SupportTicketMapper supportTicketMapper,
            OrderMapper orderMapper,
            OrderService orderService,
            OrderRepository orderRepository,
            OpenRouterService openRouterService,
            TelegramService telegramService) {
        this.supportTicketRepository = supportTicketRepository;
        this.noteRepository = noteRepository;
        this.aiConversationRepository = aiConversationRepository;
        this.aiMessageRepository = aiMessageRepository;
        this.userRepository = userRepository;
        this.aiMapper = aiMapper;
        this.supportTicketMapper = supportTicketMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.openRouterService = openRouterService;
        this.telegramService = telegramService;
    }

    private enum Intent {
        ORDER_STATUS,
        CANCEL_ORDER,
        REFUND,
        PAYMENT,
        SHIPPING,
        ACCOUNT,
        COMPLAINT,
        PRODUCT,
        GENERAL
    }

    private record OrderLookup(String context, boolean resolved) {
    }

    private record CancelOutcome(String context, boolean resolved, boolean handoff) {
    }

    // =========================================================
    // CHAT
    // =========================================================

    @Override
    public CsChatResponse chat(Long userId, CsChatRequest request) {
        User user = findUser(userId);

        AIConversation conversation =
                resolveConversation(userId, request.getConversationId(), request.getMessage());

        aiMessageRepository.save(
                aiMapper.toMessageEntity(request.getMessage(), MessageSender.USER, conversation));

        String text = request.getMessage();
        Intent intent = detectIntent(text);

        String toolContext = null;
        String canned = null;
        boolean resolved = false;
        boolean handoff = false;
        boolean redirect = false;

        switch (intent) {
            case ORDER_STATUS -> {
                OrderLookup lookup = resolveOrderLookup(userId, text);
                toolContext = lookup.context();
                resolved = lookup.resolved();
            }
            case CANCEL_ORDER -> {
                CancelOutcome outcome = resolveCancel(userId, text);
                toolContext = outcome.context();
                resolved = outcome.resolved();
                handoff = outcome.handoff();
                if (toolContext == null) {
                    canned = "I can help cancel an order! 💗 Could you share your order number? It looks like ORD- followed by a few digits.";
                }
            }
            case REFUND -> {
                canned = "I'm sorry to hear that! 💕 We accept returns and refunds for eligible orders, and I can start the process for you. "
                        + "For a damaged or incorrect item, I'll connect you straight to a customer-care specialist.";
                handoff = true;
            }
            case PAYMENT -> {
                canned = "I can help with payments! For a failed or unclear payment, please check the order status in My Orders, "
                        + "and if it still looks wrong I'll connect you to a specialist.";
                handoff = false;
            }
            case SHIPPING -> {
                canned = "I can help with delivery! 🚚 For delivery delays or a wrong address, I'll connect you to a specialist "
                        + "so they can check it right away.";
                handoff = true;
            }
            case ACCOUNT -> {
                canned = "I can point you in the right direction for your account! You can update your profile on your account page. "
                        + "For password or account access problems, I'll connect you to a specialist.";
                handoff = false;
            }
            case COMPLAINT -> {
                canned = "I'm really sorry you've had a bad experience. 💗 Your feedback matters, and a customer-care specialist will take care of this personally.";
                handoff = true;
            }
            case PRODUCT -> {
                canned = "I specialise in orders, payments, delivery, returns and account help. 💕 For finding your perfect scent, "
                        + "Blossom AI is the expert — would you like me to take you there?";
                redirect = true;
            }
            case GENERAL ->
                canned = "I'm here to help with anything about your Blossom shopping experience — orders, payments, delivery, returns, or account issues. What's going on? 💕";
        }

        String aiText;
        if (toolContext != null) {
            aiText = answerWithToolContext(conversation, toolContext);
            if (aiText == null) {
                aiText = "Here's what I found: " + toolContext;
            }
        } else {
            aiText = canned;
        }

        AIMessage aiMessage = aiMessageRepository.save(
                aiMapper.toMessageEntity(aiText, MessageSender.AI, conversation));

        conversation.touch();
        aiConversationRepository.save(conversation);

        CsChatResponse response = new CsChatResponse();
        response.setConversationId(conversation.getId());
        response.setMessageId(aiMessage.getId());
        response.setMessage(aiMessage.getMessage());
        response.setHandoffSuggested(handoff);
        response.setRedirectToAIAssistant(redirect);
        response.setCreatedAt(aiMessage.getCreatedAt());
        return response;
    }

    private String answerWithToolContext(AIConversation conversation, String toolContext) {
        List<AIMessage> history =
                aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
        }

        List<AIMessage> context = new java.util.ArrayList<>(history);
        context.add(aiMapper.toMessageEntity(
                "Order system context: " + toolContext
                        + " Reply to the customer helpfully and naturally.",
                MessageSender.AI,
                conversation));

        try {
            return openRouterService.generateResponse(context);
        } catch (Exception ex) {
            return null;
        }
    }

    // =========================================================
    // HANDOFF
    // =========================================================

    @Override
    public SupportTicketResponse handoff(Long userId, HandoffRequest request) {
        User user = findUser(userId);

        AIConversation conversation;
        if (request.getConversationId() != null) {
            conversation = aiConversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
            if (!conversation.getUser().getId().equals(userId)) {
                throw new ForbiddenException("You do not have access to this conversation");
            }
        } else {
            conversation = new AIConversation();
            conversation.setUser(user);
            conversation.setUserName(user.getName());
            conversation.setTitle("Support request");
            conversation = aiConversationRepository.save(conversation);
        }

        supportTicketRepository
                .findFirstByConversationIdAndStatusNot(conversation.getId(), TicketStatus.RESOLVED)
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "A support request already exists for this conversation: "
                                    + existing.getTicketNumber());
                });

        String summary = buildSummary(conversation);
        String combined = (request.getReason() == null ? "" : request.getReason()) + " " + summary;

        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setConversation(conversation);
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setReason(request.getReason());
        ticket.setSummary(summary);
        ticket.setOrderId(extractOrderId(combined));
        ticket.setPriority(detectPriority(combined));
        ticket.setStatus(teamOffline ? TicketStatus.PENDING : TicketStatus.OPEN);

        ticket = supportTicketRepository.save(ticket);

        telegramService.sendMessage("👩‍💼 New support request "
                + ticket.getTicketNumber()
                + " (" + ticket.getPriority() + ")\n"
                + "Customer: " + user.getName()
                + "\nReason: " + (request.getReason() == null ? "-" : request.getReason()));

        return supportTicketMapper.toResponse(ticket);
    }

    // =========================================================
    // CUSTOMER TICKETS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getCustomerTickets(Long userId) {
        findUser(userId);
        return supportTicketMapper.toResponseList(
                supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketDetailResponse getCustomerTicket(Long userId, Long ticketId) {
        SupportTicket ticket = findTicket(ticketId);
        checkOwnership(ticket, userId);

        List<AIMessageResponse> messages = loadMessages(ticket);
        //Internal notes are for agents only - never shown to the customer.
        return supportTicketMapper.toDetailResponse(
                ticket, safeOrderForCustomer(ticket, userId), messages, java.util.List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketDetailResponse getAgentTicketContext(Long ticketId) {
        SupportTicket ticket = findTicket(ticketId);

        List<AIMessageResponse> messages = loadMessages(ticket);
        List<SupportTicketNoteResponse> notes = supportTicketMapper.toNoteResponseList(
                noteRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()));

        return supportTicketMapper.toDetailResponse(ticket, safeOrder(ticket), messages, notes);
    }

    private OrderResponse safeOrder(SupportTicket ticket) {
        if (ticket.getOrderId() == null) {
            return null;
        }
        return orderRepository.findById(ticket.getOrderId())
                .map(orderMapper::toResponse)
                .orElse(null);
    }

    private OrderResponse safeOrderForCustomer(SupportTicket ticket, Long userId) {
        if (ticket.getOrderId() == null) {
            return null;
        }
        return orderRepository.findById(ticket.getOrderId())
                .filter(o -> belongsTo(o, userId))
                .map(orderMapper::toResponse)
                .orElse(null);
    }

    // =========================================================
    // AGENT / CUSTOMER REPLIES
    // =========================================================

    @Override
    public AIMessageResponse replyAsAgent(Long ticketId, String message, Long agentId) {
        SupportTicket ticket = findTicket(ticketId);
        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new BadRequestException("Ticket is already resolved");
        }

        AIConversation conversation = ensureConversation(ticket);

        if (agentId != null) {
            ticket.setAgentId(agentId);
            ticket.setAgentName(userRepository.findById(agentId)
                    .map(User::getName)
                    .orElse("Customer Care Team"));
        } else if (ticket.getAgentName() == null || ticket.getAgentName().isBlank()) {
            ticket.setAgentName("Customer Care Team");
        }
        if (ticket.getFirstRepliedAt() == null) {
            ticket.setFirstRepliedAt(LocalDateTime.now());
        }
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.touch();
        supportTicketRepository.save(ticket);

        AIMessage agentMessage = aiMessageRepository.save(
                aiMapper.toMessageEntity(message, MessageSender.AGENT, conversation));
        conversation.touch();
        aiConversationRepository.save(conversation);

        return aiMapper.toMessageResponse(agentMessage);
    }

    @Override
    public AIMessageResponse replyAsCustomer(Long userId, Long ticketId, String message) {
        SupportTicket ticket = findTicket(ticketId);
        checkOwnership(ticket, userId);
        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new BadRequestException("Ticket is already resolved");
        }

        AIConversation conversation = ensureConversation(ticket);

        if (ticket.getStatus() == TicketStatus.PENDING) {
            ticket.setStatus(TicketStatus.OPEN);
        }
        ticket.touch();
        supportTicketRepository.save(ticket);

        AIMessage customerMessage = aiMessageRepository.save(
                aiMapper.toMessageEntity(message, MessageSender.USER, conversation));
        conversation.touch();
        aiConversationRepository.save(conversation);

        return aiMapper.toMessageResponse(customerMessage);
    }

    // =========================================================
    // TICKET MANAGEMENT
    // =========================================================

    @Override
    public SupportTicketResponse updateTicketStatus(Long ticketId, TicketStatus status) {
        SupportTicket ticket = findTicket(ticketId);
        ticket.setStatus(status);
        if (status == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (status != TicketStatus.RESOLVED
                && ticket.getResolvedAt() != null) {
            ticket.setResolvedAt(null);
        }
        ticket.touch();
        return supportTicketMapper.toResponse(supportTicketRepository.save(ticket));
    }

    @Override
    public SupportTicketResponse updateTicketPriority(Long ticketId, TicketPriority priority) {
        SupportTicket ticket = findTicket(ticketId);
        ticket.setPriority(priority);
        ticket.touch();
        return supportTicketMapper.toResponse(supportTicketRepository.save(ticket));
    }

    @Override
    public SupportTicketNoteResponse addNote(Long ticketId, String note, Long agentId) {
        SupportTicket ticket = findTicket(ticketId);

        SupportTicketNote supportNote = new SupportTicketNote();
        supportNote.setTicket(ticket);
        supportNote.setContent(note);
        supportNote.setAuthorName(
                agentId != null
                        ? userRepository.findById(agentId)
                                .map(User::getName)
                                .orElse("Agent")
                        : "Agent");

        supportNote = noteRepository.save(supportNote);
        ticket.touch();
        supportTicketRepository.save(ticket);
        return supportTicketMapper.toNoteResponse(supportNote);
    }

    // =========================================================
    // QUEUE + ANALYTICS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public SupportQueueResponse getQueue(TicketStatus status, String search) {
        List<SupportTicket> tickets = status != null
                ? supportTicketRepository.findByStatusOrderByCreatedAtDesc(status)
                : supportTicketRepository.findByStatusNotOrderByCreatedAtDesc(TicketStatus.RESOLVED);

        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase();
            tickets = tickets.stream()
                    .filter(t -> matches(t, term))
                    .toList();
        }

        SupportQueueResponse response = new SupportQueueResponse();
        response.setUrgentCount(supportTicketRepository
                .countByPriorityAndStatusIn(TicketPriority.URGENT, ACTIVE_STATUSES));
        response.setOpenCount(supportTicketRepository.countByStatus(TicketStatus.OPEN));
        response.setPendingCount(supportTicketRepository.countByStatus(TicketStatus.PENDING));
        response.setInProgressCount(supportTicketRepository.countByStatus(TicketStatus.IN_PROGRESS));
        response.setResolvedCount(supportTicketRepository.countByStatus(TicketStatus.RESOLVED));
        response.setTotalCount(supportTicketRepository.count());
        response.setTickets(supportTicketMapper.toResponseList(tickets));
        return response;
    }

    private boolean matches(SupportTicket ticket, String term) {
        if (ticket.getTicketNumber() != null
                && ticket.getTicketNumber().toLowerCase().contains(term)) {
            return true;
        }
        if (ticket.getSummary() != null
                && ticket.getSummary().toLowerCase().contains(term)) {
            return true;
        }
        if (ticket.getReason() != null
                && ticket.getReason().toLowerCase().contains(term)) {
            return true;
        }
        User user = ticket.getUser();
        return user != null && user.getName() != null
                && user.getName().toLowerCase().contains(term);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        LocalDate startDate = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate endDate = to == null ? LocalDate.now() : to;

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<SupportTicket> created =
                supportTicketRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, end);
        List<SupportTicket> resolved =
                supportTicketRepository.findByResolvedAtBetweenOrderByResolvedAtAsc(start, end);

        SupportAnalyticsResponse response = new SupportAnalyticsResponse();
        response.setFrom(startDate);
        response.setTo(endDate);
        response.setCreatedCount(created.size());
        response.setResolvedCount(resolved.size());
        response.setUrgentCount(countBy(created, TicketPriority.URGENT, ACTIVE_STATUSES));
        response.setOpenCount(countByStatus(created, TicketStatus.OPEN));
        response.setPendingCount(countByStatus(created, TicketStatus.PENDING));
        response.setInProgressCount(countByStatus(created, TicketStatus.IN_PROGRESS));
        response.setAvgFirstResponseMinutes(avgFirstResponseMinutes(created));
        response.setAvgResolutionMinutes(avgResolutionMinutes(resolved));
        return response;
    }

    private long countByStatus(List<SupportTicket> tickets, TicketStatus status) {
        return tickets.stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }

    private long countBy(List<SupportTicket> tickets,
                         TicketPriority priority,
                         List<TicketStatus> statuses) {
        return tickets.stream()
                .filter(t -> t.getPriority() == priority && statuses.contains(t.getStatus()))
                .count();
    }

    private Double avgFirstResponseMinutes(List<SupportTicket> tickets) {
        return tickets.stream()
                .filter(t -> t.getFirstRepliedAt() != null)
                .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getFirstRepliedAt()).toMinutes())
                .average()
                .stream()
                .boxed()
                .map(d -> Math.round(d * 10.0) / 10.0)
                .findFirst()
                .orElse(null);
    }

    private Double avgResolutionMinutes(List<SupportTicket> tickets) {
        return tickets.stream()
                .filter(t -> t.getResolvedAt() != null)
                .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes())
                .average()
                .stream()
                .boxed()
                .map(d -> Math.round(d * 10.0) / 10.0)
                .findFirst()
                .orElse(null);
    }

    // =========================================================
    // SUPPORT ACTIONS
    // =========================================================

    private OrderLookup resolveOrderLookup(Long userId, String text) {
        Long orderId = extractOrderId(text);
        if (orderId == null) {
            List<OrderResponse> orders = orderService.getUserOrders(userId);
            if (orders == null || orders.isEmpty()) {
                return new OrderLookup("The customer has no orders yet.", false);
            }
            StringBuilder sb = new StringBuilder("The customer's orders:\n");
            orders.forEach(o -> sb.append("- ORD-").append(o.getId())
                    .append(": ").append(o.getStatus())
                    .append(" ($").append(o.getTotalAmount()).append(")\n"));
            return new OrderLookup(sb.toString(), false);
        }
        Optional<Order> found = orderRepository.findById(orderId);
        if (found.isEmpty()) {
            return new OrderLookup("No order found with number ORD-"
                    + orderId + ". Ask the customer to double check the order number.", false);
        }
        Order order = found.get();
        if (!belongsTo(order, userId)) {
            return new OrderLookup("The customer tried to access an order that does not belong to them.", false);
        }
        return new OrderLookup(buildOrderContext(orderMapper.toResponse(order)), true);
    }

    private CancelOutcome resolveCancel(Long userId, String text) {
        Long orderId = extractOrderId(text);
        if (orderId == null) {
            return new CancelOutcome(null, false, false);
        }
        Optional<Order> found = orderRepository.findById(orderId);
        if (found.isEmpty()) {
            return new CancelOutcome("No order found with number ORD-"
                    + orderId + ". Ask the customer to double check the order number.", false, false);
        }
        Order order = found.get();
        if (!belongsTo(order, userId)) {
            return new CancelOutcome("The customer tried to cancel an order that does not belong to them.", false, false);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return new CancelOutcome("Order ORD-" + orderId + " is already cancelled.", false, false);
        }
        if (order.getStatus() == OrderStatus.PENDING
                || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.PROCESSING) {
            OrderResponse cancelled =
                    orderService.cancelOrder(orderId, userId, "Cancelled through Customer Care");
            return new CancelOutcome(cancelConfirmation(cancelled), true, false);
        }
        //SHIPPED / DELIVERED - cannot cancel, route to a return specialist
        return new CancelOutcome("Order ORD-" + orderId + " is already "
                + order.getStatus() + " and cannot be cancelled.", false, true);
    }

    private boolean belongsTo(Order order, Long userId) {
        return order.getUser() != null && order.getUser().getId().equals(userId);
    }

    private String cancelConfirmation(OrderResponse order) {
        return "Order ORD-" + order.getId() + " has been cancelled successfully.";
    }

    private String buildOrderContext(OrderResponse order) {
        StringBuilder sb = new StringBuilder("Order ORD-")
                .append(order.getId())
                .append(": status=").append(order.getStatus())
                .append(", total=$").append(order.getTotalAmount())
                .append(", created=").append(order.getCreatedAt())
                .append(", items:");
        if (order.getItems() != null) {
            for (OrderItemResponse item : order.getItems()) {
                sb.append("\n- ").append(item.getProductName())
                        .append(" (").append(item.getQuantity())
                        .append(" x $").append(item.getUnitPrice()).append(")");
            }
        }
        return sb.toString();
    }

    // =========================================================
    // INTENT DETECTION
    // =========================================================

    private Intent detectIntent(String text) {
        String t = text.toLowerCase();
        if (containsAny(t, "cancel")) {
            return Intent.CANCEL_ORDER;
        }
        if (containsAny(t, "refund", "return", "damaged", "broken", "not received", "defective")) {
            return Intent.REFUND;
        }
        if (containsAny(t, "order", "ord-", "track", "where is my", "my package", "my parcel", "status of")) {
            return Intent.ORDER_STATUS;
        }
        if (containsAny(t, "payment", "pay", "paid", "charge", "invoice")) {
            return Intent.PAYMENT;
        }
        if (containsAny(t, "ship", "deliver", "delivery", "shipping", "arrive", "address")) {
            return Intent.SHIPPING;
        }
        if (containsAny(t, "account", "password", "profile", "login", "sign in")) {
            return Intent.ACCOUNT;
        }
        if (containsAny(t, "complaint", "scam", "terrible", "poor service", "unhappy", "worst", "angry")) {
            return Intent.COMPLAINT;
        }
        if (containsAny(t, "perfume", "scent", "fragrance", "recommend", "compare", "smell", "floral", "sweet")) {
            return Intent.PRODUCT;
        }
        return Intent.GENERAL;
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private Long extractOrderId(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = ORDER_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private TicketPriority detectPriority(String combined) {
        if (combined != null && containsAny(combined.toLowerCase(),
                "urgent", "asap", "emergency", "complaint", "scam", "terrible",
                "broken", "damaged", "refund", "not received")) {
            return TicketPriority.URGENT;
        }
        return TicketPriority.NORMAL;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private SupportTicket findTicket(Long ticketId) {
        return supportTicketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Support ticket not found"));
    }

    private void checkOwnership(SupportTicket ticket, Long userId) {
        if (!ticket.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this ticket");
        }
    }

    private AIConversation resolveConversation(Long userId, Long conversationId, String firstMessage) {
        if (conversationId == null) {
            AIConversation conversation = new AIConversation();
            User user = findUser(userId);
            conversation.setUser(user);
            conversation.setUserName(user.getName());
            conversation.setTitle(generateConversationTitle(firstMessage));
            return aiConversationRepository.save(conversation);
        }
        AIConversation conversation = aiConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        if (!conversation.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this conversation");
        }
        return conversation;
    }

    private AIConversation ensureConversation(SupportTicket ticket) {
        if (ticket.getConversation() != null) {
            return ticket.getConversation();
        }
        AIConversation conversation = new AIConversation();
        conversation.setUser(ticket.getUser());
        conversation.setUserName(ticket.getUser().getName());
        conversation.setTitle("Support ticket " + ticket.getTicketNumber());
        conversation = aiConversationRepository.save(conversation);
        ticket.setConversation(conversation);
        return conversation;
    }

    private List<AIMessageResponse> loadMessages(SupportTicket ticket) {
        if (ticket.getConversation() == null) {
            return java.util.List.of();
        }
        return aiMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(ticket.getConversation().getId())
                .stream()
                .map(aiMapper::toMessageResponse)
                .toList();
    }

    private String buildSummary(AIConversation conversation) {
        List<AIMessage> messages =
                aiMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        if (messages.size() > SUMMARY_MESSAGE_COUNT) {
            messages = messages.subList(messages.size() - SUMMARY_MESSAGE_COUNT, messages.size());
        }
        StringBuilder sb = new StringBuilder();
        for (AIMessage message : messages) {
            String role = switch (message.getSender()) {
                case USER -> "Customer";
                case AI -> "Care AI";
                case AGENT -> "Agent";
            };
            sb.append(role).append(": ").append(message.getMessage()).append("\n");
        }
        return sb.toString().trim();
    }

    private String generateTicketNumber() {
        long candidate = 1000 + supportTicketRepository.count();
        while (supportTicketRepository.existsByTicketNumber("BS-" + candidate)) {
            candidate++;
        }
        return "BS-" + candidate;
    }

    private String generateConversationTitle(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "New Conversation";
        }
        String title = message.trim().replaceAll("\\s+", " ");
        if (title.length() > 50) {
            title = title.substring(0, 50).trim() + "...";
        }
        return title;
    }
}