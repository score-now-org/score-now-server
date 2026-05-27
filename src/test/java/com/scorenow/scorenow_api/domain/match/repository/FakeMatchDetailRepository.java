package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument.ExtraTime;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeMatchDetailRepository implements MatchDetailRepository {

    private final AtomicLong idGenerator = new AtomicLong(0);
    private final Map<Long, MatchDetailDocument> database = new HashMap<>();

    @Override
    public Optional<MatchDetailDocument> findById(Long matchId) {
        return Optional.ofNullable(database.get(matchId));
    }

    @Override
    public MatchDetailDocument save(MatchDetailDocument matchDetail) {
        Long id = idGenerator.getAndIncrement();
        database.put(id, matchDetail);
        ReflectionTestUtils.setField(matchDetail, "id", id);
        return database.get(id);
    }

    @Override
    public long updateCurrentCommentary(Long matchId, String content, String commentaryId) {
        List<MatchDetailDocument> matchDetailDocuments = database.values().stream()
                .filter(matchDetail -> matchId.equals(matchDetail.getId()))
                .toList();

        matchDetailDocuments.forEach(matchDetailDocument -> matchDetailDocument.updateCurrentCommentary(content, commentaryId));

        return matchDetailDocuments.size();
    }

    @Override
    public void upsertMatchDetail(MatchDetailDocument matchDetailDocument) {
        Long id = matchDetailDocument.getId();
        MatchDetailDocument saved = database.get(id);

        if (saved == null) {
            save(matchDetailDocument);
            return;
        }

        ReflectionTestUtils.setField(saved, "homeScore", matchDetailDocument.getHomeScore());
        ReflectionTestUtils.setField(saved, "homeStats", matchDetailDocument.getHomeStats());
        ReflectionTestUtils.setField(saved, "awayScore", matchDetailDocument.getAwayScore());
        ReflectionTestUtils.setField(saved, "awayStats", matchDetailDocument.getAwayStats());

        ExtraTime newExtraTime = matchDetailDocument.getExtraTime();
        if (newExtraTime != null) {
            ExtraTime existingExtraTime = saved.getExtraTime();

            if (existingExtraTime == null) {
                // 기존 추가시간 객체가 아예 없었다면 객체 자체를 통째로 할당
                ReflectionTestUtils.setField(saved, "extraTime", newExtraTime);
            } else {
                // 기존 추가시간 객체가 있다면, 하위 필드(전반/후반) 별로 null이 아닐 때만 병합
                if (newExtraTime.getFirstHalf() != null) {
                    ReflectionTestUtils.setField(existingExtraTime, "firstHalf", newExtraTime.getFirstHalf());
                }
                if (newExtraTime.getSecondHalf() != null) {
                    ReflectionTestUtils.setField(existingExtraTime, "secondHalf", newExtraTime.getSecondHalf());
                }
            }
        }
    }

}
