package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.FootballMatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchDetailResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Match Detail API", description = "관리자 경기 상세 관리 API")
public interface AdminMatchDetailApiDocs {

    @Operation(
            summary = "경기 상세 조회",
            description = """
                    관리자 경기 상세 수정 화면에서 사용할 경기 상세 정보를 조회합니다.
                    현재 응답은 축구 상세 정보 footballDetail을 포함합니다.
                    """
    )
    ApiResponse<MatchDetailResponse> getMatchDetail(
            @Parameter(description = "조회할 경기 ID", required = true, example = "1001")
            Long matchId);

    @Operation(
            summary = "축구 경기 상세 수정",
            description = """
                    축구 경기 상세 정보를 부분 수정합니다.
                    요청에 포함된 필드만 변경됩니다.
                    점수, 상태, 승부차기 점수, 축구 스탯, 시작 시간, 경기 구간, 추가시간을 수정할 수 있습니다.
                    """
    )
    ApiResponse<Void> updateMatchDetail(
            @Parameter(description = "수정할 경기 ID", required = true, example = "1001")
            Long matchId,
            @Parameter(description = "수정할 축구 경기 상세 정보", required = true)
            FootballMatchDetailUpdateRequest request);
}
