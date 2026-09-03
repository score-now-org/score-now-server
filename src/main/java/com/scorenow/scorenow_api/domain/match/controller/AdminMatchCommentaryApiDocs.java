package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.CommentaryCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.AdminMatchCommentaryResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Admin Match Commentary API", description = "경기 중계 멘트 관련 API")
public interface AdminMatchCommentaryApiDocs {

    @Operation(
            summary = "중계 멘트 등록",
            description = """
                    특정 경기에 새로운 중계 멘트를 등록합니다.
                    multipart/form-data 요청이며 data 파트에는 content, highlighted, recordEnabled를 전달합니다.
                    image 파트는 선택 값입니다.
                    등록 성공 시 응답 data는 없습니다.
                    """
    )
    ApiResponse<Void> createCommentary(
            @Parameter(description = "중계 멘트를 등록할 경기 ID", required = true) Long matchId,
            @Parameter(description = "중계 멘트 정보. content, highlighted, recordEnabled 포함", required = true) CommentaryCreateRequest request,
            @Parameter(description = "첨부할 이미지 파일") MultipartFile image);

    @Operation(
            summary = "중계 멘트 목록 조회",
            description = """
                    특정 경기에 등록된 중계 멘트 목록을 조회합니다.
                    중계글 기록 저장 ON 상태에서 작성된 중계 멘트만 조회됩니다.
                    응답은 content, highlighted, createdAt을 포함합니다.
                    """
    )
    ApiResponse<List<AdminMatchCommentaryResponse>> getCommentaries(
            @Parameter(description = "중계 멘트 목록을 조회할 경기 ID", required = true) Long matchId);
}
