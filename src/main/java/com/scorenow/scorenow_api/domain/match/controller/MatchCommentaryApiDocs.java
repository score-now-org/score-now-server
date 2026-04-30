package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.CommentaryCreateRequest;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Match Commentary API", description = "경기 중계 멘트 관련 API")
public interface MatchCommentaryApiDocs {

    @Operation(summary = "중계 멘트 등록", description = "특정 경기에 새로운 중계 멘트를 등록합니다. 이미지 첨부가 가능합니다.")
    ApiResponse<Void> createCommentary(
            @Parameter(description = "중계 멘트를 등록할 경기 ID", required = true) String matchId,
            @Parameter(description = "중계 멘트 정보", required = true) CommentaryCreateRequest request,
            @Parameter(description = "첨부할 이미지 파일") MultipartFile image);

    @Operation(summary = "중계 멘트 목록 조회", description = "특정 경기에 등록된 중계 멘트 목록을 조회합니다. (중계글 기록 저장 ON 상태에서 작성된 중계 멘트 목록만 조회 가능)")
    ApiResponse<List<String>> getCommentaries(
            @Parameter(description = "중계 멘트 목록을 조회할 경기 ID", required = true) String matchId);
}
