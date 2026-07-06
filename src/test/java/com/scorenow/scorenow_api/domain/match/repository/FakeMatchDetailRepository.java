package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument.AdditionalTime;
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
    public List<MatchDetailDocument> findAllById(List<Long> matchIds) {
        return matchIds.stream()
                .map(database::get)
                .filter(matchDetailDocument -> matchDetailDocument != null)
                .toList();
    }

    @Override
    public MatchDetailDocument save(MatchDetailDocument matchDetail) {
        Long id = idGenerator.getAndIncrement();
        database.put(id, matchDetail);
        ReflectionTestUtils.setField(matchDetail, "id", id);
        return database.get(id);
    }

    @Override
    public void upsertCurrentCommentary(Long matchId, String content, String commentaryId) {
        MatchDetailDocument saved = database.get(matchId);

        if (saved == null) {
            MatchDetailDocument matchDetailDocument = MatchDetailDocument.builder()
                    .id(matchId)
                    .currentCommentary(content)
                    .currentCommentaryId(commentaryId)
                    .build();

            database.put(matchId, matchDetailDocument);
            return;
        }

        saved.updateCurrentCommentary(content, commentaryId);
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

        AdditionalTime newAdditionalTime = matchDetailDocument.getAdditionalTime();
        if (newAdditionalTime != null) {
            AdditionalTime existingAdditionalTime = saved.getAdditionalTime();

            if (existingAdditionalTime == null) {
                // 기존 추가시간 객체가 아예 없었다면 객체 자체를 통째로 할당
                ReflectionTestUtils.setField(saved, "additionalTime", newAdditionalTime);
            } else {
                // 기존 추가시간 객체가 있다면, 하위 필드(전반/후반) 별로 null이 아닐 때만 병합
                if (newAdditionalTime.getFirstHalf() != null) {
                    ReflectionTestUtils.setField(existingAdditionalTime, "firstHalf", newAdditionalTime.getFirstHalf());
                }
                if (newAdditionalTime.getSecondHalf() != null) {
                    ReflectionTestUtils.setField(existingAdditionalTime, "secondHalf", newAdditionalTime.getSecondHalf());
                }
                if (newAdditionalTime.getExtraFirstHalf() != null) {
                    ReflectionTestUtils.setField(existingAdditionalTime, "extraFirstHalf", newAdditionalTime.getExtraFirstHalf());
                }
                if (newAdditionalTime.getExtraSecondHalf() != null) {
                    ReflectionTestUtils.setField(existingAdditionalTime, "extraSecondHalf", newAdditionalTime.getExtraSecondHalf());
                }
            }
        }
    }

    @Override
    public void upsertMatchClock(Long matchId, MatchDetailDocument.MatchClock matchClock) {
        MatchDetailDocument saved = database.get(matchId);

        if (saved == null) {
            MatchDetailDocument matchDetailDocument = MatchDetailDocument.builder()
                    .id(matchId)
                    .matchClock(matchClock)
                    .build();

            database.put(matchId, matchDetailDocument);
            return;
        }

        ReflectionTestUtils.setField(saved, "matchClock", matchClock);
    }

}
