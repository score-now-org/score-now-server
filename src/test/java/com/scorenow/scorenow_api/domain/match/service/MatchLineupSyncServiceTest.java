package com.scorenow.scorenow_api.domain.match.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.same;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.mapper.MatchLineupMapper;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsLineupResponse;

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

	@Mock
	private PlayerExternalMappingRepository playerExternalMappingRepository;

	@InjectMocks
	private MatchLineupSyncService matchLineupSyncService;

	@Test
	void 라인업_문서를_자동_생성하고_저장한다() {
		// given
		Long matchId = 1L;
		Long sportId = 1L;
		String apiMatchId = "10119802";

		Long homeId = 17170L;
		Long awayId = 23451L;

		Match match = Match.builder()
			.id(matchId)
			.sportId(sportId)
			.apiMatchId(apiMatchId)
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
			.provider(DataOrigin.BETS)
			.apiTeamId("17170")
			.internalTeamId(homeId)
			.build();

		TeamExternalMapping awayMapping = TeamExternalMapping.builder()
			.provider(DataOrigin.BETS)
			.apiTeamId("23451")
			.internalTeamId(awayId)
			.build();

		BetsLineupResponse.LineupSide homeApiSide =
			new BetsLineupResponse.LineupSide();

		BetsLineupResponse.LineupSide awayApiSide =
			new BetsLineupResponse.LineupSide();

		BetsLineupResponse.Result apiResult =
			new BetsLineupResponse.Result();

		apiResult.setHome(homeApiSide);
		apiResult.setAway(awayApiSide);

		BetsLineupResponse lineupResponse = new BetsLineupResponse();
		lineupResponse.setSuccess(1);
		lineupResponse.setResults(apiResult);

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

		when(matchRepo.findById(matchId))
			.thenReturn(Optional.of(match));

		when(teamRepo.findAllById(List.of(homeId, awayId)))
			.thenReturn(List.of(homeTeam, awayTeam));

		when(teamExternalMappingRepository.findByProviderAndInternalTeamId(
			DataOrigin.BETS,
			homeId
		)).thenReturn(Optional.of(homeMapping));

		when(teamExternalMappingRepository.findByProviderAndInternalTeamId(
			DataOrigin.BETS,
			awayId
		)).thenReturn(Optional.of(awayMapping));

		when(betsApiClient.getLineup(apiMatchId))
			.thenReturn(lineupResponse);

		when(matchLineupRepo.findById(matchId))
			.thenReturn(Optional.empty());

		when(lineupMapper.toSide(
			same(homeApiSide),
			eq(homeId),
			eq("17170"),
			eq("Fulham"),
			anyMap()
		)).thenReturn(homeSide);

		when(lineupMapper.toSide(
			same(awayApiSide),
			eq(awayId),
			eq("23451"),
			eq("Liverpool"),
			anyMap()
		)).thenReturn(awaySide);

		when(matchLineupRepo.save(org.mockito.ArgumentMatchers.any(
			MatchLineupDocument.class
		))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		MatchLineupDocument result =
			matchLineupSyncService.syncMatchLineup(
				String.valueOf(matchId),
				String.valueOf(sportId)
			);

		// then
		ArgumentCaptor<MatchLineupDocument> captor =
			ArgumentCaptor.forClass(MatchLineupDocument.class);

		verify(betsApiClient).getLineup(apiMatchId);
		verify(matchLineupRepo).save(captor.capture());

		MatchLineupDocument saved = captor.getValue();

		assertThat(result).isSameAs(saved);
		assertThat(saved.getId()).isEqualTo(matchId);
		assertThat(saved.getSportId()).isEqualTo(sportId);
		assertThat(saved.getApiMatchId()).isEqualTo(apiMatchId);

		assertThat(saved.getHome()).isSameAs(homeSide);
		assertThat(saved.getHome().getTeamId()).isEqualTo(homeId);
		assertThat(saved.getHome().getTeamEname()).isEqualTo("Fulham");

		assertThat(saved.getAway()).isSameAs(awaySide);
		assertThat(saved.getAway().getTeamId()).isEqualTo(awayId);
		assertThat(saved.getAway().getTeamEname()).isEqualTo("Liverpool");
	}
}