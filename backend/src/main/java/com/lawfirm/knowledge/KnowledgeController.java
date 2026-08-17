package com.lawfirm.knowledge;

import com.lawfirm.common.ApiResponse;
import com.lawfirm.common.PageResult;
import com.lawfirm.knowledge.dto.ArticleRequest;
import com.lawfirm.knowledge.dto.ArticleView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public ApiResponse<PageResult<ArticleView>> page(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) KnowledgeCategory category,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(knowledgeService.page(keyword, category, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleView> detail(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeService.detail(id));
    }

    @PostMapping
    public ApiResponse<ArticleView> create(@Valid @RequestBody ArticleRequest request) {
        return ApiResponse.ok(knowledgeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleView> update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return ApiResponse.ok(knowledgeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ApiResponse.ok();
    }
}
