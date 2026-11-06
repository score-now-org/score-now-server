package com.scorenow.scorenow_api.external.betsapi.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BetsBaseballViewResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 원본의_away를_실제_홈으로_뒤집고_run_점수도_같은_기준으로_반환한다() throws Exception {
        String json = """
                {
                  "results": [{
                    "id": "api-1000",
                    "home": {"id": "actual-away"},
                    "away": {"id": "actual-home"},
                    "ss": "4-1",
                    "scores": {
                      "run": {"home": "4", "away": "1"}
                    }
                  }]
                }
                """;

        BetsBaseballViewResponse response = objectMapper.readValue(
                json,
                BetsBaseballViewResponse.class
        );
        BetsBaseballViewResponse.ViewResult result = response.getResults().get(0);

        assertThat(result.getHomeTeam().getId()).isEqualTo("actual-home");
        assertThat(result.getAwayTeam().getId()).isEqualTo("actual-away");
        assertThat(result.getHomeScore()).isEqualTo(1);
        assertThat(result.getAwayScore()).isEqualTo(4);
    }

    @Test
    void run_점수가_없으면_원정_홈_순서인_ss를_실제_홈_원정으로_뒤집어_반환한다() throws Exception {
        String json = """
                {
                  "results": [{
                    "id": "api-1000",
                    "ss": "4-1"
                  }]
                }
                """;

        BetsBaseballViewResponse response = objectMapper.readValue(
                json,
                BetsBaseballViewResponse.class
        );
        BetsBaseballViewResponse.ViewResult result = response.getResults().get(0);

        assertThat(result.getHomeScore()).isEqualTo(1);
        assertThat(result.getAwayScore()).isEqualTo(4);
    }
}
