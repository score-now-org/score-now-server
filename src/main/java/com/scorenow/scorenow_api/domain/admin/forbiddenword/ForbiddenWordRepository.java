package com.scorenow.scorenow_api.domain.admin.forbiddenword;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForbiddenWordRepository extends JpaRepository<ForbiddenWord, Long> {
    Page<ForbiddenWord> findByWordContainingIgnoreCase(String word, Pageable pageable);
    boolean existsByWord(String word);
}
