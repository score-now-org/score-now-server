package com.scorenow.scorenow_api.domain.notice;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @Query("select n from Notice n where n.published = true and n.publishedPlatform in :platforms and (:tickerOnly = false or n.publishedTicker = true) order by n.publishedSortOrder asc, n.id desc")
    Page<Notice> findPublished(@Param("platforms") Collection<Notice.Platform> platforms, @Param("tickerOnly") boolean tickerOnly, Pageable pageable);
    Optional<Notice> findByIdAndPublishedTrueAndPublishedPlatformIn(Long id, Collection<Notice.Platform> platforms);
    List<Notice> findByDeletedFalseOrderBySortOrderAscIdDesc();
    Page<Notice> findByDeletedFalse(Pageable pageable);
    Page<Notice> findByDeletedFalseAndPlatform(Notice.Platform platform, Pageable pageable);
    Optional<Notice> findByIdAndDeletedFalse(Long id);
}
