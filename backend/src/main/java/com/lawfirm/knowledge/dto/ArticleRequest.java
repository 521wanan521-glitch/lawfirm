package com.lawfirm.knowledge.dto;

import com.lawfirm.knowledge.KnowledgeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ArticleRequest(
        @NotBlank(message = "标题不能为空") String title,
        @NotNull(message = "分类不能为空") KnowledgeCategory category,
        @NotBlank(message = "内容不能为空") String content,
        String tags,
        Boolean published
) {
}
