package com.scorenow.scorenow_api.domain.player.service;

import com.scorenow.scorenow_api.domain.player.dto.request.PlayerAvailabilitySearchCondition;
import com.scorenow.scorenow_api.domain.player.dto.response.AdminPlayerAvailabilityResponse;
import com.scorenow.scorenow_api.domain.player.entity.PlayerAvailabilityStatus;
import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;
import com.scorenow.scorenow_api.domain.player.repository.PlayerTeamDetailRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AdminPlayerAvailabilityService {

    private final PlayerTeamDetailRepository playerTeamDetailRepository;

    // 검색 기능 (전체 조회, 선수명, 선수ID, 팀명, 팀ID)
    public Page<AdminPlayerAvailabilityResponse> searchPlayers(PlayerAvailabilitySearchCondition condition, Pageable pageable) {
        PlayerAvailabilitySearchCondition safeCondition = condition != null
                ? condition
                : new PlayerAvailabilitySearchCondition();

        return playerTeamDetailRepository.searchPlayers(
                        safeCondition.getPlayerId(),
                        normalize(safeCondition.getPlayerName()),
                        safeCondition.getTeamId(),
                        normalize(safeCondition.getTeamName()),
                        pageable
                )
                .map(AdminPlayerAvailabilityResponse::from);
    }


    /**
     * 선수 가용 상태 변경
     * 관리자 스쿼드/결장자 페이지에서 선수의 상태를 변경 시 사용된다.
     */
    @Transactional
    public void updatePlayerAvailabilityStatus(Long playerTeamDetailId, PlayerAvailabilityStatus newStatus) {
        PlayerTeamDetail playerTeamDetail = playerTeamDetailRepository.findById(playerTeamDetailId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PARAMETER, "존재하지 않는 PlayerTeamDetail 입니다."));

        playerTeamDetail.updateAvailabilityStatus(newStatus);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

}
