package com.scorenow.scorenow_api.domain.match.service.sportdetail.statistics;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.dto.response.statistics.MatchStatisticsResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MatchStatisticsResolverRegistry {

    // TODO: SportDetailType 사용 멈추고, SportCode 로 교체해야 함. StandingsResolverRegistry 참고하기
    private final Map<SportDetailType, MatchStatisticsResolver> resolvers;

    public MatchStatisticsResolverRegistry(List<MatchStatisticsResolver> resolvers) {
        this.resolvers = resolvers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MatchStatisticsResolver::type,
                        Function.identity())
                );
    }

    public MatchStatisticsResponse resolve(MatchDetailDocument matchDetailDocument) {

        if (matchDetailDocument == null || matchDetailDocument.getType() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "경기 상세 종목 정보가 없습니다.");
        }

        SportDetailType type = matchDetailDocument.getType();

        MatchStatisticsResolver resolver = resolvers.get(type);
        if (resolver == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 종목입니다. type=" + type);
        }

        return resolver.resolve(matchDetailDocument);
    }

}
