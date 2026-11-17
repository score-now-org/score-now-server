package com.scorenow.scorenow_api.domain.terms;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsRevisionRepository extends JpaRepository<TermsRevision, Long> {
    Optional<TermsRevision> findFirstByOrderByIdDesc();
}
