package com.scorenow.scorenow_api.domain.match.service.appresponse;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.dto.response.BaseballAppMatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.service.appresponse.baseball.BaseballMatchAppSportResponseMapper;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchAppSportResponseMapperRegistryTest {

    private final MatchAppSportResponseMapperRegistry registry =
            new MatchAppSportResponseMapperRegistry(List.of(new BaseballMatchAppSportResponseMapper()));

    @Test
    void 등록된_야구_Mapper를_통해_종목별_상세응답을_변환한다() {
        MatchDetailDocument matchDetail = MatchDetailDocument.builder()
                .type(SportCode.BASEBALL)
                .sportDetail(BaseballDetail.empty())
                .build();

        assertThat(registry.mapDetail(matchDetail))
                .isInstanceOf(BaseballAppMatchDetailResponse.class);
    }

    @Test
    void Mapper가_등록되지_않은_종목은_종목별_응답을_반환하지_않는다() {
        MatchDetailDocument matchDetail = MatchDetailDocument.builder()
                .type(SportCode.FOOTBALL)
                .build();

        assertThat(registry.mapDetail(matchDetail)).isNull();
        assertThat(registry.mapLiveSummary(null, matchDetail)).isNull();
    }
}
