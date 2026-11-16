package com.scorenow.scorenow_api.domain.admin.member.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.scorenow.scorenow_api.domain.admin.member.entity.MemberModerationEvent;

public interface MemberModerationEventRepository extends JpaRepository<MemberModerationEvent, Long> {
    List<MemberModerationEvent> findByUserIdOrderByIdDesc(Long userId);
}
