package com.scorenow.scorenow_api.domain.stadium.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.*;

import com.scorenow.scorenow_api.domain.stadium.dto.response.StadiumResponse;
import com.scorenow.scorenow_api.domain.stadium.service.StadiumService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
public class StadiumController implements StadiumApiDocs {

    private final StadiumService stadiumService;

    /**
     * 모든 경기장 조회
     */
    @GetMapping
    public ApiResponse<List<StadiumResponse>> getAllStadiums() {
        List<StadiumResponse> stadiums = stadiumService.getAllStadiums().stream()
                .map(StadiumResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(stadiums);
    }

    /**
     * 경기장 이름으로 검색
     */
    @GetMapping(value = "/search", params = "name")
    public ApiResponse<List<StadiumResponse>> searchStadiumsByName(@RequestParam String name) {
        List<StadiumResponse> stadiums = stadiumService.getStadiumByStadiumName(name).stream()
                .map(StadiumResponse::from)
                .toList();
        return ApiResponse.success(stadiums);
    }

    /**
     * 경기장 ID 로 검색
     */
    @GetMapping("/{stadiumId}")
    public ApiResponse<List<StadiumResponse>> searchStadiumByStadiumId(@PathVariable Long stadiumId) {
        List<StadiumResponse> stadiums = stadiumService.getStadiumByStadiumId(stadiumId)
                .stream()
                .map(StadiumResponse::from)
                .toList();
        return ApiResponse.success(stadiums);
    }

    /**
     * TODO: 추후 경기장 등록 관리자 페이지 개발 시 진행
     * 경기장 단건 수동 생성
     */
//    @PostMapping
//    public ApiResponse<StadiumResponse> createStadium(@Valid @RequestBody StadiumCreateRequest request) {
//        Stadium saved = stadiumService.getOrCreateStadium(request.toEntity());
//        return ApiResponse.success(StadiumResponse.from(saved));
//    }
}
