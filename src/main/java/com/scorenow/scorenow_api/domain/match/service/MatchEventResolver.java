package com.scorenow.scorenow_api.domain.match.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class MatchEventResolver {

    private static final String DELIMITER = "-";
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

        return (int) events.stream()
                .filter(StringUtils::hasText)
                .map(eventText -> eventText.split(DELIMITER))
                .filter(split -> split.length >= 2)
                .filter(split -> {
                    String normalizedTeamName = normalize(teamName);
                    return normalizedTeamName != null && normalizedTeamName.equals(normalize(split[0]));
                })
                .filter(split -> {
                    String detail = normalize(split[1]);
                    return detail != null && detail.contains(SCORE);
                }).count();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ?
                value.trim().toLowerCase(Locale.ROOT) :
                null;
    }
}
