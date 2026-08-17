package com.lawfirm.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocFolderRepository extends JpaRepository<DocFolder, Long> {

    List<DocFolder> findByParentIdIsNullOrderByCreatedAtAsc();

    List<DocFolder> findAllByOrderByCreatedAtAsc();
}
