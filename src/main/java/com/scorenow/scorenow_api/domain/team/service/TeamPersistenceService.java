package com.scorenow.scorenow_api.domain.team.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamPersistenceService {

	private final TeamRepository teamRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveIfNotExists(Team team){
		try{
			if(!teamRepository.existsById(team.getId())){
				teamRepository.saveAndFlush(team);
			}
		}catch (DataIntegrityViolationException e){
			log.debug("팀 동시 삽입 무시 - {}", team.getId());
		}
	}
}
