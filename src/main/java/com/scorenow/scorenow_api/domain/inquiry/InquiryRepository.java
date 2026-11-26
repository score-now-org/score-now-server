package com.scorenow.scorenow_api.domain.inquiry;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    Page<Inquiry> findByUser_Id(Long userId, Pageable pageable);
    Optional<Inquiry> findByIdAndUser_Id(Long id, Long userId);
}
