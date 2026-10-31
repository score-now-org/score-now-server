package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsDisplayPolicy;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsDisplayPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MatchOddsDisplayPolicyInitializationServiceTest {

    @InjectMocks
    private MatchOddsDisplayPolicyInitializationService initializationService;

    @Mock
    private MatchOddsDisplayPolicyRepository matchOddsDisplayPolicyRepository;

    @Test
    void 국내와_해외_배당률_노출_정책을_자동으로_초기화한다() {
        long matchId = 1L;

        initializationService.initialize(matchId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MatchOddsDisplayPolicy>> captor = ArgumentCaptor.forClass(List.class);
        then(matchOddsDisplayPolicyRepository).should().saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(
                        MatchOddsDisplayPolicy::getMatchId,
                        MatchOddsDisplayPolicy::getCategory,
                        MatchOddsDisplayPolicy::getDisplayedRegistrationType
                )
                .containsExactlyInAnyOrder(
                        tuple(matchId, OddsCategory.DOMESTIC, OddsRegistrationType.AUTOMATIC),
                        tuple(matchId, OddsCategory.OVERSEAS, OddsRegistrationType.AUTOMATIC)
                );
    }
}
