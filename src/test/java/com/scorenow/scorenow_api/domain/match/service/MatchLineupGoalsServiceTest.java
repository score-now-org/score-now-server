package com.scorenow.scorenow_api.domain.match.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse.ViewResult;

@ExtendWith(MockitoExtension.class)
class MatchLineupGoalsServiceTest {

	@Mock
	private BetsApiClient betsApiClient;

	@Mock
	private MatchLineupRepository matchLineupRepo;

	@InjectMocks
	private MatchLineupGoalsService service;

	private MatchLineupDocument document;
	private LineupSide home;
	private LineupSide away;

	@BeforeEach
	void setUp() {
		document = mock(MatchLineupDocument.class);
		home = mock(LineupSide.class);
		away = mock(LineupSide.class);
	}

	@Test
	void 득점_이벤트를_라인업_선수에게_반영한다() {
		LineupPlayer player = mock(LineupPlayer.class);
		BetsViewResponse response = mock(BetsViewResponse.class);
		ViewResult result = mock(ViewResult.class);
		BetsViewResponse.EventText event = 득점_이벤트(
			"Goal - Son Heung-min (Tottenham)"
		);

		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));
		when(document.getSportId()).thenReturn(37L);
		when(document.getApiMatchId()).thenReturn("api-match-1");

		when(betsApiClient.getEventView("api-match-1"))
			.thenReturn(response);
		when(response.getResults()).thenReturn(List.of(result));
		when(result.getEvents()).thenReturn(List.of(event));

		when(document.getHome()).thenReturn(home);
		when(document.getAway()).thenReturn(away);

		when(home.getStartingLineup()).thenReturn(List.of(player));
		when(player.getEName()).thenReturn("Son Heung-min");
		when(player.getGoals()).thenReturn(0);

		service.updateLineupGoals("1", "37");

		verify(player).setGoals(1);
		verify(matchLineupRepo).save(document);
	}

	@Test
	void 동일한_선수가_두_골을_득점하면_득점_수를_합산한다() {
		LineupPlayer player = mock(LineupPlayer.class);
		ViewResult matchDetail = mock(ViewResult.class);

		BetsViewResponse.EventText firstGoal = 득점_이벤트(
			"Goal - Son Heung-min (Tottenham)"
		);
		BetsViewResponse.EventText secondGoal = 득점_이벤트(
			"Goal - Son Heung-min (Tottenham)"
		);

		when(matchDetail.getEvents())
			.thenReturn(List.of(firstGoal, secondGoal));

		when(document.getHome()).thenReturn(home);
		when(home.getStartingLineup()).thenReturn(List.of(player));

		when(player.getEName()).thenReturn("Son Heung-min");
		when(player.getGoals()).thenReturn(0);

		service.updateLineupGoals(document, matchDetail);

		verify(player).setGoals(2);
		verify(matchLineupRepo).save(document);
	}

	@Test
	void 기존_득점_수와_같으면_저장하지_않는다() {
		LineupPlayer player = mock(LineupPlayer.class);
		ViewResult matchDetail = mock(ViewResult.class);
		BetsViewResponse.EventText event = 득점_이벤트(
			"Goal - Son Heung-min (Tottenham)"
		);

		when(matchDetail.getEvents()).thenReturn(List.of(event));

		when(document.getHome()).thenReturn(home);
		when(home.getStartingLineup()).thenReturn(List.of(player));

		when(player.getEName()).thenReturn("Son Heung-min");
		when(player.getGoals()).thenReturn(1);

		service.updateLineupGoals(document, matchDetail);

		verify(player, never()).setGoals(any());
		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void 득점_기록이_없는_선수의_기존_득점을_0으로_수정한다() {
		LineupPlayer player = mock(LineupPlayer.class);
		ViewResult matchDetail = mock(ViewResult.class);
		BetsViewResponse.EventText event = 득점_이벤트(
			"Goal - Other Player (Other Team)"
		);

		when(matchDetail.getEvents()).thenReturn(List.of(event));

		when(document.getHome()).thenReturn(home);
		when(home.getStartingLineup()).thenReturn(List.of(player));

		when(player.getEName()).thenReturn("Son Heung-min");
		when(player.getGoals()).thenReturn(1);

		service.updateLineupGoals(document, matchDetail);

		verify(player).setGoals(0);
		verify(matchLineupRepo).save(document);
	}

	@Test
	void 득점이_아닌_이벤트는_득점_계산에서_제외한다() {
		LineupPlayer player = mock(LineupPlayer.class);
		ViewResult matchDetail = mock(ViewResult.class);
		BetsViewResponse.EventText event = 득점_이벤트(
			"Yellow Card - Son Heung-min (Tottenham)"
		);

		when(matchDetail.getEvents()).thenReturn(List.of(event));

		when(document.getHome()).thenReturn(home);
		when(home.getStartingLineup()).thenReturn(List.of(player));

		when(player.getEName()).thenReturn("Son Heung-min");
		when(player.getGoals()).thenReturn(0);

		service.updateLineupGoals(document, matchDetail);

		verify(player, never()).setGoals(any());
		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void 경기_ID나_종목_ID가_숫자가_아니면_처리하지_않는다() {
		service.updateLineupGoals("invalid-match-id", "invalid-sport-id");

		verifyNoInteractions(
			matchLineupRepo,
			betsApiClient
		);
	}

	@Test
	void 경기_ID가_비어있으면_처리하지_않는다() {
		service.updateLineupGoals("   ", "37");

		verifyNoInteractions(
			matchLineupRepo,
			betsApiClient
		);
	}

	@Test
	void 라인업_문서가_없으면_API를_호출하지_않는다() {
		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.empty());

		service.updateLineupGoals("1", "37");

		verify(matchLineupRepo).findById(1L);
		verifyNoInteractions(betsApiClient);
		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void 요청한_종목_ID와_라인업의_종목_ID가_다르면_처리하지_않는다() {
		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));
		when(document.getSportId()).thenReturn(37L);

		service.updateLineupGoals("1", "99");

		verifyNoInteractions(betsApiClient);
		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void API_경기_ID가_없으면_API를_호출하지_않는다() {
		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));
		when(document.getSportId()).thenReturn(37L);
		when(document.getApiMatchId()).thenReturn("   ");

		service.updateLineupGoals("1", "37");

		verifyNoInteractions(betsApiClient);
		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void 경기_상세_응답이_비어있으면_저장하지_않는다() {
		BetsViewResponse response = mock(BetsViewResponse.class);

		when(matchLineupRepo.findById(1L))
			.thenReturn(Optional.of(document));
		when(document.getSportId()).thenReturn(37L);
		when(document.getApiMatchId()).thenReturn("api-match-1");

		when(betsApiClient.getEventView("api-match-1"))
			.thenReturn(response);
		when(response.getResults()).thenReturn(List.of());

		service.updateLineupGoals("1", "37");

		verify(matchLineupRepo, never()).save(any());
	}

	@Test
	void 경기_이벤트가_비어있으면_저장하지_않는다() {
		ViewResult matchDetail = mock(ViewResult.class);

		when(matchDetail.getEvents()).thenReturn(List.of());

		service.updateLineupGoals(document, matchDetail);

		verify(matchLineupRepo, never()).save(any());
	}

	private BetsViewResponse.EventText 득점_이벤트(String text) {
		BetsViewResponse.EventText event =
			mock(BetsViewResponse.EventText.class);

		when(event.getText()).thenReturn(text);

		return event;
	}
}