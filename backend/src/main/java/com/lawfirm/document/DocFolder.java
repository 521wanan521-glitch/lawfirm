package com.lawfirm.document;

import com.lawfirm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 文档目录（支持多级）
 */
@Getter
@Setter
@Entity
@Table(name = "doc_folder", indexes = {
        @Index(name = "idx_folder_parent", columnList = "parentId")
})
public class DocFolder extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    /** 上级目录，null 表示根目录 */
    @Column
    private Long parentId;

    @Column(nullable = false)
    private Long createdBy;
}
