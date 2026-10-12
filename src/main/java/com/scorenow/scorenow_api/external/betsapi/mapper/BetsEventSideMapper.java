package com.scorenow.scorenow_api.external.betsapi.mapper;

import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Bets Event API의 종목별 홈/원정 표기 규칙을 시스템의 홈/원정 기준으로 변환한다.
 *
 * <p>축구는 Bets의 home-away가 실제 홈-원정과 일치하지만,
 * 야구는 Bets의 home-away가 실제 원정-홈을 의미한다.</p>
 */
@Slf4j
@Component
public class BetsEventSideMapper {

    public MappedSides map(SportCode sportCode, BetsEventResponse.Event event) {
        RawScore rawScore = parseRawScore(event);

        return switch (sportCode) {
            case FOOTBALL -> new MappedSides(
                    event.getHome(),
                    event.getAway(),
                    rawScore.first(),
                    rawScore.second()
            );
            case BASEBALL -> new MappedSides(
                    event.getAway(),
                    event.getHome(),
                    rawScore.second(),
                    rawScore.first()
            );
        };
    }

    private RawScore parseRawScore(BetsEventResponse.Event event) {
        String score = event.getSs();
        if (score == null || score.isBlank()) {
            return RawScore.empty();
        }

        String[] parts = score.split("-", -1);
        if (parts.length != 2) {
            log.warn("🟠유효하지 않은 점수 값 입니다. dataOrigin:BETS, eventId:{}, ss='{}'", event.getId(), score);
            return RawScore.empty();
        }

        try {
            return new RawScore(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim())
            );
        } catch (NumberFormatException e) {
            log.warn("🟠유효하지 않은 점수 값 입니다. dataOrigin:BETS, eventId:{}, ss='{}'", event.getId(), score);
            return RawScore.empty();
        }
    }

    public record MappedSides(
            BetsEventResponse.Team homeTeam,
            BetsEventResponse.Team awayTeam,
            Integer homeScore,
            Integer awayScore
    ) {
    }

    private record RawScore(Integer first, Integer second) {
        private static RawScore empty() {
            return new RawScore(null, null);
        }
    }
}
