package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.odds.dto.request.ManualMatchOddsRegisterRequest;
import com.scorenow.scorenow_api.domain.odds.dto.response.ManualMatchOddsHistoryResponse;
import com.scorenow.scorenow_api.domain.odds.dto.response.OverseasOddsManagementOverviewResponse;
import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsDisplayPolicy;
import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsStatus;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsDisplayPolicyRepository;
import com.scorenow.scorenow_api.domain.odds.repository.MatchOddsHistoryRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminMatchOddsServiceTest {

    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final MatchOddsHistoryRepository matchOddsHistoryRepository = mock(MatchOddsHistoryRepository.class);
    private final MatchOddsDisplayPolicyRepository matchOddsDisplayPolicyRepository = mock(MatchOddsDisplayPolicyRepository.class);

    private AdminMatchOddsService adminMatchOddsService;

    @BeforeEach
    void setUp() {
        adminMatchOddsService = new AdminMatchOddsService(
                matchRepository,
                matchOddsHistoryRepository,
                matchOddsDisplayPolicyRepository
        );
    }

    @Test
    void 야구_승패는_홈과_원정_배당률만_등록할_수_있다() {
        long matchId = 1L;
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match(SportCode.BASEBALL)));
        ManualMatchOddsRegisterRequest request = requestWithWinDrawLoss(null);

        assertThatCode(() -> adminMatchOddsService.registerOverseasManualOdds(matchId, request))
                .doesNotThrowAnyException();

        verify(matchOddsHistoryRepository).saveAll(anyList());
    }

    @Test
    void 축구_승무패는_무승부_배당률이_필수다() {
        long matchId = 1L;
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match(SportCode.FOOTBALL)));
        ManualMatchOddsRegisterRequest request = requestWithWinDrawLoss(null);

        assertThatThrownBy(() -> adminMatchOddsService.registerOverseasManualOdds(matchId, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_VALUE)
                );

        verify(matchOddsHistoryRepository, never()).saveAll(anyList());
    }

    @Test
    void 야구_승패에는_무승부_배당률을_등록할_수_없다() {
        long matchId = 1L;
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match(SportCode.BASEBALL)));
        ManualMatchOddsRegisterRequest request = requestWithWinDrawLoss(new BigDecimal("3.20"));

        assertThatThrownBy(() -> adminMatchOddsService.registerOverseasManualOdds(matchId, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_VALUE)
                );

        verify(matchOddsHistoryRepository, never()).saveAll(anyList());
    }

    @Test
    void 해외_수동_배당률_이력을_무효화한다() {
        long matchId = 10L;
        long historyId = 1L;
        MatchOddsHistory history = MatchOddsHistory.createWinDrawLoss(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                new BigDecimal("1.80"),
                new BigDecimal("3.20"),
                new BigDecimal("2.10"),
                Instant.parse("2026-10-28T10:00:00Z")
        );
        when(matchOddsHistoryRepository.findByIdAndMatchIdAndCategoryAndRegistrationType(
                historyId,
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL
        )).thenReturn(Optional.of(history));

        adminMatchOddsService.invalidateOverseasManualOddsHistory(matchId, historyId);

        assertThat(history.getStatus()).isEqualTo(OddsStatus.INVALIDATED);
    }

    @Test
    void 해외_수동_배당률_이력이_아니면_무효화할_수_없다() {
        long matchId = 10L;
        long historyId = 1L;
        when(matchOddsHistoryRepository.findByIdAndMatchIdAndCategoryAndRegistrationType(
                historyId,
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMatchOddsService.invalidateOverseasManualOddsHistory(matchId, historyId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_HISTORY_NOT_FOUND)
                );
    }

    @Test
    void 활성_해외_수동_배당률_이력을_모두_무효화한다() {
        long matchId = 10L;
        Match match = Match.builder().id(matchId).build();
        MatchOddsHistory winDrawLoss = MatchOddsHistory.createWinDrawLoss(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                new BigDecimal("1.80"),
                new BigDecimal("3.20"),
                new BigDecimal("2.10"),
                Instant.parse("2026-10-28T10:00:00Z")
        );
        MatchOddsHistory handicap = MatchOddsHistory.createHandicap(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                new BigDecimal("1.90"),
                new BigDecimal("-1.50"),
                new BigDecimal("1.95"),
                Instant.parse("2026-10-28T10:01:00Z")
        );

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchOddsHistoryRepository.findAllByMatchIdAndCategoryAndRegistrationTypeAndStatus(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                OddsStatus.ACTIVE
        )).thenReturn(List.of(winDrawLoss, handicap));

        adminMatchOddsService.invalidateAllOverseasManualOddsHistories(matchId);

        assertThat(winDrawLoss.getStatus()).isEqualTo(OddsStatus.INVALIDATED);
        assertThat(handicap.getStatus()).isEqualTo(OddsStatus.INVALIDATED);
    }

    @Test
    void 활성_해외_수동_배당률_이력이_없어도_전체_무효화는_정상_처리한다() {
        long matchId = 10L;
        Match match = Match.builder().id(matchId).build();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchOddsHistoryRepository.findAllByMatchIdAndCategoryAndRegistrationTypeAndStatus(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                OddsStatus.ACTIVE
        )).thenReturn(List.of());

        assertThatCode(() -> adminMatchOddsService.invalidateAllOverseasManualOddsHistories(matchId))
                .doesNotThrowAnyException();
    }

    @Test
    void 해외_배당률_노출_방식을_변경한다() {
        long matchId = 1L;
        Match match = Match.builder().id(matchId).build();
        MatchOddsDisplayPolicy policy = new MatchOddsDisplayPolicy(matchId, OddsCategory.OVERSEAS);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchOddsDisplayPolicyRepository.findByMatchIdAndCategory(matchId, OddsCategory.OVERSEAS))
                .thenReturn(Optional.of(policy));

        adminMatchOddsService.changeOverseasOddsDisplayMode(matchId, OddsRegistrationType.MANUAL);

        assertThat(policy.getDisplayedRegistrationType()).isEqualTo(OddsRegistrationType.MANUAL);
    }

    @Test
    void 해외_배당률_노출_정책이_없으면_예외가_발생한다() {
        long matchId = 1L;
        Match match = Match.builder().id(matchId).build();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchOddsDisplayPolicyRepository.findByMatchIdAndCategory(matchId, OddsCategory.OVERSEAS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                adminMatchOddsService.changeOverseasOddsDisplayMode(matchId, OddsRegistrationType.MANUAL)
        ).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_DISPLAY_POLICY_NOT_FOUND)
        );
    }

    @Test
    void 해외_배당률_노출_방식은_null로_변경할_수_없다() {
        long matchId = 1L;
        Match match = Match.builder().id(matchId).build();
        MatchOddsDisplayPolicy policy = new MatchOddsDisplayPolicy(matchId, OddsCategory.OVERSEAS);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchOddsDisplayPolicyRepository.findByMatchIdAndCategory(matchId, OddsCategory.OVERSEAS))
                .thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> adminMatchOddsService.changeOverseasOddsDisplayMode(matchId, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAMETER)
                );
        assertThat(policy.getDisplayedRegistrationType()).isEqualTo(OddsRegistrationType.AUTOMATIC);
    }

    @Test
    void 현재_노출_방식과_최신_수동_해외_배당률을_마켓별로_조회한다() {
        long matchId = 1L;
        Match match = Match.builder().id(matchId).build();
        MatchOddsDisplayPolicy policy = new MatchOddsDisplayPolicy(matchId, OddsCategory.OVERSEAS);
        MatchOddsHistory winDrawLoss = MatchOddsHistory.createWinDrawLoss(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                new BigDecimal("1.80"),
                new BigDecimal("3.20"),
                new BigDecimal("2.10"),
                Instant.parse("2026-10-28T10:00:00Z")
        );
        MatchOddsHistory handicap = MatchOddsHistory.createHandicap(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                new BigDecimal("1.90"),
                new BigDecimal("-1.50"),
                new BigDecimal("1.95"),
                Instant.parse("2026-10-28T10:01:00Z")
        );

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchOddsDisplayPolicyRepository.findByMatchIdAndCategory(matchId, OddsCategory.OVERSEAS))
                .thenReturn(Optional.of(policy));
        when(matchOddsHistoryRepository.findLatestOddsByMarket(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                OddsMarketType.WIN_DRAW_LOSS,
                OddsStatus.ACTIVE
        )).thenReturn(Optional.of(winDrawLoss));
        when(matchOddsHistoryRepository.findLatestOddsByMarket(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                OddsMarketType.HANDICAP,
                OddsStatus.ACTIVE
        )).thenReturn(Optional.of(handicap));
        when(matchOddsHistoryRepository.findLatestOddsByMarket(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                OddsMarketType.UNDER_OVER,
                OddsStatus.ACTIVE
        )).thenReturn(Optional.empty());

        OverseasOddsManagementOverviewResponse result = adminMatchOddsService
                .getOverseasOddsManagementOverview(matchId);

        assertThat(result.displayMode()).isEqualTo(OddsRegistrationType.AUTOMATIC);
        assertThat(result.latestManualMarketOdds())
                .extracting(OverseasOddsManagementOverviewResponse.LatestManualMarketOdds::marketType)
                .containsExactly(OddsMarketType.WIN_DRAW_LOSS, OddsMarketType.HANDICAP);
        assertThat(result.latestManualMarketOdds().get(0).publishedAt())
                .isEqualTo(Instant.parse("2026-10-28T10:00:00Z"));

        for (OddsMarketType marketType : OddsMarketType.getAllTypes()) {
            verify(matchOddsHistoryRepository).findLatestOddsByMarket(
                    matchId,
                    OddsCategory.OVERSEAS,
                    OddsRegistrationType.MANUAL,
                    marketType,
                    OddsStatus.ACTIVE
            );
        }
    }

    @Test
    void 선택한_마켓의_해외_수동_배당률_내역을_최신순으로_페이지당_10개씩_조회한다() {
        long matchId = 1L;
        Set<OddsMarketType> marketTypes = Set.of(OddsMarketType.WIN_DRAW_LOSS, OddsMarketType.HANDICAP);
        LocalDateTime matchStartAt = LocalDateTime.of(2026, 10, 28, 20, 0);
        Match match = Match.builder()
                .startAt(matchStartAt)
                .build();
        MatchOddsHistory beforeMatchStart = MatchOddsHistory.createWinDrawLoss(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                new BigDecimal("1.80"),
                new BigDecimal("3.20"),
                new BigDecimal("2.10"),
                Instant.parse("2026-10-28T10:59:00Z")
        );
        MatchOddsHistory afterMatchStart = MatchOddsHistory.createWinDrawLoss(
                matchId,
                OddsCategory.OVERSEAS,
                OddsRegistrationType.MANUAL,
                new BigDecimal("1.90"),
                new BigDecimal("3.10"),
                new BigDecimal("2.00"),
                Instant.parse("2026-10-28T11:01:00Z")
        );
        Page<MatchOddsHistory> histories = new PageImpl<>(
                List.of(afterMatchStart, beforeMatchStart),
                PageRequest.of(1, 10),
                12
        );

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchOddsHistoryRepository.findByMatchIdAndCategoryAndRegistrationTypeAndMarketTypeIn(
                eq(matchId),
                eq(OddsCategory.OVERSEAS),
                eq(OddsRegistrationType.MANUAL),
                eq(marketTypes),
                any(Pageable.class)
        )).thenReturn(histories);

        Page<ManualMatchOddsHistoryResponse> result = adminMatchOddsService
                .getOverseasManualOddsHistories(matchId, marketTypes, PageRequest.of(1, 50));

        assertThat(result.getContent())
                .extracting(ManualMatchOddsHistoryResponse::registeredBeforeMatchStart)
                .containsExactly(false, true);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matchOddsHistoryRepository).findByMatchIdAndCategoryAndRegistrationTypeAndMarketTypeIn(
                eq(matchId),
                eq(OddsCategory.OVERSEAS),
                eq(OddsRegistrationType.MANUAL),
                eq(marketTypes),
                pageableCaptor.capture()
        );

        Pageable appliedPageable = pageableCaptor.getValue();
        assertThat(appliedPageable.getPageNumber()).isEqualTo(1);
        assertThat(appliedPageable.getPageSize()).isEqualTo(10);
        assertThat(appliedPageable.getSort().getOrderFor("publishedAt").getDirection().isDescending()).isTrue();
        assertThat(appliedPageable.getSort().getOrderFor("id").getDirection().isDescending()).isTrue();
    }

    private Match match(SportCode sportCode) {
        Sport sport = Sport.builder()
                .sportCode(sportCode)
                .build();

        return Match.builder()
                .sport(sport)
                .build();
    }

    private ManualMatchOddsRegisterRequest requestWithWinDrawLoss(BigDecimal drawOdds) {
        ManualMatchOddsRegisterRequest.WinDrawLoss winDrawLoss = new ManualMatchOddsRegisterRequest.WinDrawLoss(
                new BigDecimal("1.80"),
                drawOdds,
                new BigDecimal("2.10")
        );

        return new ManualMatchOddsRegisterRequest(winDrawLoss, null, null);
    }
}
