package com.scorenow.scorenow_api.domain.match;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.model.LineupSide;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupCommandService;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class LineupConcurrencyTest {

	@Autowired
	private MatchLineupCommandService matchLineupCommandService;

	@Autowired
	private MatchLineupRepository matchLineupRepository;

	@Test
	void 라인업_문서_저장_조회_테스트() {
		String matchId = "MANUAL1:MATCH1";

		matchLineupRepository.deleteById(matchId);

		MatchLineupDocument doc = MatchLineupDocument.create(matchId);

		LineupSide home = LineupSide.builder()
			.teamId("MANUAL1:TEAM1")
			.startingLineup(new ArrayList<>())
			.substitutes(new ArrayList<>())
			.build();

		doc.setHome(home);
		matchLineupRepository.save(doc);
		MatchLineupDocument saved = matchLineupRepository.findById(matchId).orElseThrow();

		assertThat(saved.getHome().getTeamId()).isEqualTo("MANUAL1:TEAM1");
		assertThat(saved.getHome().getStartingLineup()).isEmpty();
		assertThat(saved.getHome().getSubstitutes()).isEmpty();
	}

	@Test
	void 선수를_라인업에_반영할_수_있다() {
		String matchId = "MANUAL1:MATCH2";
		String teamId = "MANUAL1:TEAM1";
		String playerId = "MANUAL1:PLAYER1";

		matchLineupRepository.deleteById(matchId);

		MatchLineupDocument doc = MatchLineupDocument.create(matchId);

		LineupSide home = LineupSide.builder()
			.teamId(teamId)
			.startingLineup(new ArrayList<>())
			.substitutes(new ArrayList<>())
			.build();

		doc.setHome(home);
		matchLineupRepository.save(doc);

		LineupPlayer player = LineupPlayer.builder()
			.playerId(playerId)
			.kName("손흥민")
			.eName("Son Heung-min")
			.shirtNumber(7)
			.position("FW")
			.goals(0)
			.substitute(false)
			.temp(false)
			.build();

		matchLineupCommandService.addPlayerToLineup(matchId, teamId, player);
		MatchLineupDocument saved = matchLineupRepository.findById(matchId).orElseThrow();
		assertThat(saved.getHome().getStartingLineup()).hasSize(1);
		assertThat(saved.getHome().getSubstitutes()).isEmpty();

		LineupPlayer savedPlayer = saved.getHome().getStartingLineup().get(0);
		assertThat(savedPlayer.getPlayerId()).isEqualTo(playerId);
		assertThat(savedPlayer.getKName()).isEqualTo("손흥민");
		assertThat(savedPlayer.isSubstitute()).isFalse();
	}

	@Test
	void 같은_선수를_동시에_반영해도_중복되지_않아야_한다() throws InterruptedException {
		String matchId = "MANUAL1:MATCH3";
		String teamId = "MANUAL1:TEAM1";
		String playerId = "MANUAL1:PLAYER1";

		System.out.println("1. 테스트 시작");

		matchLineupRepository.deleteById(matchId);

		System.out.println("2. 기존 문서 삭제");

		MatchLineupDocument doc = MatchLineupDocument.create(matchId);

		LineupSide home = LineupSide.builder()
			.teamId(teamId)
			.startingLineup(new ArrayList<>())
			.substitutes(new ArrayList<>())
			.build();

		doc.setHome(home);
		matchLineupRepository.save(doc);
		System.out.println("3. 초기 문서 저장 완료");

		LineupPlayer player = LineupPlayer.builder()
			.playerId(playerId)
			.kName("손흥민")
			.eName("Son Heung-min")
			.shirtNumber(7)
			.position("FW")
			.goals(0)
			.substitute(false)
			.temp(false)
			.build();

		int threadCount = 30;
		ExecutorService executorService = Executors.newFixedThreadPool(10);

		CountDownLatch readyLatch = new CountDownLatch(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

		System.out.println("4. 동시성 테스트 작업 제출 시작");

		for (int i = 0; i < threadCount; i++) {
			final int index = i;
			executorService.submit(() -> {
				System.out.println("worker-" + index + " submit 완료");
				readyLatch.countDown();
				System.out.println("worker-" + index + " ready countDown");

				try {
					System.out.println("worker-" + index + " startLatch await 전");
					startLatch.await();
					System.out.println("worker-" + index + " service 호출 전");

					matchLineupCommandService.addPlayerToLineup(matchId, teamId, player);
					System.out.println("추가 성공");
				} catch (Exception e) {
					exceptions.add(e);
					e.printStackTrace();
					System.out.println(
						"worker-" + index + " 예외 발생: " + e.getClass().getName() + " / " + e.getMessage());
				} finally {
					doneLatch.countDown();
					System.out.println("worker-" + index + " done countDown");
				}
			});
		}

		System.out.println("5. 모든 worker submit 완료");

		boolean readyFinished = readyLatch.await(5, TimeUnit.SECONDS);
		System.out.println("6. readyFinished = " + readyFinished);

		startLatch.countDown();
		System.out.println("7. startLatch countDown 완료");

		boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
		System.out.println("8. finished = " + finished);

		MatchLineupDocument saved = matchLineupRepository.findById(matchId).orElseThrow();
		System.out.println("9. 저장 결과 조회 완료");

		long countInStarting = saved.getHome().getStartingLineup().stream()
			.filter(p -> playerId.equals(p.getPlayerId()))
			.count();

		long countInSubstitutes = saved.getHome().getSubstitutes().stream()
			.filter(p -> playerId.equals(p.getPlayerId()))
			.count();

		long totalCount = countInStarting + countInSubstitutes;

		System.out.println("10. countInStarting = " + countInStarting);
		System.out.println("11. countInSubstitutes = " + countInSubstitutes);
		System.out.println("12. totalCount = " + totalCount);
		System.out.println("13. exceptions size = " + exceptions.size());

		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.SECONDS);
		executorService.shutdownNow();
		
		assertThat(finished).isTrue();
		assertThat(totalCount).isEqualTo(1);
	}
}
