package com.scorenow.scorenow_api.domain.league.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaguePersistenceService {

	private final LeagueRepository leagueRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveIfNotExists(League league){
		try{
			if(!leagueRepository.existsById(league.getId())){
				leagueRepository.saveAndFlush(league);
			}
		}catch(DataIntegrityViolationException e){
			log.debug("리그 동시 삽입 무시 - {}", league.getId());
		}
	}
}
