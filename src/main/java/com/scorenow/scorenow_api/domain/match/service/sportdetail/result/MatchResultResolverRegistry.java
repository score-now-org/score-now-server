package com.scorenow.scorenow_api.domain.match.service.sportdetail.result;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MatchResultResolverRegistry {

    private final Map<SportDetailType, MatchResultResolver> resolvers;

    public MatchResultResolverRegistry(List<MatchResultResolver> resolvers) {
        this.resolvers = resolvers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MatchResultResolver::type,
                        resolver -> resolver
                ));
    }

    public MatchResult resolveResult(Match match, MatchDetailDocument matchDetailDocument) {
        return getResolver(resolveType(matchDetailDocument)).resolveResult(match, matchDetailDocument);
    }

    public Long resolveWinnerTeamId(
            Match match,
            MatchDetailDocument matchDetailDocument,
            MatchResult matchResult
    ) {
        return getResolver(resolveType(matchDetailDocument)).resolveWinnerTeamId(match, matchResult);
    }

    public String resolveWinnerTeamName(
            Match match,
            MatchDetailDocument matchDetailDocument,
            MatchResult matchResult
    ) {
        return getResolver(resolveType(matchDetailDocument)).resolveWinnerTeamName(match, matchResult);
    }

    private MatchResultResolver getResolver(SportDetailType type) {
        MatchResultResolver resolver = resolvers.get(type);
        if (resolver == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 종목입니다. type=" + type);
        }
        return resolver;
    }

    private SportDetailType resolveType(MatchDetailDocument matchDetailDocument) {
        if (matchDetailDocument == null || matchDetailDocument.getType() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "경기 상세 종목 정보가 없습니다.");
        }

        return matchDetailDocument.getType();
    }
}
