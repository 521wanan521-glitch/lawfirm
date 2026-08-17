package com.lawfirm.document;

import com.lawfirm.cases.Case;
import com.lawfirm.cases.CaseRepository;
import com.lawfirm.client.Client;
import com.lawfirm.client.ClientRepository;
import com.lawfirm.common.BizException;
import com.lawfirm.common.PageResult;
import com.lawfirm.document.dto.DocumentVersionView;
import com.lawfirm.document.dto.DocumentView;
import com.lawfirm.document.dto.FolderRequest;
import com.lawfirm.document.dto.FolderView;
import com.lawfirm.security.CurrentUser;
import com.lawfirm.user.User;
import com.lawfirm.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    /** 单文件大小上限：200MB */
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocFolderRepository folderRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final ClientRepository clientRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    // ==================== 上传/版本 ====================

    @Transactional
    public DocumentView upload(MultipartFile file, Long caseId, Long clientId, Long folderId,
                               DocCategory category, String description) {
        validateFile(file);
        if (folderId != null) {
            folderRepository.findById(folderId).orElseThrow(() -> new BizException("目录不存在"));
        }
        String storedName = storeFile(file);
        try {
            Document doc = new Document();
            doc.setName(file.getOriginalFilename());
            doc.setStoredName(storedName);
            doc.setSize(file.getSize());
            doc.setContentType(file.getContentType());
            doc.setUploadedBy(CurrentUser.id());
            doc.setVersion(1);
            doc.setCaseId(caseId);
            doc.setClientId(clientId);
            doc.setFolderId(folderId);
            doc.setCategory(category == null ? DocCategory.OTHER : category);
            doc.setDescription(description);
            doc = documentRepository.save(doc);

            DocumentVersion dv = new DocumentVersion();
            dv.setDocumentId(doc.getId());
            dv.setVersion(1);
            dv.setStoredName(storedName);
            dv.setSize(file.getSize());
            dv.setUploadedBy(CurrentUser.id());
            versionRepository.save(dv);
            return toView(doc);
        } catch (Exception e) {
            deleteStoredFile(storedName);
            throw e;
        }
    }

    @Transactional
    public DocumentView addVersion(Long id, MultipartFile file, String remark) {
        validateFile(file);
        Document doc = documentRepository.findById(id).orElseThrow(() -> new BizException("文档不存在"));
        checkAccess(doc);
        String storedName = storeFile(file);
        try {
            Integer newVersion = doc.getVersion() + 1;
            DocumentVersion dv = new DocumentVersion();
            dv.setDocumentId(doc.getId());
            dv.setVersion(newVersion);
            dv.setStoredName(storedName);
            dv.setSize(file.getSize());
            dv.setUploadedBy(CurrentUser.id());
            dv.setRemark(remark);
            versionRepository.save(dv);

            doc.setStoredName(storedName);
            doc.setSize(file.getSize());
            doc.setContentType(file.getContentType());
            doc.setVersion(newVersion);
            doc.setName(file.getOriginalFilename());
            doc = documentRepository.save(doc);
            return toView(doc);
        } catch (Exception e) {
            deleteStoredFile(storedName);
            throw e;
        }
    }

    // ==================== 查询 ====================

    public PageResult<DocumentView> page(String keyword, Long caseId, Long clientId, Long folderId,
                                         DocCategory category, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Document> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("name"), "%" + keyword.trim() + "%"));
            }
            if (caseId != null) predicates.add(cb.equal(root.get("caseId"), caseId));
            if (clientId != null) predicates.add(cb.equal(root.get("clientId"), clientId));
            if (folderId != null) predicates.add(cb.equal(root.get("folderId"), folderId));
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            if (!CurrentUser.isManager()) {
                Long me = CurrentUser.id();
                List<Long> caseIds = myCaseIds(me);
                List<Long> clientIds = myClientIds(me);
                List<Predicate> ors = new ArrayList<>();
                ors.add(cb.equal(root.get("uploadedBy"), me));
                if (!caseIds.isEmpty()) {
                    ors.add(root.get("caseId").in(caseIds));
                }
                if (!clientIds.isEmpty()) {
                    ors.add(root.get("clientId").in(clientIds));
                }
                predicates.add(cb.or(ors.toArray(new Predicate[0])));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Document> result = documentRepository.findAll(spec, pageable);
        return PageResult.of(result, this::toView);
    }

    public DocumentView detail(Long id) {
        Document doc = getById(id);
        checkAccess(doc);
        return toView(doc);
    }

    public List<DocumentVersionView> versions(Long id) {
        Document doc = getById(id);
        checkAccess(doc);
        List<DocumentVersion> versions = versionRepository.findByDocumentIdOrderByVersionDesc(id);
        Map<Long, String> userNames = userNameMap(versions.stream().map(DocumentVersion::getUploadedBy).distinct().toList());
        return versions.stream()
                .map(v -> new DocumentVersionView(v.getVersion(), v.getSize(), v.getUploadedBy(),
                        userNames.getOrDefault(v.getUploadedBy(), ""), v.getRemark(), v.getCreatedAt()))
                .toList();
    }

    public DownloadContent download(Long id, Integer version) {
        Document doc = getById(id);
        checkAccess(doc);
        int target = version == null ? doc.getVersion() : version;
        DocumentVersion dv = versionRepository.findByDocumentIdAndVersion(id, target)
                .orElseThrow(() -> new BizException("版本不存在"));
        Path path = resolvePath(dv.getStoredName());
        if (!Files.exists(path)) {
            throw new BizException("文件已丢失，请联系管理员");
        }
        return new DownloadContent(path, doc.getName(), doc.getContentType(), dv.getSize());
    }

    @Transactional
    public void delete(Long id) {
        Document doc = getById(id);
        Long me = CurrentUser.id();
        if (!doc.getUploadedBy().equals(me) && !CurrentUser.isAdmin()) {
            throw new BizException(403, "只能删除自己上传的文档");
        }
        versionRepository.findByDocumentIdOrderByVersionDesc(id)
                .forEach(v -> deleteStoredFile(v.getStoredName()));
        versionRepository.deleteByDocumentId(id);
        documentRepository.delete(doc);
    }

    // ==================== 目录 ====================

    public List<FolderView> folderTree() {
        List<DocFolder> all = folderRepository.findAllByOrderByCreatedAtAsc();
        Map<Long, FolderView> map = new HashMap<>();
        for (DocFolder f : all) {
            map.put(f.getId(), FolderView.of(f.getId(), f.getName(), f.getParentId()));
        }
        List<FolderView> roots = new ArrayList<>();
        for (DocFolder f : all) {
            FolderView view = map.get(f.getId());
            if (f.getParentId() == null || !map.containsKey(f.getParentId())) {
                roots.add(view);
            } else {
                map.get(f.getParentId()).children().add(view);
            }
        }
        return roots;
    }

    @Transactional
    public FolderView createFolder(FolderRequest request) {
        if (request.parentId() != null) {
            folderRepository.findById(request.parentId()).orElseThrow(() -> new BizException("上级目录不存在"));
        }
        DocFolder folder = new DocFolder();
        folder.setName(request.name());
        folder.setParentId(request.parentId());
        folder.setCreatedBy(CurrentUser.id());
        folder = folderRepository.save(folder);
        return FolderView.of(folder.getId(), folder.getName(), folder.getParentId());
    }

    @Transactional
    public void deleteFolder(Long id) {
        DocFolder folder = folderRepository.findById(id).orElseThrow(() -> new BizException("目录不存在"));
        if (folderRepository.findAllByOrderByCreatedAtAsc().stream().anyMatch(f -> id.equals(f.getParentId()))) {
            throw new BizException("请先删除子目录");
        }
        if (documentRepository.findAll().stream().anyMatch(d -> id.equals(d.getFolderId()))) {
            throw new BizException("目录下存在文档，请先移动或删除");
        }
        folderRepository.delete(folder);
    }

    // ==================== 私有方法 ====================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException("文件大小超过 200MB 限制");
        }
    }

    private String storeFile(MultipartFile file) {
        try {
            String ext = safeExtension(file.getOriginalFilename());
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String storedName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = resolvePath(storedName);
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            return storedName;
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
    }

    /** 从原始文件名安全提取扩展名（白名单，杜绝路径穿越） */
    private String safeExtension(String original) {
        if (original == null) {
            return "";
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        Set<String> allowed = Set.of(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
                ".txt", ".md", ".csv", ".jpg", ".jpeg", ".png", ".gif", ".zip", ".rar", ".7z", ".eml", ".msg");
        if (!allowed.contains(ext)) {
            throw new BizException("不支持的文件类型：" + ext);
        }
        return ext;
    }

    private Path resolvePath(String storedName) {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolved = base.resolve(storedName).normalize();
        if (!resolved.startsWith(base)) {
            throw new BizException("非法文件路径");
        }
        return resolved;
    }

    private void deleteStoredFile(String storedName) {
        try {
            Files.deleteIfExists(resolvePath(storedName));
        } catch (IOException e) {
            log.warn("删除文件失败: {}", storedName, e);
        }
    }

    private Document getById(Long id) {
        return documentRepository.findById(id).orElseThrow(() -> new BizException("文档不存在"));
    }

    // ---------- 权限 ----------

    private void checkAccess(Document doc) {
        if (CurrentUser.isManager()) {
            return;
        }
        Long me = CurrentUser.id();
        if (me.equals(doc.getUploadedBy())) {
            return;
        }
        if (doc.getCaseId() != null) {
            Case c = caseRepository.findById(doc.getCaseId()).orElse(null);
            if (c != null && isCaseMember(c, me)) {
                return;
            }
        }
        if (doc.getClientId() != null) {
            Client cl = clientRepository.findById(doc.getClientId()).orElse(null);
            if (cl != null && me.equals(cl.getOwnerId())) {
                return;
            }
        }
        throw new BizException(403, "无权访问该文档");
    }

    private boolean isCaseMember(Case c, Long userId) {
        return userId.equals(c.getLeadLawyerId())
                || (c.getCoLawyerIds() != null && c.getCoLawyerIds().contains(userId));
    }

    private List<Long> myCaseIds(Long me) {
        Specification<Case> spec = (root, q, cb) -> cb.or(
                cb.equal(root.get("leadLawyerId"), me),
                cb.isMember(me, root.get("coLawyerIds")));
        return caseRepository.findAll(spec).stream().map(Case::getId).toList();
    }

    private List<Long> myClientIds(Long me) {
        return clientRepository.findAll((root, q, cb) -> cb.equal(root.get("ownerId"), me))
                .stream().map(Client::getId).toList();
    }

    private DocumentView toView(Document d) {
        String folderName = "";
        if (d.getFolderId() != null) {
            folderName = folderRepository.findById(d.getFolderId()).map(DocFolder::getName).orElse("");
        }
        String uploaderName = userRepository.findById(d.getUploadedBy()).map(User::getRealName).orElse("");
        return new DocumentView(d.getId(), d.getName(), d.getSize(), d.getContentType(), d.getCategory(),
                d.getCaseId(), d.getClientId(), d.getFolderId(), folderName, d.getVersion(),
                d.getUploadedBy(), uploaderName, d.getDescription(), d.getCreatedAt());
    }

    private Map<Long, String> userNameMap(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getRealName));
    }
}
