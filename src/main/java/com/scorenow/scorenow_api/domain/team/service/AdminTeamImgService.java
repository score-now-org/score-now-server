package com.scorenow.scorenow_api.domain.team.service;

import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import com.scorenow.scorenow_api.global.infra.storage.gcs.GcsService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AdminTeamImgService {

    private final TeamRepository teamRepository;

    private final GcsService gcsService;

    @Transactional
    public void uploadImage(Long teamId, MultipartFile image){

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        String objectName = "teams/" + teamId + "/emblem.png";
        String imageUrl = gcsService.upload(image, objectName);
        log.info(imageUrl);

        team.updateImageUrl(imageUrl);
    }

    @Transactional
    public void deleteImage(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));

        String objectName = "teams/" + teamId + "/emblem.png";
        gcsService.delete(objectName);

        team.updateImageUrl(null);
    }
}
