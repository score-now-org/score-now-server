package com.scorenow.scorenow_api.domain.terms;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TermsLockRepository extends JpaRepository<TermsLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from TermsLock l where l.id = 1")
    Optional<TermsLock> lockForPublish();
}
