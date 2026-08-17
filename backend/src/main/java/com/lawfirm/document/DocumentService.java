package com.lawfirm.document;

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
import java.util.Map;
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Document> result = documentRepository.findAll(spec, pageable);
        return PageResult.of(result, this::toView);
    }

    public DocumentView detail(Long id) {
        return toView(getById(id));
    }

    public List<DocumentVersionView> versions(Long id) {
        getById(id);
        List<DocumentVersion> versions = versionRepository.findByDocumentIdOrderByVersionDesc(id);
        Map<Long, String> userNames = userNameMap(versions.stream().map(DocumentVersion::getUploadedBy).distinct().toList());
        return versions.stream()
                .map(v -> new DocumentVersionView(v.getVersion(), v.getSize(), v.getUploadedBy(),
                        userNames.getOrDefault(v.getUploadedBy(), ""), v.getRemark(), v.getCreatedAt()))
                .toList();
    }

    public DownloadContent download(Long id, Integer version) {
        Document doc = getById(id);
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
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
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

    private Path resolvePath(String storedName) {
        return Paths.get(uploadDir).resolve(storedName).normalize();
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
