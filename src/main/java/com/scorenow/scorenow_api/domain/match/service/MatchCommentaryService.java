package com.scorenow.scorenow_api.domain.match.service;


import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.CommentaryCreateRequest;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchCommentaryRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchDetailRepository;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchCommentaryService {
    private final MatchCommentaryRepository commentaryRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final FileStorage fileStorage;

    /**
     * 중계 멘트 저장 <br>
     * (단, 중계 멘트 저장 ON/OFF 에 따라서 추후 작성 중계글 모음에서 조회 여부가 결정)
     */
    @Transactional
    public String saveCommentary(String matchId, CommentaryCreateRequest request, MultipartFile file) {
        String imageUrl = fileStorage.uploadFile(file);

        // 1. mongoDB 에 저장할 Document 인스턴스 생성
        MatchCommentaryDocument commentary = MatchCommentaryDocument.builder()
                .matchId(matchId)
                .currentMatchTime(request.getMinute())
                .content(request.getContent())
                .imageUrl(imageUrl)
                .visible(request.isRecordEnabled())
                .build();

        // 2. MatchCommentary Document 저장
        MatchCommentaryDocument savedCommentary = commentaryRepository.save(commentary);

        // 3. MatchDetail 에서 관리되는 현재 중계 멘트 업데이트
        matchDetailRepository.updateCurrentCommentary(matchId, savedCommentary.getContent(), savedCommentary.getId());

        return savedCommentary.getId();
    }

    /**
     * 해당 경기에서 작성된 중계 멘트 목록 조회 <br>
     * (단, 중계 멘트 저장 ON 에서 작성된 중계 멘트들만 조회 가능)
     */
    public List<String> getCommentariesByMatchId(String matchId) {
        return commentaryRepository.findVisibleCommentaries(matchId).stream()
                .map(MatchCommentaryDocument::getContent)
                .toList();
    }
}