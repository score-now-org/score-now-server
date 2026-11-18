package com.scorenow.scorenow_api.domain.inquiry;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;

public interface InquiryRepository extends JpaRepository<Inquiry, Long>, JpaSpecificationExecutor<Inquiry> {
    Page<Inquiry> findByUser_Id(Long userId, Pageable pageable);
    Optional<Inquiry> findByIdAndUser_Id(Long id, Long userId);
    @Override @EntityGraph(attributePaths = "user")
    Page<Inquiry> findAll(Specification<Inquiry> spec, Pageable pageable);
}
