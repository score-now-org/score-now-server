package com.scorenow.scorenow_api.domain.match.service.sportdetail.initializer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InitialSportDetailProviderRegistry {

    private final Map<SportCode, InitialSportDetailProvider> providers;

    public InitialSportDetailProviderRegistry(List<InitialSportDetailProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        InitialSportDetailProvider::type,
                        Function.identity()
                ));
    }

    public SportDetail create(SportCode sportCode) {
        InitialSportDetailProvider provider = providers.get(sportCode);
        if (provider == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "지원하지 않는 종목입니다. sportCode=" + sportCode
            );
        }

        return provider.create();
    }
}
