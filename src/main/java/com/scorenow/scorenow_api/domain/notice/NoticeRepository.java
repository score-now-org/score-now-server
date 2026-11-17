package com.scorenow.scorenow_api.domain.notice;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @Query("select n from Notice n where n.kind = :kind and n.published = true and n.publishedPlatform in :platforms and (:tickerOnly = false or n.publishedTicker = true) order by n.publishedSortOrder asc, n.id desc")
    Page<Notice> findPublished(@Param("kind") Notice.Kind kind, @Param("platforms") Collection<Notice.Platform> platforms, @Param("tickerOnly") boolean tickerOnly, Pageable pageable);
    Optional<Notice> findByIdAndKindAndPublishedTrueAndPublishedPlatformIn(Long id, Notice.Kind kind, Collection<Notice.Platform> platforms);
    List<Notice> findByKindAndDeletedFalseOrderBySortOrderAscIdDesc(Notice.Kind kind);
    Page<Notice> findByKindAndDeletedFalse(Notice.Kind kind, Pageable pageable);
    Page<Notice> findByKindAndDeletedFalseAndPlatform(Notice.Kind kind, Notice.Platform platform, Pageable pageable);
    Optional<Notice> findByIdAndKindAndDeletedFalse(Long id, Notice.Kind kind);
    boolean existsByKindAndModifiedTrue(Notice.Kind kind);
    List<Notice> findByKind(Notice.Kind kind);
}
