package com.scorenow.scorenow_api.domain.match.service.statusdisplay;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InplayStatusDisplayResolverRegistry {

    private final Map<SportDetailType, InplayStatusDisplayResolver> resolvers;

    public InplayStatusDisplayResolverRegistry(List<InplayStatusDisplayResolver> resolverList) {
        this.resolvers = resolverList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        InplayStatusDisplayResolver::type,
                        resolver -> resolver));
    }

    public MatchStatusDisplayResponse resolve(SportDetailType type, Match match, MatchDetailDocument matchDetailDocument) {
        InplayStatusDisplayResolver resolver = resolvers.get(type);

        if (resolver == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 종목입니다. type=" + type.name());
        }

        return resolver.resolve(match, matchDetailDocument);
    }
}
