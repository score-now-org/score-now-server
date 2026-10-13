package com.scorenow.scorenow_api.domain.sport.controller;

import com.scorenow.scorenow_api.domain.match.dto.response.SportAppItemResponse;
import com.scorenow.scorenow_api.domain.sport.service.SportAppQueryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app/sports")
public class SportAppController {

    private final SportAppQueryService sportAppQueryService;

    /**
     * 앱 지원 종목 조회
     */
    @GetMapping
    public ApiResponse<List<SportAppItemResponse>> getSupportedSports() {
        return ApiResponse.success(sportAppQueryService.getSupportedSports());
    }
}
