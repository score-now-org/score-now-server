package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.request.FootballMatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer.SportDetailNormalizerRegistry;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.processor.SportDetailEventProcessorRegistry;
import com.scorenow.scorenow_api.domain.match.service.statusdisplay.MatchStatusDisplayResolver;
import com.scorenow.scorenow_api.domain.stadium.service.StadiumCacheService;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MatchDetailServiceTest {

    @InjectMocks
    private MatchDetailService matchDetailService;

    @Mock
    private StadiumCacheService stadiumCacheService;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchDetailRepository matchDetailRepository;
    @Mock
    private MatchRealtimeEventPublisher eventPublisher;
    @Mock
    private SportDetailNormalizerRegistry normalizerRegistry;
    @Mock
    private SportDetailEventProcessorRegistry eventProcessorRegistry;
    @Mock
    private MatchStatusDisplayResolver matchStatusDisplayResolver;

    @Test
    void 진행중_경기_상세_동기화시_MatchDetailDocument가_없으면_예외가_발생한다() {
        String apiMatchId = "api-1000";
        Match match = Match.builder()
                .id(10L)
                .apiMatchId(apiMatchId)
                .sportId(1L)
                .statusCode(MatchStatus.IN_PLAY)
                .build();
        BetsViewResponse.ViewResult viewResult = new BetsViewResponse.ViewResult();
        viewResult.setId(apiMatchId);

        given(matchRepository.findByExternalInfo(DataOrigin.BETS, apiMatchId)).willReturn(Optional.of(match));
        given(matchDetailRepository.findById(match.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> matchDetailService.updateInplayMatchDetail(DataOrigin.BETS, viewResult))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MATCH_DETAIL_NOT_FOUND.getMessage());
    }

    @Test
    void 수동_경기_상세_수정시_MatchDetailDocument가_없으면_예외가_발생한다() {
        Long matchId = 10L;
        Match match = Match.builder()
                .id(matchId)
                .sportId(1L)
                .statusCode(MatchStatus.NOT_STARTED)
                .build();

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));
        given(matchDetailRepository.findById(matchId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> matchDetailService.updateMatchDetailManual(matchId, new FootballMatchDetailUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MATCH_DETAIL_NOT_FOUND.getMessage());
    }
}
