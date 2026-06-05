package com.scorenow.scorenow_api.domain.match.service;


import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;
import com.scorenow.scorenow_api.domain.match.repository.MatchCommentaryRepository;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
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
    private final MatchRepository matchRepository;

    private final FileStorage fileStorage;

    /**
     * 중계 멘트 저장 <br>
     * (단, 중계 멘트 저장 ON/OFF 에 따라서 작성한 중계글 모음에서 조회 여부가 결정)
     */
    public String saveCommentary(Long matchId, String minute, String content, boolean recordEnabled, MultipartFile file) {
        String imageUrl = null;

        if (file != null && !file.isEmpty()) {
            imageUrl = fileStorage.uploadFile(file);
        }

        // 1. matchId 기반으로 존재하는 경기인지 먼저 확인
        validateMatchExists(matchId);

        // 2. mongoDB 에 저장할 MatchCommentaryDocument 인스턴스 생성 후 저장
        MatchCommentaryDocument commentary = MatchCommentaryDocument.builder()
                .matchId(matchId)
                .currentMatchTime(minute)
                .content(content)
                .imageUrl(imageUrl)
                .visible(recordEnabled)
                .build();

        MatchCommentaryDocument savedCommentary = commentaryRepository.save(commentary);

        // 3. MatchDetailDocument 가 있으면 현재 중계 멘트만 갱신하고, 없으면 현재 중계 멘트만 가진 문서를 생성한다.
        matchDetailRepository.upsertCurrentCommentary(matchId, content, savedCommentary.getId());

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

    private void validateMatchExists(Long matchId) {
        if (!matchRepository.existsById(matchId)) {
            throw new BusinessException(ErrorCode.MATCH_NOT_FOUND);
        }
    }
}
