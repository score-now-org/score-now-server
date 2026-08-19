package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchCandidateResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchesResponse;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatch;
import com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.FeaturedMatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.scorenow.scorenow_api.domain.match.entity.FeaturedMatchType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;


@ExtendWith(MockitoExtension.class)
class AdminFeaturedMatchServiceTest {

    @InjectMocks
    private AdminFeaturedMatchService service;

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private FeaturedMatchRepository featuredMatchRepository;

    @Captor
    private ArgumentCaptor<List<FeaturedMatch>> featuredMatchesCaptor;

    @Test
    void 경기_시작일_기준_displayDate가_설정되고_해당_타입의_다음_displayOrder로_저장된다() {

        Match match1 = generateFootballMatch(1L, 2026, 7, 23, 15, 50);
        Match match2 = generateFootballMatch(2L, 2026, 7, 23, 16, 50);
        Match match3 = generateFootballMatch(3L, 2026, 7, 23, 17, 50);

        List<Match> matches = List.of(match1, match2, match3);
        List<Long> matchIds = matches.stream().map(Match::getId).toList();

        given(matchRepository.findAllByIdsOrderByStartAt(matchIds)).willReturn(matches);
        given(featuredMatchRepository.findRegisteredMatchIds(matchIds)).willReturn(List.of());
        given(featuredMatchRepository.findMaxDisplayOrder(
                LocalDate.of(2026, 7, 23),
                PINNED
        )).willReturn(Optional.of(3));

        service.createFeaturedMatch(matchIds, PINNED);

        then(featuredMatchRepository).should().saveAll(featuredMatchesCaptor.capture());

        List<FeaturedMatch> featuredMatches = featuredMatchesCaptor.getValue();
        assertThat(featuredMatches)
                .extracting(FeaturedMatch::getMatchId, FeaturedMatch::getDisplayDate, FeaturedMatch::getDisplayOrder, FeaturedMatch::getType)
                .containsExactly(
                        tuple(1L, LocalDate.of(2026, 7, 23), 4, PINNED),
                        tuple(2L, LocalDate.of(2026, 7, 23), 5, PINNED),
                        tuple(3L, LocalDate.of(2026, 7, 23), 6, PINNED)
                );
    }

    @Test
    void 이미_등록된_경기를_등록하면_FEATURED_MATCH_ALREADY_EXISTS_예외가_발생한다() {
        Match match = generateFootballMatch(1L, 2026, 7, 23, 15, 50);
        List<Match> matches = List.of(match);
        List<Long> matchIds = matches.stream().map(Match::getId).toList();

        given(matchRepository.findAllByIdsOrderByStartAt(matchIds)).willReturn(matches);
        given(featuredMatchRepository.findRegisteredMatchIds(matchIds)).willReturn(List.of(1L));

        assertThatThrownBy(() -> service.createFeaturedMatch(matchIds, HOT_MATCH))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 등록_후보_조회시_이미_등록된_경기는_registerable_false이고_경기_예정_일시는_전체값으로_반환된다() {
        Match m1 = generateFootballMatch(1L, 2026, 7, 23, 15, 50);  // 상단고정,핫매치 이미 등록
        Match m2 = generateFootballMatch(2L, 2026, 7, 23, 20, 0);
        Match m3 = generateFootballMatch(3L, 2026, 7, 23, 18, 0);   // 상단고정,핫매치 이미 등록

        given(matchRepository.findMatchesByDateRange(
                LocalDate.of(2026, 7, 23).atStartOfDay(),
                LocalDate.of(2026, 7, 23).plusDays(1).atStartOfDay()
        )).willReturn(List.of(m1, m2, m3));

        given(featuredMatchRepository.findRegisteredMatchIds(List.of(1L, 2L, 3L)))
                .willReturn(List.of(1L, 3L));

        List<FeaturedMatchCandidateResponse> responseList = service.searchFeaturedMatchCandidates(LocalDate.of(2026, 7, 23));

        assertThat(responseList)
                .extracting(FeaturedMatchCandidateResponse::getMatchId,
                        FeaturedMatchCandidateResponse::isRegisterable,
                        FeaturedMatchCandidateResponse::getStartAt)
                .containsExactly(
                        tuple(1L, false, LocalDateTime.of(2026, 7, 23, 15, 50)),
                        tuple(2L, true, LocalDateTime.of(2026, 7, 23, 20, 0)),
                        tuple(3L, false, LocalDateTime.of(2026, 7, 23, 18, 0))
                );
    }

    @Test
    void 등록_경기_조회시_최상위_조회날짜와_경기별_예정_일시_전체값을_반환한다() {
        LocalDate displayDate = LocalDate.of(2026, 7, 23);
        Match match = generateFootballMatch(1L, 2026, 7, 23, 20, 30);
        FeaturedMatch featuredMatch = generateFeaturedMatch(1L, match.getId(), displayDate, PINNED, 1);
        ReflectionTestUtils.setField(featuredMatch, "match", match);

        given(featuredMatchRepository.findByDisplayDateWithMatch(displayDate))
                .willReturn(List.of(featuredMatch));

        FeaturedMatchesResponse response = service.searchFeaturedMatches(displayDate);

        assertThat(response.getDate()).isEqualTo("20260723");
        assertThat(response.getPinnedMatches())
                .singleElement()
                .extracting(item -> item.getStartAt())
                .isEqualTo(LocalDateTime.of(2026, 7, 23, 20, 30));
    }

    @Test
    void 전달받은_순서를_바탕으로_해당_타입의_순서를_재정렬한다() {

        FeaturedMatchType type = HOT_MATCH;
        LocalDate displayDate = LocalDate.of(2026, 7, 23);

        FeaturedMatch fm1 = generateFeaturedMatch(1L, 1L, displayDate, type, 1);
        FeaturedMatch fm2 = generateFeaturedMatch(2L, 2L, displayDate, type, 2);
        FeaturedMatch fm3 = generateFeaturedMatch(3L, 3L, displayDate, type, 3);
        FeaturedMatch fm4 = generateFeaturedMatch(4L, 4L, displayDate, type, 4);
        FeaturedMatch fm5 = generateFeaturedMatch(5L, 5L, displayDate, type, 5);

        given(featuredMatchRepository.findByDisplayDateAndTypeOrderByDisplayOrderAsc(displayDate, type))
                .willReturn(List.of(fm1, fm2, fm3, fm4, fm5));

        List<Long> reorderedIds = List.of(3L, 1L, 5L, 2L, 4L);
        service.updateFeaturedMatchesOrder(displayDate, type, reorderedIds);

        assertThat(fm1.getDisplayOrder()).isEqualTo(2);
        assertThat(fm2.getDisplayOrder()).isEqualTo(4);
        assertThat(fm3.getDisplayOrder()).isEqualTo(1);
        assertThat(fm4.getDisplayOrder()).isEqualTo(5);
        assertThat(fm5.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void 전달받은_순서에_중복되는_ID가_있는경우_예외가_발생한다() {
        FeaturedMatchType type = HOT_MATCH;
        LocalDate displayDate = LocalDate.of(2026, 7, 23);

        FeaturedMatch fm1 = generateFeaturedMatch(1L, 1L, displayDate, type, 1);
        FeaturedMatch fm2 = generateFeaturedMatch(2L, 2L, displayDate, type, 2);
        FeaturedMatch fm3 = generateFeaturedMatch(3L, 3L, displayDate, type, 3);
        FeaturedMatch fm4 = generateFeaturedMatch(4L, 4L, displayDate, type, 4);
        FeaturedMatch fm5 = generateFeaturedMatch(5L, 5L, displayDate, type, 5);

        given(featuredMatchRepository.findByDisplayDateAndTypeOrderByDisplayOrderAsc(displayDate, type))
                .willReturn(List.of(fm1, fm2, fm3, fm4, fm5));

        List<Long> reorderedIds = List.of(3L, 1L, 4L, 2L, 4L);  // 중복되는 ID:4

        assertThatThrownBy(() -> service.updateFeaturedMatchesOrder(displayDate, type, reorderedIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("중복인 상단고정/핫매치 정보가 존재합니다.");
    }

    @Test
    void 전달받은_순서의_개수와_현재_관리되고_있는_개수가_불일치_하는_경우_예외가_발생한다() {
        FeaturedMatchType type = HOT_MATCH;
        LocalDate displayDate = LocalDate.of(2026, 7, 23);

        // 관리되고 있는 개수 : 5개
        FeaturedMatch fm1 = generateFeaturedMatch(1L, 1L, displayDate, type, 1);
        FeaturedMatch fm2 = generateFeaturedMatch(2L, 2L, displayDate, type, 2);
        FeaturedMatch fm3 = generateFeaturedMatch(3L, 3L, displayDate, type, 3);
        FeaturedMatch fm4 = generateFeaturedMatch(4L, 4L, displayDate, type, 4);
        FeaturedMatch fm5 = generateFeaturedMatch(5L, 5L, displayDate, type, 5);

        given(featuredMatchRepository.findByDisplayDateAndTypeOrderByDisplayOrderAsc(displayDate, type))
                .willReturn(List.of(fm1, fm2, fm3, fm4, fm5));

        // 전달받은 순서의 개수 : 4개
        List<Long> reorderedIds = List.of(3L, 1L, 2L, 4L);

        assertThatThrownBy(() -> service.updateFeaturedMatchesOrder(displayDate, type, reorderedIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("순서 변경 대상 개수가 일치하지 않습니다.");
    }

    @Test
    void 전달받은_순서의_ID와_현재_관리되고_있는_ID가_불일치_하는_경우_예외가_발생한다() {
        FeaturedMatchType type = HOT_MATCH;
        LocalDate displayDate = LocalDate.of(2026, 7, 23);

        FeaturedMatch fm1 = generateFeaturedMatch(1L, 1L, displayDate, type, 1);
        FeaturedMatch fm2 = generateFeaturedMatch(2L, 2L, displayDate, type, 2);
        FeaturedMatch fm3 = generateFeaturedMatch(3L, 3L, displayDate, type, 3);
        FeaturedMatch fm4 = generateFeaturedMatch(4L, 4L, displayDate, type, 4);
        FeaturedMatch fm5 = generateFeaturedMatch(5L, 5L, displayDate, type, 5);

        given(featuredMatchRepository.findByDisplayDateAndTypeOrderByDisplayOrderAsc(displayDate, type))
                .willReturn(List.of(fm1, fm2, fm3, fm4, fm5));

        List<Long> reorderedIds = List.of(3L, 1L, 999L, 2L, 4L);  // 불일치하는 ID : 999L

        assertThatThrownBy(() -> service.updateFeaturedMatchesOrder(displayDate, type, reorderedIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("순서 변경 대상이 올바르지 않습니다.");
    }

    @Test
    void 삭제시_해당_타입의_순서가_재정렬_된다() {
        FeaturedMatchType type = HOT_MATCH;
        LocalDate displayDate = LocalDate.of(2026, 7, 23);

        FeaturedMatch fm1 = generateFeaturedMatch(1L, 1L, displayDate, type, 1);
        FeaturedMatch fm2 = generateFeaturedMatch(2L, 2L, displayDate, type, 2);
        FeaturedMatch fm3 = generateFeaturedMatch(3L, 3L, displayDate, type, 3);
        FeaturedMatch fm4 = generateFeaturedMatch(4L, 4L, displayDate, type, 4);
        FeaturedMatch fm5 = generateFeaturedMatch(5L, 5L, displayDate, type, 5);

        given(featuredMatchRepository.findById(fm1.getId()))
                .willReturn(Optional.of(fm1));

        given(featuredMatchRepository.findMatchesAfterDisplayOrder(displayDate, type, fm1.getDisplayOrder()))
                .willReturn(List.of(fm2, fm3, fm4, fm5));

        service.deleteFeaturedMatch(fm1.getId());   // displayOrder:1 인 핫매치 삭제

        assertThat(fm2.getDisplayOrder()).isEqualTo(1); // 기존:2 -> 삭제후:1
        assertThat(fm3.getDisplayOrder()).isEqualTo(2); // 기존:3 -> 삭제후:2
        assertThat(fm4.getDisplayOrder()).isEqualTo(3); // 기존:4 -> 삭제후:3
        assertThat(fm5.getDisplayOrder()).isEqualTo(4); // 기존:5 -> 삭제후:4
    }

    @Test
    void 존재하지_않는_상단고정_핫매치_설정을_삭제하면_예외가_발생한다() {
        Long featuredMatchId = 1L;

        given(featuredMatchRepository.findById(featuredMatchId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteFeaturedMatch(featuredMatchId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void matchId로_삭제시_설정이_있으면_삭제하고_후순위_순서를_재정렬한다() {
        FeaturedMatchType type = PINNED;
        LocalDate displayDate = LocalDate.of(2026, 7, 23);

        FeaturedMatch fm1 = generateFeaturedMatch(1L, 1L, displayDate, type, 1);
        FeaturedMatch fm2 = generateFeaturedMatch(2L, 2L, displayDate, type, 2);
        FeaturedMatch fm3 = generateFeaturedMatch(3L, 3L, displayDate, type, 3);

        given(featuredMatchRepository.findByMatchId(fm1.getMatchId()))
                .willReturn(Optional.of(fm1));
        given(featuredMatchRepository.findMatchesAfterDisplayOrder(displayDate, type, fm1.getDisplayOrder()))
                .willReturn(List.of(fm2, fm3));

        service.deleteFeaturedMatchByMatchId(fm1.getMatchId());

        then(featuredMatchRepository).should().delete(fm1);
        assertThat(fm2.getDisplayOrder()).isEqualTo(1);
        assertThat(fm3.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void matchId로_삭제시_설정이_없으면_예외없이_종료한다() {
        Long matchId = 1L;

        given(featuredMatchRepository.findByMatchId(matchId)).willReturn(Optional.empty());

        service.deleteFeaturedMatchByMatchId(matchId);

        then(featuredMatchRepository).should().findByMatchId(matchId);
    }

    private FeaturedMatch generateFeaturedMatch(
            Long id,
            Long matchId,
            LocalDate displayDate,
            FeaturedMatchType type,
            Integer displayOrder) {

        FeaturedMatch featuredMatch = new FeaturedMatch(matchId, displayDate, type, displayOrder);
        ReflectionTestUtils.setField(featuredMatch, "id", id);

        return featuredMatch;
    }

    private Match generateFootballMatch(Long id, int year, int month, int day, int hour, int minute) {
        return Match.builder()
                .id(id)
                .sport(Sport.builder().id(1L).sportCode(SportCode.FOOTBALL).kName("축구").eName("football").build())
                .sportId(1L)
                .startAt(LocalDateTime.of(year, month, day, hour, minute))
                .league(League.builder()
                        .kName("리그" + id)
                        .build())
                .homeTeam(Team.builder()
                        .kName("홈팀" + id)
                        .build())
                .awayTeam(Team.builder()
                        .kName("원정팀" + id)
                        .build())
                .build();
    }
}
