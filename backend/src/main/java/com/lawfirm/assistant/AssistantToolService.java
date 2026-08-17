package com.lawfirm.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lawfirm.approval.ApprovalService;
import com.lawfirm.approval.ApprovalStatus;
import com.lawfirm.approval.dto.InstanceRequest;
import com.lawfirm.approval.dto.InstanceView;
import com.lawfirm.billing.BillingService;
import com.lawfirm.billing.dto.TimeEntryRequest;
import com.lawfirm.billing.dto.TimeEntryView;
import com.lawfirm.calendar.CalendarService;
import com.lawfirm.calendar.EventType;
import com.lawfirm.calendar.dto.EventRequest;
import com.lawfirm.calendar.dto.EventView;
import com.lawfirm.cases.CaseService;
import com.lawfirm.cases.dto.CaseProgressRequest;
import com.lawfirm.cases.dto.CaseProgressView;
import com.lawfirm.cases.dto.CaseView;
import com.lawfirm.client.ClientService;
import com.lawfirm.client.dto.ClientView;
import com.lawfirm.client.dto.InteractionView;
import com.lawfirm.common.BizException;
import com.lawfirm.common.PageResult;
import com.lawfirm.dashboard.DashboardService;
import com.lawfirm.document.DocumentService;
import com.lawfirm.document.dto.DocumentView;
import com.lawfirm.knowledge.KnowledgeArticle;
import com.lawfirm.knowledge.KnowledgeRepository;
import com.lawfirm.security.CurrentUser;
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
    private final DocumentService documentService;
    private final ApprovalService approvalService;
    private final DashboardService dashboardService;

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

        return arr;
    }

    // ==================== 辅助方法 ====================

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
