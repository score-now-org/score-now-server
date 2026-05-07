package com.scorenow.scorenow_api.domain.team.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.sport.repository.SportRepository;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.external.common.ApiProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamService {
	private final TeamRepository teamRepository;
	private final TeamExternalMappingRepository teamExternalMappingRepository;
	private final SportRepository sportRepository;

	/**
	 * 팀 생성
	 */
	@Transactional
	public Team getOrCreateTeam(final ApiProvider provider, final Long internalSportId, final String externalTeamId,
		final String name, final String cc, final String imageUrl) {
		// 1. 전달받은 외부 정보를 기반으로 Team External Mapping 테이블에 데이터가 있는지 확인
		Optional<TeamExternalMapping> teamMappingInfo = teamExternalMappingRepository.findByExternalInfo(provider,
			externalTeamId);

		// 2. 매핑 정보에서 internal team id 를 추출하고, 이를 기반으로 Team 엔티티 반환
		if (teamMappingInfo.isPresent()) {
			Long internalTeamId = teamMappingInfo.get().getInternalTeamId();
			return teamRepository.findById(internalTeamId)
				.orElse(createAndMapLeague(provider, internalSportId, externalTeamId, name, cc, imageUrl));
		}

		return createAndMapLeague(provider, internalSportId, externalTeamId, name, cc, imageUrl);
	}

	private Team createAndMapLeague(ApiProvider provider, Long internalSportId, String externalTeamId, String name,
		String cc, String imageUrl) {
		Team savedTeam = teamRepository.save(Team.builder()
			.sport(sportRepository.getReferenceById(internalSportId))
			.kName(name)
			.eName(name)
			.cc(cc)
			.imageUrl(imageUrl)
			.build());

		teamExternalMappingRepository.save(TeamExternalMapping.of(provider, externalTeamId, savedTeam.getId()));

		return savedTeam;
	}
}
