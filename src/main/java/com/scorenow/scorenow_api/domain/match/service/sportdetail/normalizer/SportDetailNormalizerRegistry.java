package com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SportDetailNormalizerRegistry {

    private final Map<SportDetailType, SportDetailNormalizer> normalizers;

    public SportDetailNormalizerRegistry(List<SportDetailNormalizer> normalizerList) {
        this.normalizers = normalizerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SportDetailNormalizer::type,
                        normalizer -> normalizer));
    }

    public SportDetail normalize(SportDetailType type, BetsViewResponse.ViewResult viewResult) {
        SportDetailNormalizer normalizer = normalizers.get(type);
        if (normalizer == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "지원하지 않는 종목입니다. type=" + type.name());
        }

        return normalizer.normalize(viewResult);
    }
}
