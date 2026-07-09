package com.scorenow.scorenow_api.domain.match.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class MatchEventResolver {

    private static final String DELIMITER = " - ";
    private static final String SCORE = "score";

    /**
     * BETS 의 Event View API 의 응답으로 내려오는 events 정보를 기반으로 각 팀별 승부차기 득점 정보를 계산한다.
     * 예시) "PSG - Score 4th Penalty"
     */
    public Integer resolveShootOutScore(List<String> events, String teamName) {
        if (events == null || events.isEmpty()) {
            return 0;
        }

        if (teamName == null || teamName.isBlank()) {
            return 0;
        }

        String normalizedTeamName = normalize(teamName);

        return events.stream()
                .map(this::normalize)   // 양끝 공백 자르고 + 소문자로 만든다.
                .filter(Objects::nonNull)
                .map(event -> event.split(DELIMITER))   // 각 이벤트 텍스트를 구분자로 나눈다.
                .filter(split -> split.length >= 2) // 구분자로 나눈 개수 검증을 한다.
                .filter(split -> normalizedTeamName.equals(split[0])) // 구분자로 나눈 조각들 중 첫번째 조각은 팀명과 동일해야 한다.
                .filter(split -> split[1].contains(SCORE))  // 구분자로 나눈 조각들 중 두번째 조각에 SCORE 가 포함되어 있으면 득점
                .toList().size();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ?
                value.trim().toLowerCase(Locale.ROOT) :
                null;
    }
}
