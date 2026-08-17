package com.lawfirm.document;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 文档元数据（当前版本）
 */
@Getter
@Setter
@Entity
@Table(name = "doc_document", indexes = {
        @Index(name = "idx_doc_case", columnList = "caseId"),
        @Index(name = "idx_doc_client", columnList = "clientId"),
        @Index(name = "idx_doc_folder", columnList = "folderId")
})
public class Document extends BaseEntity {

    /** 显示名称（原始文件名） */
    @Column(nullable = false, length = 255)
    private String name;

    /** 服务器存储文件名（UUID） */
    @Column(nullable = false, length = 100)
    private String storedName;

    @Column(nullable = false)
    private Long size;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long uploadedBy;

    /** 当前版本号，从 1 开始 */
    @Column(nullable = false)
    private Integer version = 1;

    @Column
    private Long caseId;

    @Column
    private Long clientId;

    @Column
    private Long folderId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DocCategory category = DocCategory.OTHER;

    @Column(length = 500)
    private String description;
}
