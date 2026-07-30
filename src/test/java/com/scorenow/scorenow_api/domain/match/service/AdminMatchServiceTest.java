package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.mapper.MatchMapper;
import com.scorenow.scorenow_api.domain.match.realtime.MatchRealtimeEventPublisher;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminMatchServiceTest {

    @InjectMocks
    private AdminMatchService adminMatchService;

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private SportRepository sportRepository;
    @Mock
    private AdminFeaturedMatchService adminFeaturedMatchService;
    @Mock
    private MatchMapper matchMapper;
    @Mock
    private MatchTeamDisplayOrderPolicy matchTeamDisplayOrderPolicy;
    @Mock
    private MatchRealtimeEventPublisher eventPublisher;

    @Test
    void 경기_시작_일자가_변경되면_상단고정_핫매치_설정을_해제한다() {
        Long matchId = 1L;
        Match match = Match.builder()
                .id(matchId)
                .startAt(LocalDateTime.of(2026, 7, 23, 19, 0))
                .build();

        MatchUpdateRequest request = new MatchUpdateRequest();
        request.setStartAt(LocalDateTime.of(2026, 7, 24, 19, 0));

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));

        adminMatchService.updateMatch(matchId, request);

        then(adminFeaturedMatchService).should().deleteFeaturedMatchByMatchId(matchId);
        assertThat(match.getStartAt()).isEqualTo(LocalDateTime.of(2026, 7, 24, 19, 0));
    }

    @Test
    void 경기_시작_시간만_변경되면_상단고정_핫매치_설정을_해제하지_않는다() {
        Long matchId = 1L;
        Match match = Match.builder()
                .id(matchId)
                .startAt(LocalDateTime.of(2026, 7, 23, 19, 0))
                .build();

        MatchUpdateRequest request = new MatchUpdateRequest();
        request.setStartAt(LocalDateTime.of(2026, 7, 23, 21, 0));

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));

        adminMatchService.updateMatch(matchId, request);

        then(adminFeaturedMatchService).should(never()).deleteFeaturedMatchByMatchId(matchId);
        assertThat(match.getStartAt()).isEqualTo(LocalDateTime.of(2026, 7, 23, 21, 0));
    }

    @Test
    void 수동_경기_등록시_홈팀ID_와_어웨이팀ID가_동일한_경우_예외가_발생한다() {
        MatchCreateRequest request = new MatchCreateRequest();
        request.setHomeId(1L);
        request.setAwayId(1L);

        assertThatThrownBy(() -> adminMatchService.createMatch(request))
                .isInstanceOf(BusinessException.class);
    }
}
