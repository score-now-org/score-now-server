package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.constant.MatchConstants;
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
import com.scorenow.scorenow_api.domain.odds.value.OddsOrigin;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminMatchOddsService {

    private static final int MANUAL_ODDS_HISTORY_PAGE_SIZE = 10;

    private final MatchRepository matchRepository;
    private final MatchOddsHistoryRepository matchOddsHistoryRepository;
    private final MatchOddsDisplayPolicyRepository matchOddsDisplayPolicyRepository;

    /**
     * 해외 배당률 관리 현황 조회
     */
    @Transactional(readOnly = true)
    public OverseasOddsManagementOverviewResponse getOverseasOddsManagementOverview(final Long matchId) {
        findMatchByMatchId(matchId);

        // 표기 정책을 먼저 조회한다.
        MatchOddsDisplayPolicy displayPolicy = matchOddsDisplayPolicyRepository
                .findByMatchIdAndCategory(matchId, OddsCategory.OVERSEAS)
                .orElseThrow(() -> new BusinessException(ErrorCode.ODDS_DISPLAY_POLICY_NOT_FOUND));

        // 현재 노출 방식과 관계없이 최신 유효 수동 배당률을 마켓별로 조회한다.
        List<MatchOddsHistory> latestManualHistories = OddsMarketType.getAllTypes().stream()
                .map(marketType -> matchOddsHistoryRepository.findLatestOddsByMarket(
                                matchId,
                                OddsCategory.OVERSEAS,
                                OddsRegistrationType.MANUAL,
                                marketType,
                                OddsStatus.ACTIVE
                        )
                )
                .flatMap(Optional::stream)
                .toList();

        return OverseasOddsManagementOverviewResponse.from(
                displayPolicy.getDisplayedRegistrationType(),
                latestManualHistories
        );
    }

    /**
     * 해외 수동 배당률 등록
     */
    @Transactional
    public void registerOverseasManualOdds(final Long matchId, final ManualMatchOddsRegisterRequest request) {
        // 경기 존재 여부 확인
        Match match = findMatchByMatchId(matchId);

        // 수동 배당률 등록 시, 모든 마켓타입이 비어있는 경우 정상 처리됨을 방지하기 위한 검증 로직
        validateAtLeastOneMarket(request);
        validateWinDrawLossBySport(match, request.winDrawLoss());

        List<MatchOddsHistory> histories = new ArrayList<>();
        Instant publishedAt = Instant.now();

        // 수동 승무패 배당률 등록
        if (request.winDrawLoss() != null) {
            MatchOddsHistory winDrawLoss = MatchOddsHistory.createWinDrawLoss(
                    matchId,
                    OddsCategory.OVERSEAS,
                    OddsOrigin.manual(),
                    request.winDrawLoss().homeOdds(),
                    request.winDrawLoss().drawOdds(),
                    request.winDrawLoss().awayOdds(),
                    publishedAt
            );

            histories.add(winDrawLoss);
        }

        // 수동 핸디캡 배당률 등록
        if (request.handicap() != null) {
            MatchOddsHistory handicap = MatchOddsHistory.createHandicap(
                    matchId,
                    OddsCategory.OVERSEAS,
                    OddsOrigin.manual(),
                    request.handicap().homeOdds(),
                    request.handicap().handicapLine(),
                    request.handicap().awayOdds(),
                    publishedAt
            );

            histories.add(handicap);
        }

        // 수동 언더오버 배당률 등록
        if (request.underOver() != null) {
            MatchOddsHistory underOver = MatchOddsHistory.createUnderOver(
                    matchId,
                    OddsCategory.OVERSEAS,
                    OddsOrigin.manual(),
                    request.underOver().underOdds(),
                    request.underOver().underOverLine(),
                    request.underOver().overOdds(),
                    publishedAt
            );

            histories.add(underOver);
        }

        matchOddsHistoryRepository.saveAll(histories);
    }

    /**
     * 해외 수동 배당률 삭제
     */
    @Transactional
    public void invalidateOverseasManualOddsHistory(final Long matchId, final Long historyId) {
        MatchOddsHistory matchOddsHistory = matchOddsHistoryRepository
                .findByIdAndMatchIdAndCategoryAndRegistrationType(
                        historyId,
                        matchId,
                        OddsCategory.OVERSEAS,
                        OddsRegistrationType.MANUAL
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.ODDS_HISTORY_NOT_FOUND));

        matchOddsHistory.invalidate();
    }

    /**
     * 해외 수동 배당률 전체 무효화
     */
    @Transactional
    public void invalidateAllOverseasManualOddsHistories(final Long matchId) {
        findMatchByMatchId(matchId);

        List<MatchOddsHistory> activeManualOddsHistories = matchOddsHistoryRepository
                .findAllByMatchIdAndCategoryAndRegistrationTypeAndStatus(
                        matchId,
                        OddsCategory.OVERSEAS,
                        OddsRegistrationType.MANUAL,
                        OddsStatus.ACTIVE
                );

        activeManualOddsHistories.forEach(MatchOddsHistory::invalidate);
    }

    /**
     * 해외 배당률 노출 방식 변경 (자동 배당률 노출 ↔︎ 수동 배당률 노출)
     */
    @Transactional
    public void changeOverseasOddsDisplayMode(final Long matchId, OddsRegistrationType displayedRegistrationType) {
        findMatchByMatchId(matchId);

        MatchOddsDisplayPolicy matchOddsDisplayPolicy = matchOddsDisplayPolicyRepository
                .findByMatchIdAndCategory(matchId, OddsCategory.OVERSEAS)
                .orElseThrow(() -> new BusinessException(ErrorCode.ODDS_DISPLAY_POLICY_NOT_FOUND));

        matchOddsDisplayPolicy.changeDisplayedRegistrationType(displayedRegistrationType);
    }


    /**
     * 해외 수동 배당률 내역 조회
     * TODO: 근데 경기 전에 등록된 배당률인지 경기 후에 등록된 배당률인지 어떻게 판단해야할까?
     *      - 현재 방식 : Match.startAt 과 비교한다.
     *      - 다른 방식 : Match 가 IN_PLAY 로 전환되는 시점은 실제 start_at 과 다를 수 있음.
     *                  따라서 전환되는 시점을 Match 에 별도 컬럼으로 저장하고, 해당값과 비교한다.
     *
     */
    @Transactional(readOnly = true)
    public Page<ManualMatchOddsHistoryResponse> getOverseasManualOddsHistories(
            final Long matchId,
            final Set<OddsMarketType> marketTypes,
            final Pageable pageable
    ) {
        Match match = findMatchByMatchId(matchId);

        Set<OddsMarketType> appliedMarketTypes = marketTypes == null || marketTypes.isEmpty()
                ? Set.copyOf(OddsMarketType.getAllTypes())
                : Set.copyOf(marketTypes);

        int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
        Pageable historyPageable = PageRequest.of(
                pageNumber,
                MANUAL_ODDS_HISTORY_PAGE_SIZE,
                Sort.by(
                        Sort.Order.desc("publishedAt"),
                        Sort.Order.desc("id")
                )
        );

        Instant matchStartAt = match.getStartAt() == null
                ? null
                : match.getStartAt().atZone(MatchConstants.SEOUL_TIME_ZONE_ID).toInstant();

        return matchOddsHistoryRepository
                .findByMatchIdAndCategoryAndRegistrationTypeAndMarketTypeIn(
                        matchId,
                        OddsCategory.OVERSEAS,
                        OddsRegistrationType.MANUAL,
                        appliedMarketTypes,
                        historyPageable
                )
                .map(history -> ManualMatchOddsHistoryResponse.from(history, matchStartAt));
    }

    private Match findMatchByMatchId(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    /**
     * 수동 등록 하려는 마켓 타입이 하나라도 있는지 검증
     */
    private void validateAtLeastOneMarket(ManualMatchOddsRegisterRequest request) {
        if (request.winDrawLoss() == null && request.handicap() == null && request.underOver() == null) {
            throw new BusinessException(ErrorCode.ODDS_MANUAL_MARKET_REQUIRED);
        }
    }

    /**
     * 종목별 승무패 배당률 입력값 검증
     * - 축구 : 승/무/패 배당률 모두 존재해야 함.
     * - 야구 : 승/패 배당률 존재해야 함.
     */
    private void validateWinDrawLossBySport(Match match, ManualMatchOddsRegisterRequest.WinDrawLoss winDrawLoss) {
        if (winDrawLoss == null) {
            return;
        }

        Sport sport = match.getSport();
        if (sport == null) {
            throw new BusinessException(ErrorCode.SPORT_NOT_FOUND);
        }

        SportCode sportCode = match.getSport().getSportCode();

        if (sportCode == SportCode.FOOTBALL && winDrawLoss.drawOdds() == null) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_VALUE, "축구 승무패의 무승부 배당률은 필수입니다.");
        }

        if (sportCode == SportCode.BASEBALL && winDrawLoss.drawOdds() != null) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_VALUE, "야구 승패에는 무승부 배당률을 등록할 수 없습니다.");
        }
    }
}
