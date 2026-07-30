package com.scorenow.scorenow_api.domain.match.service;

import static com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.mapper.MatchMapper;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class AdminMatchServiceTeamDisplayOrderTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SportRepository sportRepository;

    @Spy
    private MatchMapper matchMapper = new MatchMapper();

    @Spy
    private MatchTeamDisplayOrderPolicy matchTeamDisplayOrderPolicy = new MatchTeamDisplayOrderPolicy();

    @InjectMocks
    private AdminMatchService adminMatchService;

    @Test
    void 수동_경기_생성시_리그의_표시_정책을_Match에_저장한다() {
        MatchCreateRequest request = createRequest();
        League league = League.builder()
                .id(request.getLeagueId())
                .sportId(request.getSportId())
                .eName("Premier League")
                .teamDisplayOrder(AWAY_HOME)
                .build();

        givenValidCreateRequest(request);
        given(leagueRepository.findById(request.getLeagueId())).willReturn(Optional.of(league));
        given(matchRepository.save(any(Match.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(matchRepository.findByIdWithRelations(any())).willReturn(Optional.empty());

        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        then(matchRepository).should().save(matchCaptor.capture());

        assertThat(matchCaptor.getValue().getTeamDisplayOrder()).isEqualTo(AWAY_HOME);
    }

    @Test
    void 수동_경기_생성시_리그의_표시_정책이_없으면_HOME_AWAY를_Match에_저장한다() {
        MatchCreateRequest request = createRequest();
        League league = League.builder()
                .id(request.getLeagueId())
                .sportId(request.getSportId())
                .eName("Premier League")
                .build();

        givenValidCreateRequest(request);
        given(leagueRepository.findById(request.getLeagueId())).willReturn(Optional.of(league));
        given(matchRepository.save(any(Match.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(matchRepository.findByIdWithRelations(any())).willReturn(Optional.empty());

        adminMatchService.createMatch(request);

        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        then(matchRepository).should().save(matchCaptor.capture());

        assertThat(matchCaptor.getValue().getTeamDisplayOrder()).isEqualTo(HOME_AWAY);
    }

    @Test
    void 경기_수정시_요청한_표시_정책으로_Match를_변경한다() {
        Long matchId = 1L;
        Match match = Match.builder()
                .id(matchId)
                .sportId(1L)
                .leagueId(1L)
                .homeId(10L)
                .awayId(20L)
                .teamDisplayOrder(HOME_AWAY)
                .build();

        MatchUpdateRequest request = new MatchUpdateRequest();
        request.setTeamDisplayOrder(AWAY_HOME);

        given(matchRepository.findById(matchId)).willReturn(Optional.of(match));

        adminMatchService.updateMatch(matchId, request);

        assertThat(match.getTeamDisplayOrder()).isEqualTo(AWAY_HOME);
    }

    private MatchCreateRequest createRequest() {
        MatchCreateRequest request = new MatchCreateRequest();
        request.setSportId(1L);
        request.setLeagueId(2L);
        request.setHomeId(10L);
        request.setAwayId(20L);
        request.setStartAt(LocalDateTime.of(2026, 1, 1, 19, 0));
        return request;
    }

    private void givenValidCreateRequest(MatchCreateRequest request) {
        given(leagueRepository.existsById(request.getLeagueId())).willReturn(true);
        given(teamRepository.existsByIdAndIsActiveTrue(request.getHomeId())).willReturn(true);
        given(teamRepository.existsByIdAndIsActiveTrue(request.getAwayId())).willReturn(true);
        given(sportRepository.existsById(request.getSportId())).willReturn(true);
    }
}
