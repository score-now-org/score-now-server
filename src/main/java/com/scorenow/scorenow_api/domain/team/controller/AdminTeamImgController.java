package com.scorenow.scorenow_api.domain.team.controller;

import com.scorenow.scorenow_api.domain.team.service.AdminTeamImgService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/teams")
@RequiredArgsConstructor
public class AdminTeamImgController implements AdminTeamImgApiDocs {

    private final AdminTeamImgService adminTeamImgService;

    // 추후 필요 시 파일크기나 content-Type 추가 예정
    @Override
    @PostMapping(
            value = "/{teamId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<Void> uploadImage(
            @PathVariable Long teamId,
            @RequestPart("image") MultipartFile image) {

        adminTeamImgService.uploadImage(teamId, image);
        return ApiResponse.success();
    }

    @Override
    @DeleteMapping("/{teamId}/image")
    public ApiResponse<Void> deleteImage(
            @PathVariable Long teamId) {

        adminTeamImgService.deleteImage(teamId);
        return ApiResponse.success();
    }
}
