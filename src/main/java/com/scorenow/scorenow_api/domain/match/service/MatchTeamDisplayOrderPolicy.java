package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class MatchTeamDisplayOrderPolicy {

    /**
     * Match 의 TeamDisplayOrder 값을 결정하며 아래와 같은 규칙을 따른다.
     * 1. 기본적으로 모든 Match 의 TeamDisplayOrder 는 HOME-AWAY
     * 2. 만약 리그에 할당된 TeamDisplayOrder 가 있다면 해당 리그에 속한 모든 경기는 리그의 TeamDisplayOrder 를 따른다.
     */
    public TeamDisplayOrder decide(League league) {
        if (league != null && league.hasTeamDisplayOrder()) {
            return league.getTeamDisplayOrder();
        }

        return TeamDisplayOrder.HOME_AWAY;
    }
}
