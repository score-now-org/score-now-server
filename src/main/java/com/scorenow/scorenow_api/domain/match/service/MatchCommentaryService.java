package com.scorenow.scorenow_api.domain.match.service;


import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;
import com.scorenow.scorenow_api.domain.match.repository.MatchCommentaryRepository;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchCommentaryService {
    private final MatchCommentaryRepository commentaryRepository;
    private final MatchDetailRepository matchDetailRepository;
    private final FileStorage fileStorage;

    /**
     * TODO: matchId 가 유효한지 먼저 검증하는 로직을 추가하는 것도 괜찮을 듯? 기능 자체는 잘 돌아감.
     */

    /**
     * 중계 멘트 저장 <br>
     * (단, 중계 멘트 저장 ON/OFF 에 따라서 작성한 중계글 모음에서 조회 여부가 결정)
     */
    public String saveCommentary(Long matchId, String minute, String content, boolean recordEnabled, MultipartFile file) {
        String imageUrl = null;

        if (file != null && !file.isEmpty()) {
            imageUrl = fileStorage.uploadFile(file);
        }

        // 1. mongoDB 에 저장할 Document 인스턴스 생성
        MatchCommentaryDocument commentary = MatchCommentaryDocument.builder()
                .matchId(matchId)
                .currentMatchTime(minute)
                .content(content)
                .imageUrl(imageUrl)
                .visible(recordEnabled)
                .build();

        // 2. MatchCommentary Document 저장
        MatchCommentaryDocument savedCommentary = commentaryRepository.save(commentary);

        // 3. MatchDetail 에서 관리되는 현재 중계 멘트 업데이트
        long updatedCount = matchDetailRepository.updateCurrentCommentary(matchId, savedCommentary.getContent(), savedCommentary.getId());
        if (updatedCount == 0) {
            log.warn("최신 중계 문구를 업데이트할 경기 정보를 찾을 수 없습니다. matchId:{}", matchId);
            throw new BusinessException(ErrorCode.MATCH_NOT_FOUND, "최신 중계 문구를 업데이트할 경기 정보를 찾을 수 없습니다.");
        }

        return savedCommentary.getId();
    }

    /**
     * 해당 경기에서 작성된 중계 멘트 목록 조회 <br>
     * (단, 중계 멘트 저장 ON 에서 작성된 중계 멘트들만 조회 가능)
     */
    public List<String> getCommentariesByMatchId(Long matchId) {
        return commentaryRepository.findVisibleCommentaries(matchId).stream()
                .map(MatchCommentaryDocument::getContent)
                .toList();
    }
}