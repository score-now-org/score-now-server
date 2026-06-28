package com.scorenow.scorenow_api.domain.player.batch;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.player.batch.dto.TeamPlayerSyncData;
import com.scorenow.scorenow_api.domain.player.entity.Player;
import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.player.repository.PlayerExternalMappingRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerRepository;
import com.scorenow.scorenow_api.domain.player.repository.PlayerTeamDetailRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsSquadResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class TeamPlayerSyncWriterTest {

	@Mock
	private PlayerRepository playerRepository;

	@Mock
	private PlayerExternalMappingRepository playerExternalMappingRepository;

	@Mock
	private PlayerTeamDetailRepository playerTeamDetailRepository;

	@Mock
	private TeamExternalMappingRepository teamExternalMappingRepository;

	@Mock
	private TeamRepository teamRepository;

	@Mock
	private LeagueRepository leagueRepository;

	@InjectMocks
	private TeamPlayerSyncWriter writer;

	@Captor
	private ArgumentCaptor<Player> playerCaptor;

	@Captor
	private ArgumentCaptor<PlayerExternalMapping> mappingCaptor;

	@Captor
	private ArgumentCaptor<PlayerTeamDetail> detailCaptor;

	@Test
	void PlayerExternalMapping이_있으면_Player를_수정하고_PlayerTeamDetail을_생성한다() {
		Long leagueId = 20L;
		String leagueApiId = "league-api";
		String teamApiId = "team-1";
		String playerApiId = "player-1";

		Team team = Team.builder().id(10L).build();
		League league = League.builder().id(leagueId).build();

		Player existingPlayer = Player.builder()
			.id(30L)
			.eName("Old Name")
			.cc("US")
			.birthdate(LocalDate.of(1990, 1, 1))
			.height(180)
			.build();

		TeamExternalMapping teamMapping = TeamExternalMapping.of(DataOrigin.BETS, teamApiId, team.getId());
		PlayerExternalMapping playerMapping = new PlayerExternalMapping(DataOrigin.BETS, playerApiId, existingPlayer);

		BetsSquadResponse.SquadPlayer squadPlayer = squadPlayer(
			playerApiId,
			"New Name",
			"GB",
			LocalDate.of(1995, 5, 20),
			"190",
			"FW",
			"9"
		);

		given(teamExternalMappingRepository.findByProviderAndApiTeamId(DataOrigin.BETS, teamApiId))
			.willReturn(Optional.of(teamMapping));
		given(teamRepository.findById(team.getId())).willReturn(Optional.of(team));
		given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));
		given(playerExternalMappingRepository.findByProviderAndApiPlayerId(DataOrigin.BETS, playerApiId))
			.willReturn(Optional.of(playerMapping));
		given(playerTeamDetailRepository.findByPlayerIdAndTeamIdAndLeagueId(
			existingPlayer.getId(),
			team.getId(),
			league.getId()
		)).willReturn(Optional.empty());

		TeamPlayerSyncData data = new TeamPlayerSyncData(
			leagueId,
			leagueApiId,
			teamApiId,
			List.of(squadPlayer)
		);

		writer.write(new Chunk<>(List.of(data)));

		assertThat(existingPlayer.getEName()).isEqualTo("New Name");
		assertThat(existingPlayer.getCc()).isEqualTo("GB");
		assertThat(existingPlayer.getBirthdate()).isEqualTo(LocalDate.of(1995, 5, 20));
		assertThat(existingPlayer.getHeight()).isEqualTo(190);

		then(playerRepository).should(never()).save(any());
		then(playerExternalMappingRepository).should(never()).save(any());
		then(playerTeamDetailRepository).should().save(detailCaptor.capture());

		PlayerTeamDetail savedDetail = detailCaptor.getValue();
		assertThat(savedDetail.getPlayer()).isSameAs(existingPlayer);
		assertThat(savedDetail.getTeam()).isSameAs(team);
		assertThat(savedDetail.getLeague()).isSameAs(league);
		assertThat(savedDetail.getPosition()).isEqualTo("FW");
		assertThat(savedDetail.getShirtNumber()).isEqualTo("9");
	}

	@Test
	void PlayerExternalMapping이_없으면_Player와_PlayerExternalMapping과_PlayerTeamDetail을_생성한다() {
		Long leagueId = 20L;
		String leagueApiId = "league-api";
		String teamApiId = "team-1";
		String playerApiId = "player-2";

		Team team = Team.builder().id(10L).build();
		League league = League.builder().id(leagueId).build();
		Player savedPlayer = Player.builder().id(31L).build();

		BetsSquadResponse.SquadPlayer squadPlayer = squadPlayer(
			playerApiId,
			"New Player",
			"KR",
			LocalDate.of(2000, 3, 15),
			"185",
			"MF",
			"8"
		);

		given(teamExternalMappingRepository.findByProviderAndApiTeamId(DataOrigin.BETS, teamApiId))
			.willReturn(Optional.of(TeamExternalMapping.of(DataOrigin.BETS, teamApiId, team.getId())));
		given(teamRepository.findById(team.getId())).willReturn(Optional.of(team));
		given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));
		given(playerExternalMappingRepository.findByProviderAndApiPlayerId(DataOrigin.BETS, playerApiId))
			.willReturn(Optional.empty());
		given(playerRepository.save(playerCaptor.capture())).willReturn(savedPlayer);
		given(playerExternalMappingRepository.save(mappingCaptor.capture()))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(playerTeamDetailRepository.findByPlayerIdAndTeamIdAndLeagueId(
			savedPlayer.getId(),
			team.getId(),
			league.getId()
		)).willReturn(Optional.empty());

		TeamPlayerSyncData data = new TeamPlayerSyncData(
			leagueId,
			leagueApiId,
			teamApiId,
			List.of(squadPlayer)
		);

		writer.write(new Chunk<>(List.of(data)));

		Player playerToSave = playerCaptor.getValue();
		assertThat(playerToSave.getEName()).isEqualTo("New Player");
		assertThat(playerToSave.getCc()).isEqualTo("KR");
		assertThat(playerToSave.getBirthdate()).isEqualTo(LocalDate.of(2000, 3, 15));
		assertThat(playerToSave.getHeight()).isEqualTo(185);

		PlayerExternalMapping savedMapping = mappingCaptor.getValue();
		assertThat(savedMapping.getProvider()).isEqualTo(DataOrigin.BETS);
		assertThat(savedMapping.getApiPlayerId()).isEqualTo(playerApiId);
		assertThat(savedMapping.getPlayer()).isSameAs(savedPlayer);

		then(playerTeamDetailRepository).should().save(detailCaptor.capture());

		PlayerTeamDetail savedDetail = detailCaptor.getValue();
		assertThat(savedDetail.getPlayer()).isSameAs(savedPlayer);
		assertThat(savedDetail.getTeam()).isSameAs(team);
		assertThat(savedDetail.getLeague()).isSameAs(league);
		assertThat(savedDetail.getPosition()).isEqualTo("MF");
		assertThat(savedDetail.getShirtNumber()).isEqualTo("8");

	}

	@Test
	void PlayerExternalMapping은_있지만_Player가_없으면_Player를_생성하고_기존_PlayerExternalMapping에_연결한다() {
		Long leagueId = 20L;
		String leagueApiId = "league-api";
		String teamApiId = "team-1";
		String playerApiId = "player-3";

		Team team = Team.builder().id(10L).build();
		League league = League.builder().id(leagueId).build();
		Player savedPlayer = Player.builder().id(32L).build();

		PlayerExternalMapping brokenMapping = new PlayerExternalMapping(DataOrigin.BETS, playerApiId, null);

		BetsSquadResponse.SquadPlayer squadPlayer = squadPlayer(
			playerApiId,
			"Recovered Player",
			"JP",
			LocalDate.of(1999, 7, 10),
			"178",
			"DF",
			"4"
		);

		given(teamExternalMappingRepository.findByProviderAndApiTeamId(DataOrigin.BETS, teamApiId))
			.willReturn(Optional.of(TeamExternalMapping.of(DataOrigin.BETS, teamApiId, team.getId())));
		given(teamRepository.findById(team.getId())).willReturn(Optional.of(team));
		given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));
		given(playerExternalMappingRepository.findByProviderAndApiPlayerId(DataOrigin.BETS, playerApiId))
			.willReturn(Optional.of(brokenMapping));
		given(playerRepository.save(playerCaptor.capture())).willReturn(savedPlayer);
		given(playerExternalMappingRepository.save(mappingCaptor.capture()))
			.willAnswer(invocation -> invocation.getArgument(0));
		given(playerTeamDetailRepository.findByPlayerIdAndTeamIdAndLeagueId(
			savedPlayer.getId(),
			team.getId(),
			league.getId()
		)).willReturn(Optional.empty());

		TeamPlayerSyncData data = new TeamPlayerSyncData(
			leagueId,
			leagueApiId,
			teamApiId,
			List.of(squadPlayer)
		);

		writer.write(new Chunk<>(List.of(data)));

		Player playerToSave = playerCaptor.getValue();
		assertThat(playerToSave.getEName()).isEqualTo("Recovered Player");
		assertThat(playerToSave.getCc()).isEqualTo("JP");
		assertThat(playerToSave.getBirthdate()).isEqualTo(LocalDate.of(1999, 7, 10));
		assertThat(playerToSave.getHeight()).isEqualTo(178);

		PlayerExternalMapping updatedMapping = mappingCaptor.getValue();
		assertThat(updatedMapping).isSameAs(brokenMapping);
		assertThat(updatedMapping.getPlayer()).isSameAs(savedPlayer);

		then(playerTeamDetailRepository).should().save(detailCaptor.capture());

		PlayerTeamDetail savedDetail = detailCaptor.getValue();
		assertThat(savedDetail.getPlayer()).isSameAs(savedPlayer);
		assertThat(savedDetail.getTeam()).isSameAs(team);
		assertThat(savedDetail.getLeague()).isSameAs(league);
		assertThat(savedDetail.getPosition()).isEqualTo("DF");
		assertThat(savedDetail.getShirtNumber()).isEqualTo("4");

	}

	@Test
	void PlayerTeamDetail이_이미_있으면_position과_shirtNumber를_수정한다() {

		Long leagueId = 20L;
		String leagueApiId = "league-api";
		String teamApiId = "team-1";
		String playerApiId = "player-4";

		Team team = Team.builder().id(10L).build();
		League league = League.builder().id(leagueId).build();
		Player existingPlayer = Player.builder().id(30L).build();

		PlayerTeamDetail existingDetail = PlayerTeamDetail.builder()
			.player(existingPlayer)
			.team(team)
			.league(league)
			.position("OLD")
			.shirtNumber("99")
			.build();

		PlayerExternalMapping playerMapping = new PlayerExternalMapping(DataOrigin.BETS, playerApiId, existingPlayer);

		BetsSquadResponse.SquadPlayer squadPlayer = squadPlayer(
			playerApiId,
			"Updated Player",
			"FR",
			LocalDate.of(1998, 8, 8),
			"182",
			"GK",
			"1"
		);

		given(teamExternalMappingRepository.findByProviderAndApiTeamId(DataOrigin.BETS, teamApiId))
			.willReturn(Optional.of(TeamExternalMapping.of(DataOrigin.BETS, teamApiId, team.getId())));
		given(teamRepository.findById(team.getId())).willReturn(Optional.of(team));
		given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));
		given(playerExternalMappingRepository.findByProviderAndApiPlayerId(DataOrigin.BETS, playerApiId))
			.willReturn(Optional.of(playerMapping));
		given(playerTeamDetailRepository.findByPlayerIdAndTeamIdAndLeagueId(
			existingPlayer.getId(),
			team.getId(),
			league.getId()
		)).willReturn(Optional.of(existingDetail));

		TeamPlayerSyncData data = new TeamPlayerSyncData(
			leagueId,
			leagueApiId,
			teamApiId,
			List.of(squadPlayer)
		);

		writer.write(new Chunk<>(List.of(data)));

		then(playerTeamDetailRepository).should().save(detailCaptor.capture());

		PlayerTeamDetail savedDetail = detailCaptor.getValue();
		assertThat(savedDetail).isSameAs(existingDetail);
		assertThat(savedDetail.getPosition()).isEqualTo("GK");
		assertThat(savedDetail.getShirtNumber()).isEqualTo("1");

	}

	@Test
	void SquadPlayer의_id가_없으면_선수_저장을_건너뛴다() {

		Long leagueId = 20L;
		String teamApiId = "team-1";

		Team team = Team.builder().id(10L).build();
		League league = League.builder().id(leagueId).build();

		given(teamExternalMappingRepository.findByProviderAndApiTeamId(DataOrigin.BETS, teamApiId))
			.willReturn(Optional.of(TeamExternalMapping.of(DataOrigin.BETS, teamApiId, team.getId())));
		given(teamRepository.findById(team.getId())).willReturn(Optional.of(team));
		given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));

		TeamPlayerSyncData data = new TeamPlayerSyncData(
			leagueId,
			"league-api",
			teamApiId,
			List.of(squadPlayer(null, "Ignored", "KR", null, null, null, null))
		);

		writer.write(new Chunk<>(List.of(data)));

		then(playerRepository).should(never()).save(any());
		then(playerExternalMappingRepository).should(never()).save(any());
		then(playerTeamDetailRepository).should(never()).save(any());

	}

	@Test
	void players가_비어있으면_저장하지_않고_savedCount는_0이다() {

		Long leagueId = 20L;
		String teamApiId = "team-1";

		Team team = Team.builder().id(10L).build();
		League league = League.builder().id(leagueId).build();

		given(teamExternalMappingRepository.findByProviderAndApiTeamId(DataOrigin.BETS, teamApiId))
			.willReturn(Optional.of(TeamExternalMapping.of(DataOrigin.BETS, teamApiId, team.getId())));
		given(teamRepository.findById(team.getId())).willReturn(Optional.of(team));
		given(leagueRepository.findById(leagueId)).willReturn(Optional.of(league));

		TeamPlayerSyncData data = new TeamPlayerSyncData(
			leagueId,
			"league-api",
			teamApiId,
			List.of()
		);

		writer.write(new Chunk<>(List.of(data)));

		then(playerRepository).should(never()).save(any());
		then(playerExternalMappingRepository).should(never()).save(any());
		then(playerTeamDetailRepository).should(never()).save(any());

	}

	@Test
	void TeamExternalMapping이_없으면_BusinessException이_발생한다() {

		TeamPlayerSyncData data = new TeamPlayerSyncData(
			1L,
			"league-api",
			"team-1",
			List.of()
		);

		given(teamExternalMappingRepository.findByProviderAndApiTeamId(DataOrigin.BETS, "team-1"))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> writer.write(new Chunk<>(List.of(data))))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("teamApiId에 해당하는 팀을 찾을 수 없습니다");

	}

	@Test
	void internalTeamId에_해당하는_Team이_없으면_BusinessException이_발생한다() {
		Long internalTeamId = 10L;
		String teamApiId = "team-1";

		TeamPlayerSyncData data = new TeamPlayerSyncData(
			1L,
			"league-api",
			teamApiId,
			List.of()
		);

		given(teamExternalMappingRepository.findByProviderAndApiTeamId(DataOrigin.BETS, teamApiId))
			.willReturn(Optional.of(TeamExternalMapping.of(DataOrigin.BETS, teamApiId, internalTeamId)));
		given(teamRepository.findById(internalTeamId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> writer.write(new Chunk<>(List.of(data))))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("internalTeamId에 해당하는 팀을 찾을 수 없습니다");
	}

	@Test
	void leagueId에_해당하는_League가_없으면_BusinessException이_발생한다() {
		Long leagueId = 20L;
		String teamApiId = "team-1";
		Team team = Team.builder().id(10L).build();

		TeamPlayerSyncData data = new TeamPlayerSyncData(
			leagueId,
			"league-api",
			teamApiId,
			List.of()
		);

		given(teamExternalMappingRepository.findByProviderAndApiTeamId(DataOrigin.BETS, teamApiId))
			.willReturn(Optional.of(TeamExternalMapping.of(DataOrigin.BETS, teamApiId, team.getId())));
		given(teamRepository.findById(team.getId())).willReturn(Optional.of(team));
		given(leagueRepository.findById(leagueId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> writer.write(new Chunk<>(List.of(data))))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("leagueId에 해당하는 리그를 찾을 수 없습니다");
	}

	private BetsSquadResponse.SquadPlayer squadPlayer(
		String id,
		String name,
		String cc,
		LocalDate birthdate,
		String height,
		String position,
		String shirtNumber
	) {
		BetsSquadResponse.SquadPlayer player = new BetsSquadResponse.SquadPlayer();
		player.setId(id);
		player.setName(name);
		player.setCc(cc);
		player.setBirthdate(birthdate);
		player.setHeight(height);
		player.setPosition(position);
		player.setShirtnumber(shirtNumber);
		return player;
	}

}
