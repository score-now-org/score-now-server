package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeagueSeasonTest {

    @Test
    void 리그_시즌을_생성한다() {
        LeagueSeason leagueSeason = LeagueSeason.create(
                1L,
                "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                true
        );

        assertThat(leagueSeason.getLeagueId()).isEqualTo(1L);
        assertThat(leagueSeason.getSeasonName()).isEqualTo("2025/26");
        assertThat(leagueSeason.getSeasonStartAt()).isEqualTo(LocalDate.of(2025, 8, 1));
        assertThat(leagueSeason.getSeasonEndAt()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(leagueSeason.isCurrent()).isTrue();
        assertThat(leagueSeason.isActive()).isTrue();
    }

    @Test
    void 리그_ID가_null이면_생성에_실패한다() {
        assertThatThrownBy(() -> LeagueSeason.create(
                null,
                "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 시즌명이_null이거나_blank이면_생성에_실패한다() {
        assertThatThrownBy(() -> LeagueSeason.create(
                1L,
                " ",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 시즌_시작일이_종료일보다_늦으면_생성에_실패한다() {
        assertThatThrownBy(() -> LeagueSeason.create(
                1L,
                "2025/26",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 31),
                false
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 시즌명을_수정한다() {
        LeagueSeason leagueSeason = createSeason(1L, "2025/26");

        leagueSeason.updateSeasonName("2026/27");

        assertThat(leagueSeason.getSeasonName()).isEqualTo("2026/27");
    }

    @Test
    void 시즌명을_blank로_수정하면_실패한다() {
        LeagueSeason leagueSeason = createSeason(1L, "2025/26");

        assertThatThrownBy(() -> leagueSeason.updateSeasonName(" "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 시즌_일정을_수정한다() {
        LeagueSeason leagueSeason = createSeason(1L, "2025/26");

        leagueSeason.updatePeriod(
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2026, 6, 30)
        );

        assertThat(leagueSeason.getSeasonStartAt()).isEqualTo(LocalDate.of(2025, 7, 1));
        assertThat(leagueSeason.getSeasonEndAt()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void 시즌_일정을_잘못_수정하면_실패한다() {
        LeagueSeason leagueSeason = createSeason(1L, "2025/26");

        assertThatThrownBy(() -> leagueSeason.updatePeriod(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 6, 30)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 시즌을_비활성화하면_현재_시즌도_해제된다() {
        LeagueSeason leagueSeason = LeagueSeason.create(
                1L,
                "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                true
        );

        leagueSeason.deactivate();

        assertThat(leagueSeason.isActive()).isFalse();
        assertThat(leagueSeason.isCurrent()).isFalse();
    }

    @Test
    void 시즌_기간_겹침을_판단한다() {
        LeagueSeason leagueSeason = createSeason(1L, "2025/26");

        assertThat(leagueSeason.overlapsWith(
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2025, 8, 1)
        )).isTrue();
        assertThat(leagueSeason.overlapsWith(
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 6, 1)
        )).isTrue();
        assertThat(leagueSeason.overlapsWith(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        )).isFalse();
    }

    @Test
    void 같은_ID인지_판단한다() {
        LeagueSeason leagueSeason = createSeason(1L, "2025/26");
        ReflectionTestUtils.setField(leagueSeason, "id", 10L);

        assertThat(leagueSeason.isSameId(10L)).isTrue();
        assertThat(leagueSeason.isSameId(11L)).isFalse();
    }

    private LeagueSeason createSeason(Long leagueId, String seasonName) {
        return LeagueSeason.create(
                leagueId,
                seasonName,
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false
        );
    }
}
