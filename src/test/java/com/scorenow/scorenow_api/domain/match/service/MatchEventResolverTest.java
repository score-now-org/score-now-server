package com.scorenow.scorenow_api.domain.match.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchEventResolverTest {

    private final MatchEventResolver resolver = new MatchEventResolver();

    @Test
    void 승부차기_이벤트_문자열에서_팀별_득점수를_계산한다() {
        List<String> events = List.of(
                "PSG - Score 1st Penalty",
                "Chelsea - Score 1st Penalty",
                "PSG - Score 2nd Penalty",
                "Chelsea - Miss 2nd Penalty",
                "PSG - Score 3rd Penalty"
        );

        Integer psgScore = resolver.resolveShootOutScore(events, "PSG");
        Integer chelseaScore = resolver.resolveShootOutScore(events, "Chelsea");

        assertThat(psgScore).isEqualTo(3);
        assertThat(chelseaScore).isEqualTo(1);
    }

    @Test
    void 승부차기_이벤트_형식이_깨져있거나_null이어도_득점_계산이_실패하지_않는다() {
        List<String> events = new java.util.ArrayList<>();
        events.add(null);
        events.add(" ");
        events.add("Invalid Event");
        events.add("PSG - Score 1st Penalty");
        events.add("PSG - Miss 2nd Penalty");

        Integer score = resolver.resolveShootOutScore(events, "PSG");

        assertThat(score).isEqualTo(1);
    }
}
