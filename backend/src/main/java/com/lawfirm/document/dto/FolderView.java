package com.lawfirm.document.dto;

import java.util.ArrayList;
import java.util.List;

public record FolderView(
        Long id,
        String name,
        Long parentId,
        List<FolderView> children
) {
    public static FolderView of(Long id, String name, Long parentId) {
        return new FolderView(id, name, parentId, new ArrayList<>());
    }
}
