package com.scorenow.scorenow_api.domain.match.service.stage;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.stage.EndedStageDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.stage.MatchStageResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.result.MatchResultResolverRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class MatchStageResponseResolver {

    private final SportInplayStageResolverRegistry sportInplayStageResolverRegistry;
    private final MatchResultResolverRegistry matchResultResolverRegistry;

    public MatchStageResponse resolve(Match match, MatchDetailDocument matchDetailDocument) {

        MatchStatus status = match.getStatusCode();

        if (status == null) {
            return MatchStageResponse.empty();
        }

        return switch (status) {
            case NOT_STARTED -> toScheduledMatchStageResponse(match);
            case IN_PLAY -> toInplayMatchStageResponse(match, matchDetailDocument);
            case ENDED -> toEndedMatchStageResponse(match, matchDetailDocument);

            case POSTPONED, CANCELLED, ABANDONED, INTERRUPTED,
                 TO_BE_FIXED, WALKOVER, RETIRED, REMOVED -> toStatusStageResponse(status);
        };
    }

    private MatchStageResponse toScheduledMatchStageResponse(Match match) {
        if (match.getStartAt() == null) {
            return MatchStageResponse.empty();
        }

        return MatchStageResponse.builder()
                .displayText(match.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private MatchStageResponse toEndedMatchStageResponse(Match match, MatchDetailDocument matchDetailDocument) {
        MatchResult matchResult = matchResultResolverRegistry.resolveResult(match, matchDetailDocument);

        EndedStageDetailResponse endedStageDetailResponse = EndedStageDetailResponse.builder()
                .result(matchResult.name())
                .winnerTeamId(matchResultResolverRegistry.resolveWinnerTeamId(match, matchDetailDocument, matchResult))
                .winnerTeamName(matchResultResolverRegistry.resolveWinnerTeamName(match, matchDetailDocument, matchResult))
                .build();

        return MatchStageResponse.builder()
                .displayText(matchResult.getDescription())
                .detail(endedStageDetailResponse)
                .build();
    }

    private MatchStageResponse toStatusStageResponse(MatchStatus status) {
        return MatchStageResponse.builder()
                .displayText(status.getDescription())
                .build();
    }

    private MatchStageResponse toInplayMatchStageResponse(Match match, MatchDetailDocument matchDetailDocument) {
        if (matchDetailDocument == null || matchDetailDocument.getType() == null) {
            return MatchStageResponse.empty();
        }

        return sportInplayStageResolverRegistry.resolve(matchDetailDocument.getType(), match, matchDetailDocument);
    }
}
