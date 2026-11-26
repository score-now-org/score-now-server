package com.scorenow.scorenow_api.domain.match.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;

class MatchListResponseTest {

    @Test
    void 경기_목록_응답_스키마에_원천과_외부_ID를_문서화한다() {
        Schema<?> response = ModelConverters.getInstance().read(MatchListResponse.class)
                .get("MatchListResponse");

        Schema<?> dataOrigin = response.getProperties().get("dataOrigin");
        assertThat(dataOrigin.getType()).isEqualTo("string");
        assertThat(dataOrigin.getEnum()).isEqualTo(List.of("BETS", "MANUAL"));
        assertThat(dataOrigin.getNullable()).isTrue();
        assertThat(dataOrigin.getDescription()).contains("BetsAPI", "관리자 직접 등록");

        Schema<?> apiMatchId = response.getProperties().get("apiMatchId");
        assertThat(apiMatchId.getType()).isEqualTo("string");
        assertThat(apiMatchId.getNullable()).isTrue();
        assertThat(apiMatchId.getDescription()).contains("외부 경기 ID");

        Schema<?> manual = response.getProperties().get("manual");
        assertThat(manual.getDescription()).contains("현재 경기 관리 모드");
    }
}
