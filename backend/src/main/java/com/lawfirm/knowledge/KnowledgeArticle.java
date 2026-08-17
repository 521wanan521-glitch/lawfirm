package com.lawfirm.knowledge;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 知识库文章
 */
@Getter
@Setter
@Entity
@Table(name = "know_article", indexes = {
        @Index(name = "idx_know_category", columnList = "category")
})
public class KnowledgeArticle extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeCategory category = KnowledgeCategory.OTHER;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Long authorId;

    @Column(length = 200)
    private String tags;

    @Column(nullable = false)
    private Integer viewCount = 0;

    /** 是否发布（草稿仅作者可见） */
    @Column(nullable = false)
    private Boolean published = true;
}
