package com.lawfirm.document.dto;

import jakarta.validation.constraints.NotBlank;

public record FolderRequest(
        @NotBlank(message = "目录名称不能为空") String name,
        Long parentId
) {
}
