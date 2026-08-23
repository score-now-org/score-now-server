package com.scorenow.scorenow_api.domain.community.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.domain.community.dto.response.CommunityBoardResponse;

@Service
public class CommunityBoardService {

	public List<CommunityBoardResponse> getBoards() {
		return CommunityBoardResponse.defaults();
	}
}
