package com.scorenow.scorenow_api.domain.match.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;

class MatchMapperTest {

    private final MatchMapper matchMapper = new MatchMapper();

    @ParameterizedTest
    @CsvSource({
            "BETS, 00123456, false",
            "BETS, evt-7, true",
            "MANUAL, , true",
            ", , false"
    })
    void 저장된_경기_원천과_외부_ID를_목록_응답에_매핑한다(
            DataOrigin dataOrigin, String apiMatchId, boolean manual) {
        Match match = Match.builder()
                .dataOrigin(dataOrigin)
                .apiMatchId(apiMatchId)
                .isManual(manual)
                .statusCode(MatchStatus.NOT_STARTED)
                .build();

        MatchListResponse response = matchMapper.toResponse(match);

        assertThat(response.getDataOrigin()).isEqualTo(dataOrigin);
        assertThat(response.getApiMatchId()).isEqualTo(apiMatchId);
        assertThat(response.isManual()).isEqualTo(manual);
    }
}
