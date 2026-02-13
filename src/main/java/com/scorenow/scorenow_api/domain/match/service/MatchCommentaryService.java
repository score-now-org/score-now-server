package com.scorenow.scorenow_api.domain.match.service;


import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.CommentaryCreateRequest;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchCommentaryRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchDetailRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.s3.S3Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchCommentaryService {
    private final MatchCommentaryRepository commentaryRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final S3Service s3Service;

    @Transactional
    public String saveCommentary(String eventId, CommentaryCreateRequest request, MultipartFile file) throws IOException {
        String imageUrl = s3Service.uploadFile(file);

        MatchCommentaryDocument commentary = MatchCommentaryDocument.builder()
                .eventId(eventId)
                .minute(request.getMinute())
                .content(request.getContent())
                .imageUrl(imageUrl)
                .build();
        MatchCommentaryDocument saved = commentaryRepository.save(commentary);

        MatchDetailDocument detail = matchDetailRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));

        detail.updateCurrentCommentary(saved.getContent(), saved.getId());

        matchDetailRepository.save(detail);

        return saved.getId();
    }
}