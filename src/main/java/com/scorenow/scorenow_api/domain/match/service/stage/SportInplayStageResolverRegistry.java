package com.scorenow.scorenow_api.domain.match.service.stage;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.dto.response.stage.MatchStageResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SportInplayStageResolverRegistry {

    private final Map<SportDetailType, SportInplayStageResponseResolver> resolvers;

    public SportInplayStageResolverRegistry(List<SportInplayStageResponseResolver> resolverList) {
        this.resolvers = resolverList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SportInplayStageResponseResolver::type,
                        resolver -> resolver));
    }

    public MatchStageResponse resolve(SportDetailType type, Match match, MatchDetailDocument matchDetailDocument) {
        SportInplayStageResponseResolver resolver = resolvers.get(type);

        if (resolver == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 종목입니다. type=" + type.name());
        }

        return resolver.resolve(match, matchDetailDocument);
    }
}
