package com.scorenow.scorenow_api.external.betsapi.mapper;

import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BetsEventSideMapperTest {

    private final BetsEventSideMapper mapper = new BetsEventSideMapper();

    @Test
    void 축구는_Bets의_home_away_순서를_그대로_사용한다() {
        BetsEventResponse.Team rawHome = team("raw-home");
        BetsEventResponse.Team rawAway = team("raw-away");
        BetsEventResponse.Event event = event(rawHome, rawAway, "2-1");

        BetsEventSideMapper.MappedSides result = mapper.map(SportCode.FOOTBALL, event);

        assertThat(result.homeTeam()).isSameAs(rawHome);
        assertThat(result.awayTeam()).isSameAs(rawAway);
        assertThat(result.homeScore()).isEqualTo(2);
        assertThat(result.awayScore()).isEqualTo(1);
    }

    @Test
    void 야구는_Bets의_home_away_순서를_반대로_사용한다() {
        BetsEventResponse.Team rawHome = team("actual-away");
        BetsEventResponse.Team rawAway = team("actual-home");
        BetsEventResponse.Event event = event(rawHome, rawAway, "4-1");

        BetsEventSideMapper.MappedSides result = mapper.map(SportCode.BASEBALL, event);

        assertThat(result.homeTeam()).isSameAs(rawAway);
        assertThat(result.awayTeam()).isSameAs(rawHome);
        assertThat(result.homeScore()).isEqualTo(1);
        assertThat(result.awayScore()).isEqualTo(4);
    }

    @Test
    void 점수가_없는_예정_경기는_팀_방향만_보정한다() {
        BetsEventResponse.Team rawHome = team("actual-away");
        BetsEventResponse.Team rawAway = team("actual-home");
        BetsEventResponse.Event event = event(rawHome, rawAway, null);

        BetsEventSideMapper.MappedSides result = mapper.map(SportCode.BASEBALL, event);

        assertThat(result.homeTeam()).isSameAs(rawAway);
        assertThat(result.awayTeam()).isSameAs(rawHome);
        assertThat(result.homeScore()).isNull();
        assertThat(result.awayScore()).isNull();
    }

    private BetsEventResponse.Event event(
            BetsEventResponse.Team home,
            BetsEventResponse.Team away,
            String score
    ) {
        BetsEventResponse.Event event = new BetsEventResponse.Event();
        event.setId("event-1");
        event.setHome(home);
        event.setAway(away);
        event.setSs(score);
        return event;
    }

    private BetsEventResponse.Team team(String id) {
        BetsEventResponse.Team team = new BetsEventResponse.Team();
        team.setId(id);
        return team;
    }
}
