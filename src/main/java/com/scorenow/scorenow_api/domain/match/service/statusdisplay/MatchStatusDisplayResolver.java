package com.scorenow.scorenow_api.domain.match.service.statusdisplay;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.EndedStatusDisplayDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.result.MatchResultResolverRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class MatchStatusDisplayResolver {

    private final InplayStatusDisplayResolverRegistry inplayStatusDisplayResolverRegistry;
    private final MatchResultResolverRegistry matchResultResolverRegistry;

    public MatchStatusDisplayResponse resolve(Match match, MatchDetailDocument matchDetailDocument) {

        MatchStatus status = match.getStatusCode();

        if (status == null) {
            return MatchStatusDisplayResponse.empty();
        }

        return switch (status) {
            case NOT_STARTED -> toScheduledMatchStatusDisplayResponse(match);
            case IN_PLAY -> toInplayMatchStatusDisplayResponse(match, matchDetailDocument);
            case ENDED -> toEndedMatchStatusDisplayResponse(match, matchDetailDocument);

            case POSTPONED, CANCELLED, ABANDONED, INTERRUPTED,
                 TO_BE_FIXED, WALKOVER, RETIRED, REMOVED -> toStatusDisplayResponse(status);
        };
    }

    private MatchStatusDisplayResponse toScheduledMatchStatusDisplayResponse(Match match) {
        if (match.getStartAt() == null) {
            return MatchStatusDisplayResponse.empty();
        }

        return MatchStatusDisplayResponse.builder()
                .displayText(match.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private MatchStatusDisplayResponse toEndedMatchStatusDisplayResponse(Match match, MatchDetailDocument matchDetailDocument) {
        MatchResult matchResult = matchResultResolverRegistry.resolveResult(match, matchDetailDocument);

        EndedStatusDisplayDetailResponse endedStatusDisplayDetailResponse = EndedStatusDisplayDetailResponse.builder()
                .result(matchResult.name())
                .winnerTeamId(matchResultResolverRegistry.resolveWinnerTeamId(match, matchDetailDocument, matchResult))
                .winnerTeamName(matchResultResolverRegistry.resolveWinnerTeamName(match, matchDetailDocument, matchResult))
                .build();

        return MatchStatusDisplayResponse.builder()
                .displayText(matchResult.getDescription())
                .detail(endedStatusDisplayDetailResponse)
                .build();
    }

    private MatchStatusDisplayResponse toStatusDisplayResponse(MatchStatus status) {
        return MatchStatusDisplayResponse.builder()
                .displayText(status.getDescription())
                .build();
    }

    private MatchStatusDisplayResponse toInplayMatchStatusDisplayResponse(Match match, MatchDetailDocument matchDetailDocument) {
        if (matchDetailDocument == null || matchDetailDocument.getType() == null) {
            return MatchStatusDisplayResponse.empty();
        }

        return inplayStatusDisplayResolverRegistry.resolve(matchDetailDocument.getType(), match, matchDetailDocument);
    }
}
