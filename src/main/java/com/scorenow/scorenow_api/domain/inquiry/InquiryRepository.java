package com.scorenow.scorenow_api.domain.inquiry;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface InquiryRepository extends JpaRepository<Inquiry, Long>, JpaSpecificationExecutor<Inquiry> {
    Page<Inquiry> findByUser_Id(Long userId, Pageable pageable);
    Optional<Inquiry> findByIdAndUser_Id(Long id, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inquiry i where i.id = :id and i.user.id = :userId")
    Optional<Inquiry> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inquiry i where i.id = :id")
    Optional<Inquiry> findForUpdate(@Param("id") Long id);
    @Override @EntityGraph(attributePaths = "user")
    Page<Inquiry> findAll(Specification<Inquiry> spec, Pageable pageable);
}
