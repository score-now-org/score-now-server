package com.scorenow.scorenow_api.domain.league.service;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonResponse;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminLeagueSeasonServiceTest {

    @Mock
    private LeagueSeasonRepository leagueSeasonRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @InjectMocks
    private AdminLeagueSeasonService adminLeagueSeasonService;

    @Test
    void 리그_시즌을_생성한다() {
        given(leagueRepository.existsById(1L)).willReturn(true);
        given(leagueSeasonRepository.existsByLeagueIdAndSeasonName(1L, "2025/26")).willReturn(false);
        given(leagueSeasonRepository.findAllByLeagueIdAndIsActiveTrue(1L)).willReturn(List.of());

        adminLeagueSeasonService.createLeagueSeason(createRequest(
                1L,
                " 2025/26 ",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false
        ));

        ArgumentCaptor<LeagueSeason> captor = ArgumentCaptor.forClass(LeagueSeason.class);
        then(leagueSeasonRepository).should().save(captor.capture());

        LeagueSeason saved = captor.getValue();
        assertThat(saved.getLeagueId()).isEqualTo(1L);
        assertThat(saved.getSeasonName()).isEqualTo("2025/26");
        assertThat(saved.isCurrent()).isFalse();
    }

    @Test
    void 존재하지_않는_리그면_시즌_생성에_실패한다() {
        given(leagueRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> adminLeagueSeasonService.createLeagueSeason(createRequest(
                1L,
                "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LEAGUE_NOT_FOUND);

        then(leagueSeasonRepository).should(never()).save(any());
    }

    @Test
    void 동일한_리그에_같은_시즌명이_있으면_시즌_생성에_실패한다() {
        given(leagueRepository.existsById(1L)).willReturn(true);
        given(leagueSeasonRepository.existsByLeagueIdAndSeasonName(1L, "2025/26")).willReturn(true);

        assertThatThrownBy(() -> adminLeagueSeasonService.createLeagueSeason(createRequest(
                1L,
                "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(leagueSeasonRepository).should(never()).save(any());
    }

    @Test
    void 시즌_시작일이_종료일보다_늦으면_시즌_생성에_실패한다() {
        given(leagueRepository.existsById(1L)).willReturn(true);
        given(leagueSeasonRepository.existsByLeagueIdAndSeasonName(1L, "2025/26")).willReturn(false);

        assertThatThrownBy(() -> adminLeagueSeasonService.createLeagueSeason(createRequest(
                1L,
                "2025/26",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 31),
                false
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(leagueSeasonRepository).should(never()).save(any());
    }

    @Test
    void 시즌_일정이_겹치면_시즌_생성에_실패한다() {
        LeagueSeason existingSeason = season(1L, 1L, "2024/25",
                LocalDate.of(2024, 8, 1),
                LocalDate.of(2025, 5, 31),
                false);

        given(leagueRepository.existsById(1L)).willReturn(true);
        given(leagueSeasonRepository.existsByLeagueIdAndSeasonName(1L, "2025/26")).willReturn(false);
        given(leagueSeasonRepository.findAllByLeagueIdAndIsActiveTrue(1L)).willReturn(List.of(existingSeason));

        assertThatThrownBy(() -> adminLeagueSeasonService.createLeagueSeason(createRequest(
                1L,
                "2025/26",
                LocalDate.of(2025, 5, 1),
                LocalDate.of(2026, 5, 31),
                false
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        then(leagueSeasonRepository).should(never()).save(any());
    }

    @Test
    void 현재_시즌으로_생성하면_기존_현재_시즌을_해제한다() {
        LeagueSeason currentSeason = season(1L, 1L, "2024/25",
                LocalDate.of(2024, 8, 1),
                LocalDate.of(2025, 5, 31),
                true);

        given(leagueRepository.existsById(1L)).willReturn(true);
        given(leagueSeasonRepository.existsByLeagueIdAndSeasonName(1L, "2025/26")).willReturn(false);
        given(leagueSeasonRepository.findAllByLeagueIdAndIsActiveTrue(1L)).willReturn(List.of(currentSeason));
        given(leagueSeasonRepository.findCurrentSeasonByLeagueId(1L)).willReturn(Optional.of(currentSeason));

        adminLeagueSeasonService.createLeagueSeason(createRequest(
                1L,
                "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                true
        ));

        assertThat(currentSeason.isCurrent()).isFalse();
        then(leagueSeasonRepository).should().save(any(LeagueSeason.class));
    }

    @Test
    void 리그_시즌을_검색한다() {
        PageRequest pageable = PageRequest.of(0, 20);
        LeagueSeasonSearchCondition condition = new LeagueSeasonSearchCondition();
        condition.setLeagueId(1L);
        condition.setLeagueName(" Premier ");
        AdminLeagueSeasonResponse response = new AdminLeagueSeasonResponse(
                1L,
                "Premier League",
                10L,
                "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                true
        );
        given(leagueSeasonRepository.searchLeagueSeasons(1L, "Premier", pageable))
                .willReturn(new PageImpl<>(List.of(response), pageable, 1));

        Page<AdminLeagueSeasonResponse> result = adminLeagueSeasonService.searchLeagueSeasons(condition, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getLeagueSeasonId()).isEqualTo(10L);
    }

    @Test
    void 리그_시즌을_부분_수정한다() {
        LeagueSeason target = season(10L, 1L, "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false);

        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(target));
        given(leagueSeasonRepository.findAllByLeagueIdAndIsActiveTrue(1L)).willReturn(List.of(target));

        adminLeagueSeasonService.updateLeagueSeason(10L, updateRequest(
                "2025/2026",
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2026, 6, 30),
                null
        ));

        assertThat(target.getSeasonName()).isEqualTo("2025/2026");
        assertThat(target.getSeasonStartAt()).isEqualTo(LocalDate.of(2025, 7, 1));
        assertThat(target.getSeasonEndAt()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void 수정_시_시즌명이_중복되면_실패한다() {
        LeagueSeason target = season(10L, 1L, "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false);
        LeagueSeason other = season(11L, 1L, "2026/27",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2027, 5, 31),
                false);

        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(target));
        given(leagueSeasonRepository.findAllByLeagueIdAndIsActiveTrue(1L)).willReturn(List.of(target, other));

        assertThatThrownBy(() -> adminLeagueSeasonService.updateLeagueSeason(10L, updateRequest(
                "2026/27",
                null,
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 수정_시_시즌_일정이_겹치면_실패한다() {
        LeagueSeason target = season(10L, 1L, "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false);
        LeagueSeason other = season(11L, 1L, "2026/27",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2027, 5, 31),
                false);

        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(target));
        given(leagueSeasonRepository.findAllByLeagueIdAndIsActiveTrue(1L)).willReturn(List.of(target, other));

        assertThatThrownBy(() -> adminLeagueSeasonService.updateLeagueSeason(10L, updateRequest(
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 1),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);
    }

    @Test
    void 현재_시즌으로_수정하면_기존_현재_시즌을_해제한다() {
        LeagueSeason target = season(10L, 1L, "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                false);
        LeagueSeason otherCurrent = season(11L, 1L, "2024/25",
                LocalDate.of(2024, 8, 1),
                LocalDate.of(2025, 5, 31),
                true);

        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(target));
        given(leagueSeasonRepository.findAllByLeagueIdAndIsActiveTrue(1L)).willReturn(List.of(target, otherCurrent));

        adminLeagueSeasonService.updateLeagueSeason(10L, updateRequest(null, null, null, true));

        assertThat(target.isCurrent()).isTrue();
        assertThat(otherCurrent.isCurrent()).isFalse();
    }

    @Test
    void 리그_시즌을_삭제하면_비활성화하고_현재_시즌을_해제한다() {
        LeagueSeason target = season(10L, 1L, "2025/26",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2026, 5, 31),
                true);
        given(leagueSeasonRepository.findByIdAndIsActiveTrue(10L)).willReturn(Optional.of(target));

        adminLeagueSeasonService.deleteLeagueSeason(10L);

        assertThat(target.isActive()).isFalse();
        assertThat(target.isCurrent()).isFalse();
    }

    private LeagueSeasonCreateRequest createRequest(
            Long leagueId,
            String seasonName,
            LocalDate startAt,
            LocalDate endAt,
            boolean current
    ) {
        LeagueSeasonCreateRequest request = mock(LeagueSeasonCreateRequest.class);
        lenient().when(request.getLeagueId()).thenReturn(leagueId);
        lenient().when(request.getSeasonName()).thenReturn(seasonName);
        lenient().when(request.getSeasonStartAt()).thenReturn(startAt);
        lenient().when(request.getSeasonEndAt()).thenReturn(endAt);
        lenient().when(request.isCurrent()).thenReturn(current);
        return request;
    }

    private LeagueSeasonUpdateRequest updateRequest(
            String seasonName,
            LocalDate startAt,
            LocalDate endAt,
            Boolean current
    ) {
        LeagueSeasonUpdateRequest request = mock(LeagueSeasonUpdateRequest.class);
        lenient().when(request.getSeasonName()).thenReturn(seasonName);
        lenient().when(request.getSeasonStartAt()).thenReturn(startAt);
        lenient().when(request.getSeasonEndAt()).thenReturn(endAt);
        lenient().when(request.getIsCurrent()).thenReturn(current);
        return request;
    }

    private LeagueSeason season(
            Long id,
            Long leagueId,
            String seasonName,
            LocalDate startAt,
            LocalDate endAt,
            boolean current
    ) {
        LeagueSeason leagueSeason = LeagueSeason.create(leagueId, seasonName, startAt, endAt, current);
        ReflectionTestUtils.setField(leagueSeason, "id", id);
        return leagueSeason;
    }
}
