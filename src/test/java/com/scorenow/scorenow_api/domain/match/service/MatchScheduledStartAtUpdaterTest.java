package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MatchScheduledStartAtUpdaterTest {

    @InjectMocks
    private MatchScheduledStartAtUpdater updater;

    @Mock
    private AdminFeaturedMatchService adminFeaturedMatchService;

    @Test
    void 경기_시작_일자가_변경되면_상단고정_핫매치를_해제한다() {
        Match match = createMatch(LocalDateTime.of(2026, 7, 23, 19, 0));
        LocalDateTime newStartAt = LocalDateTime.of(2026, 7, 24, 19, 0);

        updater.update(match, newStartAt);

        then(adminFeaturedMatchService).should().deleteFeaturedMatchByMatchId(match.getId());
        assertThat(match.getStartAt()).isEqualTo(newStartAt);
    }

    @Test
    void 경기_시작_시간만_변경되면_상단고정_핫매치를_유지한다() {
        Match match = createMatch(LocalDateTime.of(2026, 7, 23, 19, 0));
        LocalDateTime newStartAt = LocalDateTime.of(2026, 7, 23, 21, 0);

        updater.update(match, newStartAt);

        then(adminFeaturedMatchService).should(never()).deleteFeaturedMatchByMatchId(match.getId());
        assertThat(match.getStartAt()).isEqualTo(newStartAt);
    }

    @Test
    void 같은_경기_시작_시각이면_아무것도_변경하지_않는다() {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 23, 19, 0);
        Match match = createMatch(startAt);

        updater.update(match, startAt);

        then(adminFeaturedMatchService).shouldHaveNoInteractions();
        assertThat(match.getStartAt()).isEqualTo(startAt);
    }

    private Match createMatch(LocalDateTime startAt) {
        return Match.builder()
                .id(1L)
                .startAt(startAt)
                .build();
    }
}
