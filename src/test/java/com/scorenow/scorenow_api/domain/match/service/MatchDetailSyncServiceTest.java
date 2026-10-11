package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.synchronizer.InplayMatchDetailSynchronizerRegistry;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.sport.service.SportCodeResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MatchDetailSyncServiceTest {

    @InjectMocks
    private MatchDetailSyncService matchDetailSyncService;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private SportCodeResolver sportCodeResolver;

    @Mock
    private InplayMatchDetailSynchronizerRegistry synchronizerRegistry;

    @Test
    @DisplayName("진행 중인 경기가 없으면 종목 분류와 동기화를 수행하지 않는다.")
    void syncInplayMatchDetails_WhenNoInplayMatches_ThenReturnEarly() {
        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY))
                .willReturn(List.of());

        matchDetailSyncService.syncInplayMatchDetails();

        verifyNoInteractions(sportCodeResolver, synchronizerRegistry);
    }

    @Test
    @DisplayName("진행 중인 경기를 종목별로 분리하여 각 Synchronizer에 위임한다.")
    void syncInplayMatchDetails_WhenMultipleSportsExist_ThenDelegateBySportCode() {
        Match footballMatch = createMatch(1L, 10L, "football-1");
        Match baseballMatch = createMatch(2L, 20L, "baseball-1");

        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY))
                .willReturn(List.of(footballMatch, baseballMatch));
        given(sportCodeResolver.resolveByInternalSportId(10L)).willReturn(SportCode.FOOTBALL);
        given(sportCodeResolver.resolveByInternalSportId(20L)).willReturn(SportCode.BASEBALL);

        matchDetailSyncService.syncInplayMatchDetails();

        verify(synchronizerRegistry).sync(SportCode.FOOTBALL, List.of(footballMatch));
        verify(synchronizerRegistry).sync(SportCode.BASEBALL, List.of(baseballMatch));
    }

    @Test
    @DisplayName("서로 다른 내부 종목 ID가 같은 SportCode로 매핑되면 하나의 그룹으로 합친다.")
    void syncInplayMatchDetails_WhenSportIdsResolveToSameCode_ThenMergeIntoOneGroup() {
        Match firstMatch = createMatch(1L, 10L, "football-1");
        Match secondMatch = createMatch(2L, 11L, "football-2");

        given(matchRepository.findByStatusCodeAndIsManualFalse(MatchStatus.IN_PLAY))
                .willReturn(List.of(firstMatch, secondMatch));
        given(sportCodeResolver.resolveByInternalSportId(10L)).willReturn(SportCode.FOOTBALL);
        given(sportCodeResolver.resolveByInternalSportId(11L)).willReturn(SportCode.FOOTBALL);

        matchDetailSyncService.syncInplayMatchDetails();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Match>> matchesCaptor = ArgumentCaptor.forClass(List.class);
        verify(synchronizerRegistry).sync(eq(SportCode.FOOTBALL), matchesCaptor.capture());
        verify(synchronizerRegistry, never()).sync(eq(SportCode.BASEBALL), anyList());

        assertThat(matchesCaptor.getValue()).containsExactlyInAnyOrder(firstMatch, secondMatch);
    }

    private Match createMatch(Long id, Long sportId, String apiMatchId) {
        return Match.builder()
                .id(id)
                .sportId(sportId)
                .apiMatchId(apiMatchId)
                .statusCode(MatchStatus.IN_PLAY)
                .build();
    }
}
