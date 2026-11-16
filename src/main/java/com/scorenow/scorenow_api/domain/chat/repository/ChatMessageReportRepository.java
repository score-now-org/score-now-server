package com.scorenow.scorenow_api.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessageReport;

public interface ChatMessageReportRepository extends JpaRepository<ChatMessageReport, Long> {
    Page<ChatMessageReport> findByMessage_IdOrderByIdDesc(Long messageId, Pageable pageable);
    boolean existsByMessage_IdAndReporter_Id(Long messageId, Long reporterId);
}
