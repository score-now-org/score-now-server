package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.CommentaryCreateRequest;
import com.scorenow.scorenow_api.domain.match.service.MatchCommentaryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchCommentaryController implements MatchCommentaryApiDocs {

    private final MatchCommentaryService commentaryService;

    /**
     * 중계 멘트 등록
     */
    @PostMapping("/{matchId}/commentaries")
    public ApiResponse<String> createCommentary(
            @PathVariable Long matchId,
            @Valid @RequestPart(value = "data") CommentaryCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        String savedCommentaryId = commentaryService.saveCommentary(matchId, request.getMinute(), request.getContent(), request.isRecordEnabled(), image);
        return ApiResponse.success(savedCommentaryId);
    }

    /**
     * 작성한 중계 멘트 목록 조회
     */
    @GetMapping("/{matchId}/commentaries")
    public ApiResponse<List<String>> getCommentaries(@PathVariable Long matchId) {
        return ApiResponse.success(commentaryService.getCommentariesByMatchId(matchId));
    }
}