package com.scorenow.scorenow_api.domain.league.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.LeagueAdminResponse;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeagueAdminService {

	private final LeagueRepository leagueRepository;

	public List<LeagueAdminResponse> getLeagues(String keyword){
		return leagueRepository.searchByKeyword(keyword)
			.stream()
			.map(LeagueAdminResponse::from)
			.toList();
	}

	@Transactional
	public LeagueAdminResponse createLeague(LeagueCreateRequest request){
		String id = League.generateLeagueId(request.getSportId(), request.getLeagueId());

		if(leagueRepository.existsById(id)){
			throw new BusinessException(ErrorCode.LEAGUE_ALREADY_EXISTS);
		}

		League league = League.builder()
			.id(id)
			.sportId(request.getSportId())
			.eName(request.getEName())
			.kName(request.getKName())
			.sName(request.getSName())
			.build();

		return LeagueAdminResponse.from(leagueRepository.save(league));
	}

	@Transactional
	public void updateLeague(String id, LeagueUpdateRequest request){
		League league = leagueRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));

		if(request.getSportId() != null) league.setSportId(request.getSportId());
		if(request.getEName() != null) league.setEName(request.getEName());
		if(request.getKName() != null) league.setKName(request.getKName());
		if(request.getSName() != null) league.setSName(request.getSName());
	}

	@Transactional
	public void deleteLeague(String id){
		if(!leagueRepository.existsById(id)){
			throw new BusinessException(ErrorCode.LEAGUE_NOT_FOUND);
		}
		leagueRepository.deleteById(id);
	}
}
