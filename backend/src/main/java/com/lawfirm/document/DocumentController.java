package com.lawfirm.document;

import com.lawfirm.common.ApiResponse;
import com.lawfirm.common.PageResult;
import com.lawfirm.document.dto.DocumentVersionView;
import com.lawfirm.document.dto.DocumentView;
import com.lawfirm.document.dto.FolderRequest;
import com.lawfirm.document.dto.FolderView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ApiResponse<PageResult<DocumentView>> page(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long caseId,
                                                      @RequestParam(required = false) Long clientId,
                                                      @RequestParam(required = false) Long folderId,
                                                      @RequestParam(required = false) DocCategory category,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(documentService.page(keyword, caseId, clientId, folderId, category, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentView> detail(@PathVariable Long id) {
        return ApiResponse.ok(documentService.detail(id));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<DocumentVersionView>> versions(@PathVariable Long id) {
        return ApiResponse.ok(documentService.versions(id));
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentView> upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) Long caseId,
                                            @RequestParam(required = false) Long clientId,
                                            @RequestParam(required = false) Long folderId,
                                            @RequestParam(required = false) DocCategory category,
                                            @RequestParam(required = false) String description) {
        return ApiResponse.ok(documentService.upload(file, caseId, clientId, folderId, category, description));
    }

    @PostMapping("/{id}/version")
    public ApiResponse<DocumentView> addVersion(@PathVariable Long id,
                                                @RequestParam("file") MultipartFile file,
                                                @RequestParam(required = false) String remark) {
        return ApiResponse.ok(documentService.addVersion(id, file, remark));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, @RequestParam(required = false) Integer version) {
        DownloadContent content = documentService.download(id, version);
        String encoded = URLEncoder.encode(content.originalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        content.contentType() == null ? "application/octet-stream" : content.contentType()))
                .contentLength(content.size())
                .body(new FileSystemResource(content.path()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.ok();
    }

    // ---------- 目录 ----------

    @GetMapping("/folders")
    public ApiResponse<List<FolderView>> folderTree() {
        return ApiResponse.ok(documentService.folderTree());
    }

    @PostMapping("/folders")
    public ApiResponse<FolderView> createFolder(@Valid @RequestBody FolderRequest request) {
        return ApiResponse.ok(documentService.createFolder(request));
    }

    @DeleteMapping("/folders/{id}")
    public ApiResponse<Void> deleteFolder(@PathVariable Long id) {
        documentService.deleteFolder(id);
        return ApiResponse.ok();
    }
}
