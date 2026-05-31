package com.scorenow.scorenow_api.domain.match.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;

class MatchTeamDisplayOrderPolicyTest {

    private final MatchTeamDisplayOrderPolicy policy = new MatchTeamDisplayOrderPolicy();

    @Test
    void 리그가_null이면_시스템_기본값인_HOME_AWAY를_반환한다() {
        TeamDisplayOrder result = policy.decide(null);

        assertThat(result).isEqualTo(TeamDisplayOrder.HOME_AWAY);
    }

    @Test
    void 리그에_표시_정책이_없으면_시스템_기본값인_HOME_AWAY를_반환한다() {
        League league = League.builder()
                .id(1L)
                .sportId(1L)
                .eName("Premier League")
                .build();

        TeamDisplayOrder result = policy.decide(league);

        assertThat(result).isEqualTo(TeamDisplayOrder.HOME_AWAY);
    }

    @Test
    void 리그에_표시_정책이_있으면_리그의_표시_정책을_반환한다() {
        League league = League.builder()
                .id(1L)
                .sportId(1L)
                .eName("Premier League")
                .teamDisplayOrder(TeamDisplayOrder.AWAY_HOME)
                .build();

        TeamDisplayOrder result = policy.decide(league);

        assertThat(result).isEqualTo(TeamDisplayOrder.AWAY_HOME);
    }
}
