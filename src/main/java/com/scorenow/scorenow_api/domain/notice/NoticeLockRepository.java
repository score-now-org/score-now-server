package com.scorenow.scorenow_api.domain.notice;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface NoticeLockRepository extends JpaRepository<NoticeLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from NoticeLock l where l.id = 1")
    Optional<NoticeLock> lockForEdit();
}
