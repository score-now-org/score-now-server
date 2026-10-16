package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.repository.MatchDetailRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.initializer.InitialSportDetailProviderRegistry;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MatchDetailInitializationServiceTest {

    @InjectMocks
    private MatchDetailInitializationService service;

    @Mock
    private MatchDetailRepository matchDetailRepository;

    @Mock
    private InitialSportDetailProviderRegistry providerRegistry;

    @Test
    void 종목에_맞는_초기_상세정보를_포함한_MatchDetailDocument를_생성한다() {
        Long matchId = 1L;
        BaseballDetail initialBaseballDetail = BaseballDetail.builder()
                .detailedBroadcastEnabled(false)
                .build();

        given(providerRegistry.create(SportCode.BASEBALL))
                .willReturn(initialBaseballDetail);

        service.createIfAbsent(matchId, SportCode.BASEBALL);

        ArgumentCaptor<MatchDetailDocument> documentCaptor =
                ArgumentCaptor.forClass(MatchDetailDocument.class);
        then(matchDetailRepository).should()
                .createInitialMatchDetailIfAbsent(documentCaptor.capture());

        MatchDetailDocument initialDocument = documentCaptor.getValue();
        assertThat(initialDocument.getId()).isEqualTo(matchId);
        assertThat(initialDocument.getType()).isEqualTo(SportCode.BASEBALL);
        assertThat(initialDocument.getSportDetail()).isSameAs(initialBaseballDetail);
    }
}
