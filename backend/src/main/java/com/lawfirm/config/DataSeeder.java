package com.lawfirm.config;

import com.lawfirm.approval.ApprovalTemplate;
import com.lawfirm.approval.ApprovalTemplateRepository;
import com.lawfirm.approval.ApprovalType;
import com.lawfirm.calendar.CalendarEvent;
import com.lawfirm.calendar.CalendarEventRepository;
import com.lawfirm.calendar.EventType;
import com.lawfirm.cases.Case;
import com.lawfirm.cases.CaseRepository;
import com.lawfirm.cases.CaseType;
import com.lawfirm.cases.Priority;
import com.lawfirm.client.Client;
import com.lawfirm.client.ClientRepository;
import com.lawfirm.knowledge.KnowledgeArticle;
import com.lawfirm.knowledge.KnowledgeCategory;
import com.lawfirm.knowledge.KnowledgeRepository;
import com.lawfirm.user.User;
import com.lawfirm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;

/**
 * 首次启动种子数据：默认账号、审批模板与演示数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApprovalTemplateRepository templateRepository;
    private final ClientRepository clientRepository;
    private final CaseRepository caseRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final CalendarEventRepository calendarEventRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("检测到空数据库，初始化默认账号与演示数据...");

        User admin = createUser("admin", "admin123", "系统管理员", User.Role.ADMIN, "管理部", "系统管理员");
        User partner = createUser("partner", "partner123", "张合伙人", User.Role.PARTNER, "合伙人团队", "高级合伙人");
        User lawyer1 = createUser("lawyer1", "lawyer123", "李律师", User.Role.LAWYER, "民商事部", "执业律师");
        User lawyer2 = createUser("lawyer2", "lawyer123", "王律师", User.Role.LAWYER, "刑事部", "执业律师");
        User paralegal = createUser("paralegal", "paralegal123", "赵助理", User.Role.PARALEGAL, "民商事部", "律师助理");
        User staff = createUser("staff", "staff123", "钱行政", User.Role.STAFF, "行政部", "行政专员");

        seedTemplates();
        seedDemo(admin, partner, lawyer1, lawyer2, paralegal, staff);

        log.info("默认账号：admin/admin123、partner/partner123、lawyer1/lawyer123（请上线后立即修改密码）");
    }

    private User createUser(String username, String rawPassword, String realName, User.Role role,
                            String department, String title) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setRole(role);
        user.setDepartment(department);
        user.setTitle(title);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private void seedTemplates() {
        saveTemplate("用章申请", ApprovalType.SEAL, "公章/合同专用章使用申请");
        saveTemplate("请假申请", ApprovalType.LEAVE, "事假、病假、年假申请");
        saveTemplate("报销申请", ApprovalType.EXPENSE, "差旅费、办公费等报销");
        saveTemplate("立案审批", ApprovalType.CASE_FILING, "新案件立案审批");
        saveTemplate("其他审批", ApprovalType.OTHER, "其他事项审批");
    }

    private void saveTemplate(String name, ApprovalType type, String desc) {
        ApprovalTemplate t = new ApprovalTemplate();
        t.setName(name);
        t.setType(type);
        t.setDescription(desc);
        t.setEnabled(true);
        templateRepository.save(t);
    }

    private void seedDemo(User admin, User partner, User lawyer1, User lawyer2, User paralegal, User staff) {
        // 演示客户
        Client c1 = new Client();
        c1.setName("华晨科技有限公司");
        c1.setType(Client.Type.COMPANY);
        c1.setIdNumber("91310000MA1KXXXXXX");
        c1.setIndustry("信息技术");
        c1.setAddress("上海市浦东新区张江高科技园区");
        c1.setPhone("021-55551234");
        c1.setLevel(Client.Level.A);
        c1.setSource("老客户转介绍");
        c1.setOwnerId(lawyer1.getId());
        c1.setRemark("年度顾问单位");
        clientRepository.save(c1);

        Client c2 = new Client();
        c2.setName("陈建国");
        c2.setType(Client.Type.PERSONAL);
        c2.setIndustry("个体经营");
        c2.setAddress("上海市徐汇区");
        c2.setPhone("13800138000");
        c2.setLevel(Client.Level.B);
        c2.setSource("官网咨询");
        c2.setOwnerId(lawyer2.getId());
        clientRepository.save(c2);

        // 演示案件
        int year = Year.now().getValue();
        Case case1 = new Case();
        case1.setCaseNo(String.format("LF%d-0001", year));
        case1.setTitle("华晨科技诉某某软件合同纠纷案");
        case1.setClientId(c1.getId());
        case1.setType(CaseType.COMMERCIAL);
        case1.setPriority(Priority.HIGH);
        case1.setLeadLawyerId(lawyer1.getId());
        case1.setCoLawyerIds(java.util.List.of(paralegal.getId()));
        case1.setCourt("上海市浦东新区人民法院");
        case1.setCaseAmount(new BigDecimal("860000"));
        case1.setFilingDate(LocalDate.now().minusDays(15));
        case1.setFee(new BigDecimal("80000"));
        case1.setDescription("软件采购合同履行争议，涉及验收条款与付款条件。");
        case1.setStatus(com.lawfirm.cases.CaseStatus.ACTIVE);
        caseRepository.save(case1);

        // 演示知识文章
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle("合同审查要点清单（试用版）");
        article.setCategory(KnowledgeCategory.EXPERIENCE);
        article.setContent("一、主体资格审查：营业执照、法定代表人身份证明、授权委托书...\n\n" +
                "二、标的条款审查：标的物描述、质量标准、验收方式...\n\n" +
                "三、违约条款审查：违约金比例、赔偿范围、免责条款...\n\n" +
                "四、争议解决条款：管辖法院/仲裁机构约定、适用法律...\n\n" +
                "五、签署程序：盖章、签字、骑缝章、日期...");
        article.setAuthorId(lawyer1.getId());
        article.setTags("合同,审查,要点");
        article.setPublished(true);
        knowledgeRepository.save(article);

        // 演示日程
        CalendarEvent event = new CalendarEvent();
        event.setTitle("华晨科技案一审开庭");
        event.setType(EventType.COURT);
        event.setStartTime(LocalDateTime.now().plusDays(3).withHour(9).withMinute(30));
        event.setEndTime(LocalDateTime.now().plusDays(3).withHour(11).withMinute(30));
        event.setLocation("上海市浦东新区人民法院 第二法庭");
        event.setDescription("携带证据原件及代理手续");
        event.setCreatorId(lawyer1.getId());
        event.setCaseId(case1.getId());
        event.setParticipantIds(java.util.List.of(lawyer1.getId(), paralegal.getId()));
        calendarEventRepository.save(event);

        // 演示审批
        // （审批实例由员工实际发起，此处不预置）
    }
}
