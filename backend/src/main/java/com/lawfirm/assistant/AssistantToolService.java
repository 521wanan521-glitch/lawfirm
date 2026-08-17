package com.lawfirm.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lawfirm.approval.ApprovalService;
import com.lawfirm.approval.ApprovalStatus;
import com.lawfirm.approval.dto.DecisionRequest;
import com.lawfirm.approval.dto.InstanceRequest;
import com.lawfirm.approval.dto.InstanceView;
import com.lawfirm.billing.BillingService;
import com.lawfirm.billing.InvoiceStatus;
import com.lawfirm.billing.dto.InvoiceRequest;
import com.lawfirm.billing.dto.InvoiceStatusRequest;
import com.lawfirm.billing.dto.InvoiceView;
import com.lawfirm.billing.dto.TimeEntryRequest;
import com.lawfirm.billing.dto.TimeEntryView;
import com.lawfirm.calendar.CalendarService;
import com.lawfirm.calendar.EventType;
import com.lawfirm.calendar.dto.EventRequest;
import com.lawfirm.calendar.dto.EventView;
import com.lawfirm.cases.CaseService;
import com.lawfirm.cases.CaseStatus;
import com.lawfirm.cases.CaseType;
import com.lawfirm.cases.Priority;
import com.lawfirm.cases.dto.CaseProgressRequest;
import com.lawfirm.cases.dto.CaseProgressView;
import com.lawfirm.cases.dto.CaseRequest;
import com.lawfirm.cases.dto.CaseStatusRequest;
import com.lawfirm.cases.dto.CaseView;
import com.lawfirm.client.Client;
import com.lawfirm.client.ClientService;
import com.lawfirm.client.Interaction;
import com.lawfirm.client.dto.ClientRequest;
import com.lawfirm.client.dto.ClientView;
import com.lawfirm.client.dto.ContactRequest;
import com.lawfirm.client.dto.ContactView;
import com.lawfirm.client.dto.InteractionRequest;
import com.lawfirm.client.dto.InteractionView;
import com.lawfirm.common.BizException;
import com.lawfirm.common.PageResult;
import com.lawfirm.dashboard.DashboardService;
import com.lawfirm.document.DocumentService;
import com.lawfirm.document.dto.DocumentView;
import com.lawfirm.document.dto.FolderRequest;
import com.lawfirm.document.dto.FolderView;
import com.lawfirm.knowledge.KnowledgeArticle;
import com.lawfirm.knowledge.KnowledgeCategory;
import com.lawfirm.knowledge.KnowledgeRepository;
import com.lawfirm.knowledge.KnowledgeService;
import com.lawfirm.knowledge.dto.ArticleRequest;
import com.lawfirm.knowledge.dto.ArticleView;
import com.lawfirm.security.CurrentUser;
import com.lawfirm.user.User;
import com.lawfirm.user.UserService;
import com.lawfirm.user.dto.ResetPasswordRequest;
import com.lawfirm.user.dto.UserRequest;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * AI 助手的工具（function calling）注册与执行。
 * 工具直接复用现有 Service，权限与业务规则与正常接口完全一致（基于当前登录人）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantToolService {

    private final ObjectMapper mapper;
    private final CaseService caseService;
    private final ClientService clientService;
    private final BillingService billingService;
    private final CalendarService calendarService;
    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeService knowledgeService;
    private final DocumentService documentService;
    private final ApprovalService approvalService;
    private final DashboardService dashboardService;
    private final UserService userService;

    /** 工具执行结果 */
    public record ToolResult(String json, boolean ok) {
    }

    private volatile ArrayNode toolDefinitions;

    public ArrayNode definitions() {
        ArrayNode d = toolDefinitions;
        if (d == null) {
            synchronized (this) {
                if (toolDefinitions == null) {
                    toolDefinitions = buildDefinitions();
                }
            }
            d = toolDefinitions;
        }
        return d;
    }

    public ToolResult execute(String name, String argumentsJson) {
        try {
            String result = switch (name) {
                case "list_my_cases" -> listMyCases(argumentsJson);
                case "get_case_detail" -> getCaseDetail(argumentsJson);
                case "search_clients" -> searchClients(argumentsJson);
                case "get_client_detail" -> getClientDetail(argumentsJson);
                case "get_my_time_entries" -> getMyTimeEntries(argumentsJson);
                case "record_time_entry" -> recordTimeEntry(argumentsJson);
                case "get_my_schedule" -> getMySchedule(argumentsJson);
                case "create_calendar_event" -> createCalendarEvent(argumentsJson);
                case "search_knowledge" -> searchKnowledge(argumentsJson);
                case "search_documents" -> searchDocuments(argumentsJson);
                case "get_todo_approvals" -> getTodoApprovals(argumentsJson);
                case "list_approval_templates" -> listApprovalTemplates(argumentsJson);
                case "list_approvers" -> listApprovers(argumentsJson);
                case "create_approval" -> createApproval(argumentsJson);
                case "add_case_progress" -> addCaseProgress(argumentsJson);
                case "get_dashboard_summary" -> getDashboardSummary(argumentsJson);
                case "create_case" -> createCase(argumentsJson);
                case "update_case" -> updateCase(argumentsJson);
                case "update_case_status" -> updateCaseStatus(argumentsJson);
                case "create_client" -> createClient(argumentsJson);
                case "update_client" -> updateClient(argumentsJson);
                case "add_client_interaction" -> addClientInteraction(argumentsJson);
                case "add_client_contact" -> addClientContact(argumentsJson);
                case "update_client_contact" -> updateClientContact(argumentsJson);
                case "update_time_entry" -> updateTimeEntry(argumentsJson);
                case "create_invoice" -> createInvoice(argumentsJson);
                case "update_invoice_status" -> updateInvoiceStatus(argumentsJson);
                case "update_calendar_event" -> updateCalendarEvent(argumentsJson);
                case "decide_approval" -> decideApproval(argumentsJson);
                case "cancel_approval" -> cancelApproval(argumentsJson);
                case "create_folder" -> createFolder(argumentsJson);
                case "create_knowledge_article" -> createKnowledgeArticle(argumentsJson);
                case "update_knowledge_article" -> updateKnowledgeArticle(argumentsJson);
                case "create_user" -> createUser(argumentsJson);
                case "update_user" -> updateUser(argumentsJson);
                case "reset_user_password" -> resetUserPassword(argumentsJson);
                default -> throw new BizException("未知工具：" + name);
            };
            return new ToolResult(result, true);
        } catch (Exception e) {
            log.warn("工具 {} 执行失败：{}", name, e.getMessage());
            Map<String, Object> err = Map.of("error", e.getMessage() == null ? "执行失败" : e.getMessage());
            try {
                return new ToolResult(mapper.writeValueAsString(err), false);
            } catch (Exception ex) {
                return new ToolResult("{\"error\":\"执行失败\"}", false);
            }
        }
    }

    // ==================== 工具实现 ====================

    private String listMyCases(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        String keyword = a.path("keyword").asText("");
        PageResult<CaseView> page = caseService.myCases(1, 50);
        List<CaseView> items = page.getItems();
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            items = items.stream().filter(c ->
                    (c.caseNo() != null && c.caseNo().contains(k))
                            || (c.title() != null && c.title().contains(k))
                            || (c.clientName() != null && c.clientName().contains(k))).toList();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", items.size());
        out.put("items", items);
        return write(out);
    }

    private String getCaseDetail(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("caseId").asLong();
        CaseView c = caseService.detail(id);
        PageResult<CaseProgressView> progress = caseService.progress(id, 1, 10);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("case", c);
        out.put("recentProgress", progress.getItems());
        return write(out);
    }

    private String searchClients(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        String keyword = a.path("keyword").asText("");
        PageResult<ClientView> page = clientService.page(keyword, null, null, 1, 20);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", page.getTotal());
        out.put("items", page.getItems());
        return write(out);
    }

    private String getClientDetail(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("clientId").asLong();
        ClientView c = clientService.detail(id);
        PageResult<InteractionView> interactions = clientService.interactions(id, 1, 10);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("client", c);
        out.put("recentInteractions", interactions.getItems());
        return write(out);
    }

    private String getMyTimeEntries(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        int days = (int) asLong(a, "days", 7);
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        LocalDate end = LocalDate.now();
        PageResult<TimeEntryView> page = billingService.pageTimeEntries(null, null, null, start, end, 1, 200);
        BigDecimal sum = page.getItems().stream().map(TimeEntryView::hours)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", days);
        out.put("totalHours", sum);
        out.put("count", page.getItems().size());
        out.put("items", page.getItems());
        return write(out);
    }

    private String recordTimeEntry(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long caseId = asLong(a, "caseId", 0);
        BigDecimal hours = asDecimal(a, "hours");
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("工时（hours）必须为大于 0 的数字");
        }
        String description = a.path("description").asText("");
        String workDateStr = a.path("workDate").asText("");
        BigDecimal rate = asDecimal(a, "rate");
        LocalDate workDate = StringUtils.hasText(workDateStr) ? LocalDate.parse(workDateStr) : LocalDate.now();
        TimeEntryView v = billingService.createTimeEntry(
                new TimeEntryRequest(caseId, workDate, hours, rate, description));
        return write(v);
    }

    private String getMySchedule(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        int days = (int) asLong(a, "days", 7);
        List<EventView> list = calendarService.myEvents(LocalDateTime.now(), LocalDateTime.now().plusDays(days));
        return write(list);
    }

    private String createCalendarEvent(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        String title = a.path("title").asText("");
        String type = a.path("type").asText("TASK");
        EventType et = EventType.valueOf(type.toUpperCase());
        LocalDateTime start = parseDateTime(a.path("startTime").asText(""));
        LocalDateTime end = a.hasNonNull("endTime") ? parseDateTime(a.get("endTime").asText("")) : null;
        String location = optText(a, "location");
        String description = optText(a, "description");
        Long caseId = a.hasNonNull("caseId") ? a.get("caseId").asLong() : null;
        EventView v = calendarService.create(new EventRequest(title, et, start, end, location, description, caseId, null));
        return write(v);
    }

    private String searchKnowledge(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        String query = a.path("query").asText("");
        if (!StringUtils.hasText(query)) {
            throw new BizException("缺少查询关键词");
        }
        Long me = CurrentUser.id();
        boolean admin = CurrentUser.isAdmin();
        String like = "%" + query.trim() + "%";
        Specification<KnowledgeArticle> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("tags"), like),
                    cb.like(root.get("content"), like)));
            if (!admin) {
                ps.add(cb.or(cb.isTrue(root.get("published")), cb.equal(root.get("authorId"), me)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        List<KnowledgeArticle> list = knowledgeRepository
                .findAll(spec, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
        List<Map<String, Object>> items = new ArrayList<>();
        for (KnowledgeArticle art : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", art.getId());
            m.put("title", art.getTitle());
            m.put("category", art.getCategory() == null ? null : art.getCategory().name());
            m.put("tags", art.getTags());
            String content = art.getContent() == null ? "" : art.getContent();
            m.put("snippet", content.length() > 600 ? content.substring(0, 600) + "..." : content);
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", items.size());
        out.put("items", items);
        return write(out);
    }

    private String searchDocuments(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        String keyword = a.path("keyword").asText("");
        PageResult<DocumentView> page = documentService.page(keyword, null, null, null, null, 1, 20);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", page.getTotal());
        out.put("items", page.getItems());
        return write(out);
    }

    private String getTodoApprovals(String argsJson) {
        PageResult<InstanceView> page = approvalService.instances("todo", ApprovalStatus.PENDING, 1, 20);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", page.getTotal());
        out.put("items", page.getItems());
        return write(out);
    }

    private String listApprovalTemplates(String argsJson) {
        return write(approvalService.templates(true));
    }

    private String listApprovers(String argsJson) {
        return write(approvalService.approvers());
    }

    private String createApproval(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long templateId = a.path("templateId").asLong();
        String title = a.path("title").asText("");
        String content = a.path("content").asText("");
        long approverId = a.path("approverId").asLong();
        Long caseId = a.hasNonNull("caseId") ? a.get("caseId").asLong() : null;
        InstanceView v = approvalService.create(new InstanceRequest(templateId, title, content, approverId, caseId));
        return write(v);
    }

    private String addCaseProgress(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long caseId = a.path("caseId").asLong();
        String content = a.path("content").asText("");
        String dateStr = a.path("progressDate").asText("");
        LocalDate date = StringUtils.hasText(dateStr) ? LocalDate.parse(dateStr) : LocalDate.now();
        CaseProgressView v = caseService.addProgress(caseId, new CaseProgressRequest(content, date));
        return write(v);
    }

    private String getDashboardSummary(String argsJson) {
        return write(dashboardService.summary());
    }

    private String createCase(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long clientId = a.path("clientId").asLong();
        String title = a.path("title").asText("");
        CaseType type = CaseType.valueOf(a.path("type").asText("").toUpperCase());
        Priority priority = a.hasNonNull("priority") ? Priority.valueOf(a.get("priority").asText().toUpperCase()) : null;
        long leadLawyerId = a.hasNonNull("leadLawyerId") ? a.get("leadLawyerId").asLong() : CurrentUser.id();
        List<Long> coLawyerIds = listOf(a, "coLawyerIds");
        String court = optText(a, "court");
        BigDecimal caseAmount = asDecimal(a, "caseAmount");
        LocalDate filingDate = hasDate(a, "filingDate") ? LocalDate.parse(a.get("filingDate").asText()) : null;
        String description = optText(a, "description");
        BigDecimal fee = asDecimal(a, "fee");
        CaseView v = caseService.create(new CaseRequest(clientId, title, type, priority,
                leadLawyerId, coLawyerIds, court, caseAmount, filingDate, description, fee));
        return write(v);
    }

    private String updateCase(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("caseId").asLong();
        long clientId = a.path("clientId").asLong();
        String title = a.path("title").asText("");
        CaseType type = CaseType.valueOf(a.path("type").asText("").toUpperCase());
        Priority priority = a.hasNonNull("priority") ? Priority.valueOf(a.get("priority").asText().toUpperCase()) : null;
        long leadLawyerId = a.hasNonNull("leadLawyerId") ? a.get("leadLawyerId").asLong() : CurrentUser.id();
        List<Long> coLawyerIds = listOf(a, "coLawyerIds");
        String court = optText(a, "court");
        BigDecimal caseAmount = asDecimal(a, "caseAmount");
        LocalDate filingDate = hasDate(a, "filingDate") ? LocalDate.parse(a.get("filingDate").asText()) : null;
        String description = optText(a, "description");
        BigDecimal fee = asDecimal(a, "fee");
        CaseView v = caseService.update(id, new CaseRequest(clientId, title, type, priority,
                leadLawyerId, coLawyerIds, court, caseAmount, filingDate, description, fee));
        return write(v);
    }

    private String updateCaseStatus(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("caseId").asLong();
        CaseStatus status = CaseStatus.valueOf(a.path("status").asText("").toUpperCase());
        String result = optText(a, "result");
        LocalDate closeDate = hasDate(a, "closeDate") ? LocalDate.parse(a.get("closeDate").asText()) : null;
        CaseView v = caseService.updateStatus(id, new CaseStatusRequest(status, result, closeDate));
        return write(v);
    }

    private String createClient(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        String name = a.path("name").asText("");
        Client.Type type = Client.Type.valueOf(a.path("type").asText("").toUpperCase());
        String idNumber = optText(a, "idNumber");
        String industry = optText(a, "industry");
        String address = optText(a, "address");
        String phone = optText(a, "phone");
        String email = optText(a, "email");
        Client.Level level = a.hasNonNull("level") ? Client.Level.valueOf(a.get("level").asText().toUpperCase()) : null;
        String source = optText(a, "source");
        Long ownerId = a.hasNonNull("ownerId") ? a.get("ownerId").asLong() : CurrentUser.id();
        String remark = optText(a, "remark");
        ClientView v = clientService.create(new ClientRequest(name, type, idNumber, industry,
                address, phone, email, level, source, ownerId, remark));
        return write(v);
    }

    private String updateClient(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("clientId").asLong();
        String name = a.path("name").asText("");
        Client.Type type = Client.Type.valueOf(a.path("type").asText("").toUpperCase());
        String idNumber = optText(a, "idNumber");
        String industry = optText(a, "industry");
        String address = optText(a, "address");
        String phone = optText(a, "phone");
        String email = optText(a, "email");
        Client.Level level = a.hasNonNull("level") ? Client.Level.valueOf(a.get("level").asText().toUpperCase()) : null;
        String source = optText(a, "source");
        Long ownerId = a.hasNonNull("ownerId") ? a.get("ownerId").asLong() : CurrentUser.id();
        String remark = optText(a, "remark");
        ClientView v = clientService.update(id, new ClientRequest(name, type, idNumber, industry,
                address, phone, email, level, source, ownerId, remark));
        return write(v);
    }

    private String addClientInteraction(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long clientId = a.path("clientId").asLong();
        Interaction.Type type = Interaction.Type.valueOf(a.path("type").asText("").toUpperCase());
        String content = a.path("content").asText("");
        LocalDate nextFollowDate = hasDate(a, "nextFollowDate") ? LocalDate.parse(a.get("nextFollowDate").asText()) : null;
        InteractionView v = clientService.addInteraction(clientId, new InteractionRequest(type, content, nextFollowDate));
        return write(v);
    }

    private String addClientContact(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long clientId = a.path("clientId").asLong();
        String name = a.path("name").asText("");
        String phone = optText(a, "phone");
        String email = optText(a, "email");
        String position = optText(a, "position");
        Boolean primary = a.hasNonNull("primaryContact") ? a.get("primaryContact").asBoolean() : null;
        ContactView v = clientService.addContact(clientId, new ContactRequest(name, phone, email, position, primary));
        return write(v);
    }

    private String updateClientContact(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long clientId = a.path("clientId").asLong();
        long contactId = a.path("contactId").asLong();
        String name = a.path("name").asText("");
        String phone = optText(a, "phone");
        String email = optText(a, "email");
        String position = optText(a, "position");
        Boolean primary = a.hasNonNull("primaryContact") ? a.get("primaryContact").asBoolean() : null;
        ContactView v = clientService.updateContact(clientId, contactId,
                new ContactRequest(name, phone, email, position, primary));
        return write(v);
    }

    private String updateTimeEntry(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("timeEntryId").asLong();
        long caseId = a.path("caseId").asLong();
        BigDecimal hours = asDecimal(a, "hours");
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("工时（hours）必须为大于 0 的数字");
        }
        String description = a.path("description").asText("");
        BigDecimal rate = asDecimal(a, "rate");
        LocalDate workDate = hasDate(a, "workDate") ? LocalDate.parse(a.get("workDate").asText()) : LocalDate.now();
        TimeEntryView v = billingService.updateTimeEntry(id,
                new TimeEntryRequest(caseId, workDate, hours, rate, description));
        return write(v);
    }

    private String createInvoice(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long clientId = a.path("clientId").asLong();
        List<Long> timeEntryIds = listOf(a, "timeEntryIds");
        LocalDate issueDate = hasDate(a, "issueDate") ? LocalDate.parse(a.get("issueDate").asText()) : LocalDate.now();
        LocalDate dueDate = hasDate(a, "dueDate") ? LocalDate.parse(a.get("dueDate").asText()) : null;
        String remark = optText(a, "remark");
        InvoiceView v = billingService.createInvoice(new InvoiceRequest(clientId, timeEntryIds, issueDate, dueDate, remark));
        return write(v);
    }

    private String updateInvoiceStatus(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("invoiceId").asLong();
        InvoiceStatus status = InvoiceStatus.valueOf(a.path("status").asText("").toUpperCase());
        InvoiceView v = billingService.updateInvoiceStatus(id, new InvoiceStatusRequest(status));
        return write(v);
    }

    private String updateCalendarEvent(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("eventId").asLong();
        String title = a.path("title").asText("");
        EventType et = EventType.valueOf(a.path("type").asText("TASK").toUpperCase());
        LocalDateTime start = parseDateTime(a.path("startTime").asText(""));
        LocalDateTime end = a.hasNonNull("endTime") ? parseDateTime(a.get("endTime").asText("")) : null;
        String location = optText(a, "location");
        String description = optText(a, "description");
        Long caseId = a.hasNonNull("caseId") ? a.get("caseId").asLong() : null;
        EventView v = calendarService.update(id, new EventRequest(title, et, start, end, location, description, caseId, null));
        return write(v);
    }

    private String decideApproval(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("approvalId").asLong();
        boolean approved = a.path("approved").asBoolean();
        String comment = a.path("comment").asText("");
        InstanceView v = approvalService.decide(id, approved, new DecisionRequest(comment));
        return write(v);
    }

    private String cancelApproval(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("approvalId").asLong();
        InstanceView v = approvalService.cancel(id);
        return write(v);
    }

    private String createFolder(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        String name = a.path("name").asText("");
        Long parentId = a.hasNonNull("parentId") ? a.get("parentId").asLong() : null;
        FolderView v = documentService.createFolder(new FolderRequest(name, parentId));
        return write(v);
    }

    private String createKnowledgeArticle(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        String title = a.path("title").asText("");
        KnowledgeCategory category = KnowledgeCategory.valueOf(a.path("category").asText("").toUpperCase());
        String content = a.path("content").asText("");
        String tags = optText(a, "tags");
        Boolean published = a.hasNonNull("published") ? a.get("published").asBoolean() : true;
        ArticleView v = knowledgeService.create(new ArticleRequest(title, category, content, tags, published));
        return write(v);
    }

    private String updateKnowledgeArticle(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("articleId").asLong();
        String title = a.path("title").asText("");
        KnowledgeCategory category = KnowledgeCategory.valueOf(a.path("category").asText("").toUpperCase());
        String content = a.path("content").asText("");
        String tags = optText(a, "tags");
        Boolean published = a.hasNonNull("published") ? a.get("published").asBoolean() : true;
        ArticleView v = knowledgeService.update(id, new ArticleRequest(title, category, content, tags, published));
        return write(v);
    }

    private String createUser(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        UserRequest req = new UserRequest();
        req.setUsername(a.path("username").asText(""));
        req.setPassword(a.path("password").asText(""));
        req.setRealName(a.path("realName").asText(""));
        req.setEmail(optText(a, "email"));
        req.setPhone(optText(a, "phone"));
        req.setRole(User.Role.valueOf(a.path("role").asText("").toUpperCase()));
        req.setDepartment(optText(a, "department"));
        req.setTitle(optText(a, "title"));
        return write(userService.create(req));
    }

    private String updateUser(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("userId").asLong();
        UserRequest req = new UserRequest();
        req.setUsername(a.path("username").asText(""));
        req.setRealName(a.path("realName").asText(""));
        req.setEmail(optText(a, "email"));
        req.setPhone(optText(a, "phone"));
        req.setRole(User.Role.valueOf(a.path("role").asText("").toUpperCase()));
        req.setDepartment(optText(a, "department"));
        req.setTitle(optText(a, "title"));
        return write(userService.update(id, req));
    }

    private String resetUserPassword(String argsJson) {
        JsonNode a = parseArgs(argsJson);
        long id = a.path("userId").asLong();
        String newPassword = a.path("newPassword").asText("");
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setNewPassword(newPassword);
        userService.resetPassword(id, req);
        return write(Map.of("ok", true, "message", "密码已重置"));
    }

    // ==================== 工具定义 ====================

    private ArrayNode buildDefinitions() {
        ArrayNode arr = mapper.createArrayNode();
        ObjectNode p;

        p = mapper.createObjectNode();
        p.set("keyword", prop("string", "按案号、案件名称或客户名称过滤，可选"));
        arr.add(tool("list_my_cases", "查询当前用户主办或协办的案件列表（可选关键词过滤）", p));

        p = mapper.createObjectNode();
        p.set("caseId", prop("integer", "案件 id"));
        arr.add(tool("get_case_detail", "根据案件 id 查询案件详情与最近进展记录", p, "caseId"));

        p = mapper.createObjectNode();
        p.set("keyword", prop("string", "客户名称、电话或证件号关键字"));
        arr.add(tool("search_clients", "按关键字搜索客户", p, "keyword"));

        p = mapper.createObjectNode();
        p.set("clientId", prop("integer", "客户 id"));
        arr.add(tool("get_client_detail", "根据客户 id 查询客户档案与最近跟进记录", p, "clientId"));

        p = mapper.createObjectNode();
        p.set("days", prop("integer", "查询最近 N 天，默认 7"));
        arr.add(tool("get_my_time_entries", "查询当前用户最近 N 天的工时记录与合计工时", p));

        p = mapper.createObjectNode();
        p.set("caseId", prop("integer", "关联案件 id"));
        p.set("hours", prop("number", "工时小时数，如 2.5"));
        p.set("description", prop("string", "工作内容描述"));
        p.set("workDate", prop("string", "工作日期，格式 yyyy-MM-dd，默认今天"));
        p.set("rate", prop("number", "小时费率（元），可选"));
        arr.add(tool("record_time_entry", "为当前用户新增一条工时记录", p, "caseId", "hours", "description"));

        p = mapper.createObjectNode();
        p.set("days", prop("integer", "查询未来 N 天，默认 7"));
        arr.add(tool("get_my_schedule", "查询当前用户未来 N 天的日程（开庭/会议/任务等）", p));

        p = mapper.createObjectNode();
        p.set("title", prop("string", "日程标题"));
        p.set("type", propEnum("日程类型", "COURT", "MEETING", "TASK", "REMINDER"));
        p.set("startTime", prop("string", "开始时间，ISO 格式如 2024-06-01T09:30:00"));
        p.set("endTime", prop("string", "结束时间，ISO 格式，可选"));
        p.set("location", prop("string", "地点，可选"));
        p.set("description", prop("string", "备注，可选"));
        p.set("caseId", prop("integer", "关联案件 id，可选"));
        arr.add(tool("create_calendar_event", "为当前用户创建一条日程", p, "title", "startTime"));

        p = mapper.createObjectNode();
        p.set("query", prop("string", "检索关键词，如「合同 违约金 管辖」"));
        arr.add(tool("search_knowledge", "在知识库中检索办案经验、法规、文书模板等，返回标题与内容摘要", p, "query"));

        p = mapper.createObjectNode();
        p.set("keyword", prop("string", "文档名称关键字"));
        arr.add(tool("search_documents", "按名称搜索文档中心的文档", p, "keyword"));

        arr.add(tool("get_todo_approvals", "查询待当前用户审批的审批单", mapper.createObjectNode()));

        arr.add(tool("list_approval_templates", "查询可用的审批模板（用章/请假/报销/立案等）", mapper.createObjectNode()));

        arr.add(tool("list_approvers", "查询可作为审批人的成员（合伙人/管理员）列表", mapper.createObjectNode()));

        p = mapper.createObjectNode();
        p.set("templateId", prop("integer", "审批模板 id，先通过 list_approval_templates 获取"));
        p.set("title", prop("string", "审批标题"));
        p.set("content", prop("string", "申请内容"));
        p.set("approverId", prop("integer", "审批人 id，先通过 list_approvers 获取"));
        p.set("caseId", prop("integer", "关联案件 id，可选"));
        arr.add(tool("create_approval", "为当前用户发起一条审批申请", p, "templateId", "title", "content", "approverId"));

        p = mapper.createObjectNode();
        p.set("caseId", prop("integer", "案件 id"));
        p.set("content", prop("string", "进展记录内容"));
        p.set("progressDate", prop("string", "进展日期 yyyy-MM-dd，默认今天"));
        arr.add(tool("add_case_progress", "为案件新增一条办理进展记录", p, "caseId", "content"));

        arr.add(tool("get_dashboard_summary", "查询律所经营概况（案件/客户/待审批/待审核工时/营收等）", mapper.createObjectNode()));

        // ==================== 写操作工具（均需用户二次确认） ====================

        p = mapper.createObjectNode();
        p.set("clientId", prop("integer", "客户 id"));
        p.set("title", prop("string", "案件名称"));
        p.set("type", propEnum("案件类型", "CIVIL", "CRIMINAL", "ADMIN", "COMMERCIAL", "LABOR", "IP", "FAMILY", "OTHER"));
        p.set("priority", propEnum("优先级，可选", "LOW", "MEDIUM", "HIGH", "URGENT"));
        p.set("leadLawyerId", prop("integer", "主办律师 id，默认当前登录人"));
        p.set("coLawyerIds", prop("array", "协办律师 id 数组，可选"));
        p.set("court", prop("string", "受理法院，可选"));
        p.set("caseAmount", prop("number", "标的额（元），可选"));
        p.set("filingDate", prop("string", "立案日期 yyyy-MM-dd，可选"));
        p.set("description", prop("string", "案情摘要，可选"));
        p.set("fee", prop("number", "收费金额（元），可选"));
        arr.add(tool("create_case", "新建案件（写操作，需用户确认后执行）", p, "clientId", "title", "type"));

        p = mapper.createObjectNode();
        p.set("caseId", prop("integer", "案件 id"));
        p.set("clientId", prop("integer", "客户 id"));
        p.set("title", prop("string", "案件名称"));
        p.set("type", propEnum("案件类型", "CIVIL", "CRIMINAL", "ADMIN", "COMMERCIAL", "LABOR", "IP", "FAMILY", "OTHER"));
        p.set("priority", propEnum("优先级，可选", "LOW", "MEDIUM", "HIGH", "URGENT"));
        p.set("leadLawyerId", prop("integer", "主办律师 id，可选"));
        p.set("coLawyerIds", prop("array", "协办律师 id 数组，可选"));
        p.set("court", prop("string", "受理法院，可选"));
        p.set("caseAmount", prop("number", "标的额（元），可选"));
        p.set("filingDate", prop("string", "立案日期 yyyy-MM-dd，可选"));
        p.set("description", prop("string", "案情摘要，可选"));
        p.set("fee", prop("number", "收费金额（元），可选"));
        arr.add(tool("update_case", "修改案件信息（写操作，需用户确认后执行）", p, "caseId", "clientId", "title", "type"));

        p = mapper.createObjectNode();
        p.set("caseId", prop("integer", "案件 id"));
        p.set("status", propEnum("目标状态", "NEW", "ACTIVE", "PAUSED", "CLOSED", "ARCHIVED"));
        p.set("result", prop("string", "办理结果（结案时填写），可选"));
        p.set("closeDate", prop("string", "结案日期 yyyy-MM-dd，可选"));
        arr.add(tool("update_case_status", "变更案件状态（写操作，需用户确认后执行）", p, "caseId", "status"));

        p = mapper.createObjectNode();
        p.set("name", prop("string", "客户名称"));
        p.set("type", propEnum("客户类型", "PERSONAL", "COMPANY"));
        p.set("idNumber", prop("string", "证件号/统一社会信用代码，可选"));
        p.set("industry", prop("string", "行业，可选"));
        p.set("address", prop("string", "地址，可选"));
        p.set("phone", prop("string", "电话，可选"));
        p.set("email", prop("string", "邮箱，可选"));
        p.set("level", propEnum("客户分级，可选", "A", "B", "C"));
        p.set("source", prop("string", "来源，可选"));
        p.set("ownerId", prop("integer", "负责人 id，默认当前登录人"));
        p.set("remark", prop("string", "备注，可选"));
        arr.add(tool("create_client", "新建客户（写操作，需用户确认后执行）", p, "name", "type"));

        p = mapper.createObjectNode();
        p.set("clientId", prop("integer", "客户 id"));
        p.set("name", prop("string", "客户名称"));
        p.set("type", propEnum("客户类型", "PERSONAL", "COMPANY"));
        p.set("idNumber", prop("string", "证件号/统一社会信用代码，可选"));
        p.set("industry", prop("string", "行业，可选"));
        p.set("address", prop("string", "地址，可选"));
        p.set("phone", prop("string", "电话，可选"));
        p.set("email", prop("string", "邮箱，可选"));
        p.set("level", propEnum("客户分级，可选", "A", "B", "C"));
        p.set("source", prop("string", "来源，可选"));
        p.set("ownerId", prop("integer", "负责人 id，可选"));
        p.set("remark", prop("string", "备注，可选"));
        arr.add(tool("update_client", "修改客户档案（写操作，需用户确认后执行）", p, "clientId", "name", "type"));

        p = mapper.createObjectNode();
        p.set("clientId", prop("integer", "客户 id"));
        p.set("type", propEnum("跟进方式", "PHONE", "VISIT", "EMAIL", "WECHAT", "MEETING", "OTHER"));
        p.set("content", prop("string", "跟进内容"));
        p.set("nextFollowDate", prop("string", "下次跟进日期 yyyy-MM-dd，可选"));
        arr.add(tool("add_client_interaction", "为客户添加跟进记录（写操作，需用户确认后执行）", p, "clientId", "type", "content"));

        p = mapper.createObjectNode();
        p.set("clientId", prop("integer", "客户 id"));
        p.set("name", prop("string", "联系人姓名"));
        p.set("phone", prop("string", "电话，可选"));
        p.set("email", prop("string", "邮箱，可选"));
        p.set("position", prop("string", "职位，可选"));
        p.set("primaryContact", prop("boolean", "是否主要联系人，可选"));
        arr.add(tool("add_client_contact", "为客户添加联系人（写操作，需用户确认后执行）", p, "clientId", "name"));

        p = mapper.createObjectNode();
        p.set("clientId", prop("integer", "客户 id"));
        p.set("contactId", prop("integer", "联系人 id"));
        p.set("name", prop("string", "联系人姓名"));
        p.set("phone", prop("string", "电话，可选"));
        p.set("email", prop("string", "邮箱，可选"));
        p.set("position", prop("string", "职位，可选"));
        p.set("primaryContact", prop("boolean", "是否主要联系人，可选"));
        arr.add(tool("update_client_contact", "修改客户联系人（写操作，需用户确认后执行）", p, "clientId", "contactId", "name"));

        p = mapper.createObjectNode();
        p.set("timeEntryId", prop("integer", "工时记录 id"));
        p.set("caseId", prop("integer", "关联案件 id"));
        p.set("hours", prop("number", "工时小时数，如 2.5"));
        p.set("description", prop("string", "工作内容描述"));
        p.set("workDate", prop("string", "工作日期 yyyy-MM-dd，默认今天"));
        p.set("rate", prop("number", "小时费率（元），可选"));
        arr.add(tool("update_time_entry", "修改自己的工时记录（写操作，需用户确认后执行）", p, "timeEntryId", "caseId", "hours", "description"));

        p = mapper.createObjectNode();
        p.set("clientId", prop("integer", "客户 id"));
        p.set("timeEntryIds", prop("array", "已审核工时记录 id 数组"));
        p.set("issueDate", prop("string", "开票日期 yyyy-MM-dd，默认今天"));
        p.set("dueDate", prop("string", "到期日 yyyy-MM-dd，可选"));
        p.set("remark", prop("string", "备注，可选"));
        arr.add(tool("create_invoice", "创建账单（将已审核工时打包，需合伙人/管理员权限；写操作需用户确认）", p, "clientId", "timeEntryIds"));

        p = mapper.createObjectNode();
        p.set("invoiceId", prop("integer", "账单 id"));
        p.set("status", propEnum("目标状态", "ISSUED", "PAID", "VOID"));
        arr.add(tool("update_invoice_status", "变更账单状态（开票/收款/作废，需合伙人/管理员权限；写操作需用户确认）", p, "invoiceId", "status"));

        p = mapper.createObjectNode();
        p.set("eventId", prop("integer", "日程 id"));
        p.set("title", prop("string", "日程标题"));
        p.set("type", propEnum("日程类型，可选", "COURT", "MEETING", "TASK", "REMINDER"));
        p.set("startTime", prop("string", "开始时间，ISO 格式如 2024-06-01T09:30:00"));
        p.set("endTime", prop("string", "结束时间，ISO 格式，可选"));
        p.set("location", prop("string", "地点，可选"));
        p.set("description", prop("string", "备注，可选"));
        p.set("caseId", prop("integer", "关联案件 id，可选"));
        arr.add(tool("update_calendar_event", "修改自己创建的日程（写操作，需用户确认后执行）", p, "eventId", "title", "startTime"));

        p = mapper.createObjectNode();
        p.set("approvalId", prop("integer", "审批单 id"));
        p.set("approved", prop("boolean", "true 通过 / false 驳回"));
        p.set("comment", prop("string", "审批意见"));
        arr.add(tool("decide_approval", "审批待办（通过或驳回，仅被指定的审批人；写操作需用户确认）", p, "approvalId", "approved", "comment"));

        p = mapper.createObjectNode();
        p.set("approvalId", prop("integer", "审批单 id"));
        arr.add(tool("cancel_approval", "撤销自己提交的待审批申请（写操作，需用户确认后执行）", p, "approvalId"));

        p = mapper.createObjectNode();
        p.set("name", prop("string", "目录名称"));
        p.set("parentId", prop("integer", "上级目录 id，可选"));
        arr.add(tool("create_folder", "新建文档目录（写操作，需用户确认后执行）", p, "name"));

        p = mapper.createObjectNode();
        p.set("title", prop("string", "文章标题"));
        p.set("category", propEnum("分类", "EXPERIENCE", "LAW", "TEMPLATE", "TRAINING", "OTHER"));
        p.set("content", prop("string", "正文内容"));
        p.set("tags", prop("string", "标签，逗号分隔，可选"));
        p.set("published", prop("boolean", "是否发布，默认 true"));
        arr.add(tool("create_knowledge_article", "发布知识库文章（写操作，需用户确认后执行）", p, "title", "category", "content"));

        p = mapper.createObjectNode();
        p.set("articleId", prop("integer", "文章 id"));
        p.set("title", prop("string", "文章标题"));
        p.set("category", propEnum("分类", "EXPERIENCE", "LAW", "TEMPLATE", "TRAINING", "OTHER"));
        p.set("content", prop("string", "正文内容"));
        p.set("tags", prop("string", "标签，逗号分隔，可选"));
        p.set("published", prop("boolean", "是否发布，可选"));
        arr.add(tool("update_knowledge_article", "修改自己发布的知识库文章（写操作，需用户确认后执行）", p, "articleId", "title", "category", "content"));

        p = mapper.createObjectNode();
        p.set("username", prop("string", "登录用户名"));
        p.set("password", prop("string", "初始密码（至少 6 位）"));
        p.set("realName", prop("string", "姓名"));
        p.set("role", propEnum("角色", "ADMIN", "PARTNER", "LAWYER", "PARALEGAL", "STAFF"));
        p.set("email", prop("string", "邮箱，可选"));
        p.set("phone", prop("string", "电话，可选"));
        p.set("department", prop("string", "部门，可选"));
        p.set("title", prop("string", "职务，可选"));
        arr.add(tool("create_user", "新建成员账号（仅管理员；写操作需用户确认）", p, "username", "password", "realName", "role"));

        p = mapper.createObjectNode();
        p.set("userId", prop("integer", "成员 id"));
        p.set("username", prop("string", "登录用户名"));
        p.set("realName", prop("string", "姓名"));
        p.set("role", propEnum("角色", "ADMIN", "PARTNER", "LAWYER", "PARALEGAL", "STAFF"));
        p.set("email", prop("string", "邮箱，可选"));
        p.set("phone", prop("string", "电话，可选"));
        p.set("department", prop("string", "部门，可选"));
        p.set("title", prop("string", "职务，可选"));
        arr.add(tool("update_user", "修改成员信息（仅管理员；写操作需用户确认）", p, "userId", "username", "realName", "role"));

        p = mapper.createObjectNode();
        p.set("userId", prop("integer", "成员 id"));
        p.set("newPassword", prop("string", "新密码（6-32 位）"));
        arr.add(tool("reset_user_password", "重置成员密码（仅管理员；写操作需用户确认）", p, "userId", "newPassword"));

        return arr;
    }

    // ==================== 辅助方法 ====================

    /** 需要用户二次确认的写操作工具 */
    private static final Set<String> CONFIRM_TOOLS = Set.of(
            "record_time_entry", "create_calendar_event", "update_calendar_event",
            "create_approval", "decide_approval", "cancel_approval", "add_case_progress",
            "create_case", "update_case", "update_case_status",
            "create_client", "update_client", "add_client_interaction", "add_client_contact", "update_client_contact",
            "update_time_entry", "create_invoice", "update_invoice_status",
            "create_folder", "create_knowledge_article", "update_knowledge_article",
            "create_user", "update_user", "reset_user_password");

    /** 是否为写操作（需要用户确认后才执行） */
    public boolean requiresConfirmation(String name) {
        return CONFIRM_TOOLS.contains(name);
    }

    /** 生成人类可读的操作摘要，展示在确认卡片上 */
    public String describe(String name, String argsJson) {
        JsonNode a = parseArgs(argsJson);
        return switch (name) {
            case "record_time_entry" -> "记录工时：案件 #" + a.path("caseId").asText() + "，" + a.path("hours").asText() + " 小时";
            case "create_calendar_event" -> "创建日程：「" + a.path("title").asText("") + "」";
            case "update_calendar_event" -> "修改日程 #" + a.path("eventId").asText();
            case "create_approval" -> "发起审批：「" + a.path("title").asText("") + "」";
            case "decide_approval" -> (a.path("approved").asBoolean() ? "通过" : "驳回") + "审批 #" + a.path("approvalId").asText();
            case "cancel_approval" -> "撤销审批 #" + a.path("approvalId").asText();
            case "add_case_progress" -> "为案件 #" + a.path("caseId").asText() + "记录进展";
            case "create_case" -> "新建案件：「" + a.path("title").asText("") + "」";
            case "update_case" -> "修改案件 #" + a.path("caseId").asText();
            case "update_case_status" -> "案件 #" + a.path("caseId").asText() + " 状态变更为 " + a.path("status").asText("");
            case "create_client" -> "新建客户：「" + a.path("name").asText("") + "」";
            case "update_client" -> "修改客户 #" + a.path("clientId").asText();
            case "add_client_interaction" -> "为客户 #" + a.path("clientId").asText() + "添加跟进记录";
            case "add_client_contact" -> "为客户 #" + a.path("clientId").asText() + "添加联系人「" + a.path("name").asText("") + "」";
            case "update_client_contact" -> "修改客户 #" + a.path("clientId").asText() + " 的联系人 #" + a.path("contactId").asText();
            case "update_time_entry" -> "修改工时记录 #" + a.path("timeEntryId").asText();
            case "create_invoice" -> "为客户 #" + a.path("clientId").asText() + "创建账单（打包已审核工时）";
            case "update_invoice_status" -> "账单 #" + a.path("invoiceId").asText() + " 状态变更为 " + a.path("status").asText("");
            case "create_folder" -> "新建文档目录：「" + a.path("name").asText("") + "」";
            case "create_knowledge_article" -> "发布知识库文章：「" + a.path("title").asText("") + "」";
            case "update_knowledge_article" -> "修改知识库文章 #" + a.path("articleId").asText("");
            case "create_user" -> "新建成员账号：" + a.path("username").asText("") + "（" + a.path("realName").asText("") + "）";
            case "update_user" -> "修改成员信息 #" + a.path("userId").asText();
            case "reset_user_password" -> "重置成员 #" + a.path("userId").asText() + " 的密码";
            default -> "执行操作：" + name;
        };
    }

    /** 展示前对敏感字段（密码类）脱敏，真实参数仍保存在待确认记录中 */
    public String maskSensitive(String argsJson) {
        try {
            JsonNode node = mapper.readTree(argsJson);
            if (node.isObject()) {
                ObjectNode obj = (ObjectNode) node;
                if (obj.has("password") && obj.get("password").isTextual()) {
                    obj.put("password", "******");
                }
                if (obj.has("newPassword") && obj.get("newPassword").isTextual()) {
                    obj.put("newPassword", "******");
                }
            }
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return argsJson;
        }
    }

    private ObjectNode tool(String name, String desc, ObjectNode properties, String... required) {
        ObjectNode t = mapper.createObjectNode();
        t.put("type", "function");
        ObjectNode fn = t.putObject("function");
        fn.put("name", name);
        fn.put("description", desc);
        ObjectNode params = fn.putObject("parameters");
        params.put("type", "object");
        params.set("properties", properties);
        ArrayNode req = params.putArray("required");
        for (String r : required) {
            req.add(r);
        }
        return t;
    }

    private ObjectNode prop(String type, String desc) {
        ObjectNode p = mapper.createObjectNode();
        p.put("type", type);
        p.put("description", desc);
        return p;
    }

    private ObjectNode propEnum(String desc, String... values) {
        ObjectNode p = mapper.createObjectNode();
        p.put("type", "string");
        p.put("description", desc);
        ArrayNode en = p.putArray("enum");
        for (String v : values) {
            en.add(v);
        }
        return p;
    }

    private JsonNode parseArgs(String json) {
        try {
            if (StringUtils.hasText(json)) {
                return mapper.readTree(json);
            }
        } catch (Exception ignored) {
        }
        return mapper.createObjectNode();
    }

    private String optText(JsonNode n, String key) {
        return (n.has(key) && !n.get(key).isNull()) ? n.get(key).asText() : null;
    }

    private long asLong(JsonNode n, String key, long def) {
        JsonNode v = n.get(key);
        if (v == null || v.isNull()) {
            return def;
        }
        if (v.isNumber()) {
            return v.asLong();
        }
        try {
            return Long.parseLong(v.asText().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private BigDecimal asDecimal(JsonNode n, String key) {
        JsonNode v = n.get(key);
        if (v == null || v.isNull()) {
            return null;
        }
        if (v.isNumber()) {
            return v.decimalValue();
        }
        try {
            return new BigDecimal(v.asText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private List<Long> listOf(JsonNode n, String key) {
        JsonNode v = n.get(key);
        if (v == null || !v.isArray()) {
            return null;
        }
        List<Long> list = new ArrayList<>();
        for (JsonNode e : v) {
            if (e.isNumber()) {
                list.add(e.asLong());
            } else {
                try {
                    list.add(Long.parseLong(e.asText().trim()));
                } catch (Exception ignored) {
                }
            }
        }
        return list;
    }

    private boolean hasDate(JsonNode n, String key) {
        JsonNode v = n.get(key);
        return v != null && !v.isNull() && StringUtils.hasText(v.asText());
    }

    private String write(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{\"error\":\"结果序列化失败\"}";
        }
    }

    private LocalDateTime parseDateTime(String s) {
        if (!StringUtils.hasText(s)) {
            throw new BizException("缺少时间参数");
        }
        String t = s.trim();
        try {
            return LocalDateTime.parse(t);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(t).atStartOfDay();
        } catch (Exception ignored) {
        }
        throw new BizException("无法解析时间：" + s);
    }
}
