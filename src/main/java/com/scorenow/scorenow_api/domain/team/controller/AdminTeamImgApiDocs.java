package com.scorenow.scorenow_api.domain.team.controller;

import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin Team Image API", description = "관리자 팀 이미지 관리 API")
public interface AdminTeamImgApiDocs {
    @Operation(summary = "팀 이미지 등록/수정", description = "팀 ID로 팀 이미지를 등록 및 수정합니다.")
    ApiResponse<Void> uploadImage(
            @Parameter(description = "등록할 팀 ID", required = true) Long teamId,
            @Parameter(description = "등록할 이미지", required = true) MultipartFile image);

    @Operation(
            summary = "팀 이미지 삭제",
            description = "팀 ID에 해당하는 팀 이미지를 삭제합니다."
    )
    ApiResponse<Void> deleteImage(Long teamId);
}
