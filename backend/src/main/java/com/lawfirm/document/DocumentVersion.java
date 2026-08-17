package com.lawfirm.document;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 文档历史版本
 */
@Getter
@Setter
@Entity
@Table(name = "doc_version", indexes = {
        @Index(name = "idx_version_doc", columnList = "documentId")
})
public class DocumentVersion extends BaseEntity {

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 100)
    private String storedName;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private Long uploadedBy;

    @Column(length = 500)
    private String remark;
}
