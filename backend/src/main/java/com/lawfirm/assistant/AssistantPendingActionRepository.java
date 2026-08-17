package com.lawfirm.assistant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssistantPendingActionRepository extends JpaRepository<AssistantPendingAction, Long> {

    List<AssistantPendingAction> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, AssistantPendingAction.Status status);

    List<AssistantPendingAction> findBySessionIdAndStatusOrderByCreatedAtDesc(Long sessionId, AssistantPendingAction.Status status);
}
