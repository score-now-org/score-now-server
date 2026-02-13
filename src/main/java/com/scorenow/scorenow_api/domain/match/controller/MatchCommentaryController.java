package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.CommentaryCreateRequest;
import com.scorenow.scorenow_api.domain.match.service.MatchCommentaryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchCommentaryController {
    private final MatchCommentaryService commentaryService;

    @PostMapping("/{eventId}/commentary")
    public ApiResponse<String> createCommentary(
            @PathVariable String eventId,
            @RequestPart(value = "data") CommentaryCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        String savedId = commentaryService.saveCommentary(eventId, request, image);
        return ApiResponse.success(savedId);
    }
}