package com.scorenow.scorenow_api.domain.admin.forbiddenword;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ForbiddenWordRepository extends JpaRepository<ForbiddenWord, Long> {
    Page<ForbiddenWord> findByWordContainingIgnoreCase(String word, Pageable pageable);
    boolean existsByWordIgnoreCase(String word);

    @Query("select f from ForbiddenWord f order by length(f.word) desc, f.id asc")
    List<ForbiddenWord> findAllForFiltering();
}
