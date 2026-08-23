package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.AdminMatchSearchOptionsResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchTeamCandidateResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Tag(name = "Admin Match API", description = "관리자 경기 관리 API")
public interface AdminMatchApiDocs {

    @Operation(
            summary = "경기 리스트 검색 옵션 조회",
            description = "관리자 경기 목록 검색 화면에서 사용할 종목, 리그, 경기 상태, 앱 노출 여부, 자동/수동 여부 옵션을 조회합니다."
    )
    ApiResponse<AdminMatchSearchOptionsResponse> getSearchOptions();

    @Operation(
            summary = "경기 등록용 팀 후보 검색",
            description = "수동 경기 등록 시 선택할 팀 후보를 검색합니다. keyword와 sportId를 기준으로 팀을 조회합니다."
    )
    ApiResponse<List<MatchTeamCandidateResponse>> getTeamCandidates(
            @Parameter(description = "팀명 검색어", required = true, example = "토트넘")
            String keyword,
            @Parameter(description = "종목 ID", required = true, example = "1")
            Long sportId);

    @Operation(
            summary = "경기 리스트 조회",
            description = """
                    관리자 경기 목록을 페이징 조회합니다.
                    종목, 리그, 상태, 앱 노출 여부, 자동/수동 여부, 날짜 조건으로 검색할 수 있습니다.
                    """
    )
    ApiResponse<Page<MatchListResponse>> getMatches(
            @ParameterObject MatchSearchCondition condition,
            @ParameterObject Pageable pageable);

    @Operation(
            summary = "경기 수동 등록",
            description = """
                    관리자 페이지에서 경기를 수동 등록합니다.
                    등록 성공 시 응답 data는 없습니다.
                    """
    )
    ApiResponse<Void> createMatch(
            @Parameter(description = "생성할 경기 정보", required = true)
            MatchCreateRequest request);

    @Operation(
            summary = "경기 수정",
            description = """
                    경기 기본 정보를 부분 수정합니다.
                    요청에 포함된 필드만 변경됩니다.
                    statusCode는 MatchStatus enum 값으로 전달합니다.
                    """
    )
    ApiResponse<Void> updateMatch(
            @Parameter(description = "수정할 경기 ID", required = true, example = "1001")
            Long matchId,
            @Parameter(description = "수정할 경기 정보", required = true)
            MatchUpdateRequest request);
}
