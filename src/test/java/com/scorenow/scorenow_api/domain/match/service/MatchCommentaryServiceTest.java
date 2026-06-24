package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.FakeFileStorage;
import com.scorenow.scorenow_api.domain.match.repository.FakeMatchCommentaryRepository;
import com.scorenow.scorenow_api.domain.match.repository.FakeMatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MatchCommentaryServiceTest {
    private MatchCommentaryService matchCommentaryService;
    private FakeMatchCommentaryRepository matchCommentaryRepository;
    private FakeMatchDetailRepository matchDetailRepository;
    private FakeFileStorage fileStorage;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchRealtimeEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        matchCommentaryRepository = new FakeMatchCommentaryRepository();
        matchDetailRepository = new FakeMatchDetailRepository();
        fileStorage = new FakeFileStorage();
        matchCommentaryService = new MatchCommentaryService(matchCommentaryRepository, matchDetailRepository, matchRepository, fileStorage, eventPublisher);
    }

    @Test
    void 중계_정보에는_가장_최신의_중계_멘트만_조회된다() {
        MatchDetailDocument matchDetailDocument = createMatchDetailDocument();
        Long matchId = matchDetailDocument.getId();
        given(matchRepository.existsById(matchId)).willReturn(true);

        boolean recordEnabled = true;
        matchCommentaryService.saveCommentary(matchId, "18", "손흥민 오늘 최고네요.", recordEnabled, null);

        recordEnabled = false;
        String recentSavedCommentaryId = matchCommentaryService.saveCommentary(matchId, "52", "이니에스타 오늘 좋네요.", recordEnabled, null);

        assertThat(matchDetailDocument.getCurrentCommentaryId()).isEqualTo(recentSavedCommentaryId);
    }

    @Test
    void 중계_멘트_저장_ON_상태에서만_등록된_중계_멘트만_조회된다() {
        MatchDetailDocument matchDetailDocument = createMatchDetailDocument();
        Long matchId = matchDetailDocument.getId();
        given(matchRepository.existsById(matchId)).willReturn(true);

        // 중계 멘트 저장 ON (3건)
        boolean recordEnabled = true;
        matchCommentaryService.saveCommentary(matchId, "3", "메시 오늘 좋네요.", recordEnabled, null);
        matchCommentaryService.saveCommentary(matchId, "15", "호날두 오늘 별로네요.", recordEnabled, null);
        matchCommentaryService.saveCommentary(matchId, "18", "손흥민 오늘 최고네요.", recordEnabled, null);

        // 중계 멘트 저장 OFF (2건)
        recordEnabled = false;
        matchCommentaryService.saveCommentary(matchId, "38", "모드리치 오늘 좋네요.", recordEnabled, null);
        matchCommentaryService.saveCommentary(matchId, "52", "이니에스타 오늘 좋네요.", recordEnabled, null);

        List<String> commentaries = matchCommentaryService.getCommentariesByMatchId(matchId);

        assertThat(commentaries.size()).isEqualTo(3);
    }

    @Test
    void 존재하지_않는_경기에_중계멘트_저장시_예외가_발생한다() {
        Long invalidMatchId = 999L;
        given(matchRepository.existsById(invalidMatchId)).willReturn(false);

        assertThatRuntimeException()
                .isThrownBy(() -> matchCommentaryService.saveCommentary(invalidMatchId, "3", "메시 오늘 좋네요.", true, null));
    }

    @Test
    void 중계_멘트를_저장하는데_MatchDetailDocument가_없으면_현재_중계_멘트를_가진_새로운_문서를_생성한다() {
        Long matchId = 10L;
        String elapsedMinutes = "3";
        String content = "중계 멘트 입니다.";
        given(matchRepository.existsById(matchId)).willReturn(true);

        String commentaryId = matchCommentaryService.saveCommentary(matchId, elapsedMinutes, content, true, null);

        MatchDetailDocument matchDetailDocument = matchDetailRepository.findById(matchId).get();
        assertThat(matchDetailDocument.getCurrentCommentary()).isEqualTo(content);
        assertThat(matchDetailDocument.getCurrentCommentaryId()).isEqualTo(commentaryId);
    }

    private MatchDetailDocument createMatchDetailDocument() {
        return matchDetailRepository.save(MatchDetailDocument.builder().build());
    }

}
