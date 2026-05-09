package com.scorenow.scorenow_api.domain.match.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.mapper.MatchLineupMapper;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLineupResponse;
import com.scorenow.scorenow_api.external.common.ApiProvider;

@ExtendWith(MockitoExtension.class)
class MatchLineupSyncServiceTest {

	@Mock
	private MatchLineupMapper lineupMapper;

	@Mock
	private BetsApiClient betsApiClient;

	@Mock
	private MatchLineupRepository matchLineupRepo;

	@Mock
	private MatchRepository matchRepo;

	@Mock
	private TeamRepository teamRepo;

	@Mock
	private TeamExternalMappingRepository teamExternalMappingRepository;

	@InjectMocks
	private MatchLineupSyncService matchLineupSyncService;

	@Test
	void 라인업_문서를_자동_생성하고_저장한다() {
		// given
		Long matchId = 10119802L;
		Long sportId = 1L;
		Long homeId = 17170L;
		Long awayId = 23451L;

		Match match = Match.builder()
			.id(matchId)
			.sportId(sportId)
			.apiMatchId("10119802")
			.homeId(homeId)
			.awayId(awayId)
			.build();

		Team homeTeam = Team.builder()
			.id(homeId)
			.eName("Fulham")
			.build();

		Team awayTeam = Team.builder()
			.id(awayId)
			.eName("Liverpool")
			.build();

		TeamExternalMapping homeMapping = TeamExternalMapping.builder()
			.provider(ApiProvider.BETS)
			.apiTeamId("17170")
			.internalTeamId(homeId)
			.build();

		TeamExternalMapping awayMapping = TeamExternalMapping.builder()
			.provider(ApiProvider.BETS)
			.apiTeamId("23451")
			.internalTeamId(awayId)
			.build();

		BetsLineupResponse homeResponse = new BetsLineupResponse();
		homeResponse.setSuccess(1);
		homeResponse.setResults(new BetsLineupResponse.Result());

		BetsLineupResponse awayResponse = new BetsLineupResponse();
		awayResponse.setSuccess(1);
		awayResponse.setResults(new BetsLineupResponse.Result());

		LineupSide homeSide = LineupSide.builder()
			.teamId(homeId)
			.apiTeamId("17170")
			.teamEname("Fulham")
			.build();

		LineupSide awaySide = LineupSide.builder()
			.teamId(awayId)
			.apiTeamId("23451")
			.teamEname("Liverpool")
			.build();

		when(matchRepo.findById(matchId)).thenReturn(Optional.of(match));
		when(teamRepo.findAllById(List.of(homeId, awayId))).thenReturn(List.of(homeTeam, awayTeam));

		when(teamExternalMappingRepository.findByProviderAndInternalTeamId(ApiProvider.BETS, homeId)) // 메서드명 수정
			.thenReturn(Optional.of(homeMapping));
		when(teamExternalMappingRepository.findByProviderAndInternalTeamId(ApiProvider.BETS, awayId))
			.thenReturn(Optional.of(awayMapping));

		when(betsApiClient.getLineup("17170")).thenReturn(homeResponse);
		when(betsApiClient.getLineup("23451")).thenReturn(awayResponse);

		when(matchLineupRepo.findById(matchId)).thenReturn(Optional.empty());

		when(lineupMapper.toSide(any(), eq(homeId), eq("17170"), eq("Fulham"), anyMap()))
			.thenReturn(homeSide);

		when(lineupMapper.toSide(any(), eq(awayId), eq("23451"), eq("Liverpool"), anyMap()))
			.thenReturn(awaySide);

		when(matchLineupRepo.save(any(MatchLineupDocument.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		MatchLineupDocument result = matchLineupSyncService.syncMatchLineup("10119802", "1");

		// then
		ArgumentCaptor<MatchLineupDocument> captor =
			ArgumentCaptor.forClass(MatchLineupDocument.class);

		verify(matchLineupRepo).save(captor.capture());

		MatchLineupDocument saved = captor.getValue();

		assertThat(result).isSameAs(saved);
		assertThat(saved.getId()).isEqualTo(matchId);
		assertThat(saved.getSportId()).isEqualTo(sportId);
		assertThat(saved.getApiMatchId()).isEqualTo("10119802");

		assertThat(saved.getHome()).isNotNull();
		assertThat(saved.getHome().getTeamId()).isEqualTo(homeId);
		assertThat(saved.getHome().getTeamEname()).isEqualTo("Fulham");

		assertThat(saved.getAway()).isNotNull();
		assertThat(saved.getAway().getTeamId()).isEqualTo(awayId);
		assertThat(saved.getAway().getTeamEname()).isEqualTo("Liverpool");
	}
}