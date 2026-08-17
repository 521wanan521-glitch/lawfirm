package com.lawfirm.knowledge.dto;

import com.lawfirm.knowledge.KnowledgeCategory;

import java.time.LocalDateTime;

public record ArticleView(
        Long id,
        String title,
        KnowledgeCategory category,
        String content,
        Long authorId,
        String authorName,
        String tags,
        Integer viewCount,
        Boolean published,
        LocalDateTime createdAt
) {
}
