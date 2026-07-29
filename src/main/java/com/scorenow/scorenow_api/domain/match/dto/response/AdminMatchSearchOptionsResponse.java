package com.scorenow.scorenow_api.domain.match.dto.response;

import java.util.Arrays;
import java.util.List;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;

import lombok.Getter;

@Getter
public class AdminMatchSearchOptionsResponse {

    /* 경기 상태 옵션*/
    private static final List<StatusOption> STATUS_OPTIONS = Arrays.stream(MatchStatus.values())
            .map(StatusOption::from)
            .toList();

    /* 앱 노출 사용 여부 */
    private static final List<BooleanOption> IS_ACTIVE_OPTIONS = List.of(
            new BooleanOption(true, "사용중"),
            new BooleanOption(false, "미사용중"));

    /* 자동,수동 경기 여부 */
    private static final List<BooleanOption> IS_MANUAL_OPTIONS = List.of(
            new BooleanOption(false, "자동"),
            new BooleanOption(true, "수동"));

    private final List<SportOption> sports;
    private final List<LeagueOption> leagues;
    private final List<StatusOption> statusOptions = STATUS_OPTIONS;
    private final List<BooleanOption> isActiveOptions = IS_ACTIVE_OPTIONS;
    private final List<BooleanOption> isManualOptions = IS_MANUAL_OPTIONS;

    private AdminMatchSearchOptionsResponse(
            List<SportOption> sports,
            List<LeagueOption> leagues) {

        this.sports = sports;
        this.leagues = leagues;
    }

    public static AdminMatchSearchOptionsResponse of(List<Sport> sports, List<League> leagues) {
        return new AdminMatchSearchOptionsResponse(
                sports.stream()
                        .map(SportOption::from)
                        .toList(),
                leagues.stream()
                        .map(LeagueOption::from)
                        .toList()
        );
    }

    @Getter
    public static class SportOption {
        private final Long id;
        private final String label;

        private SportOption(Long id, String label) {
            this.id = id;
            this.label = label;
        }

        public static SportOption from(Sport sport) {
            return new SportOption(
                    sport.getId(),
                    sport.resolveSportName());
        }
    }

    @Getter
    public static class LeagueOption {
        private final Long id;
        private final Long sportId;
        private final String label;

        private LeagueOption(Long id, Long sportId, String label) {
            this.id = id;
            this.sportId = sportId;
            this.label = label;
        }

        public static LeagueOption from(League league) {
            return new LeagueOption(
                    league.getId(),
                    league.getSportId(),
                    league.resolveLeagueName());
        }
    }

    @Getter
    public static class StatusOption {
        private final String value;
        private final String label;

        private StatusOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public static StatusOption from(MatchStatus status) {
            return new StatusOption(status.name(), status.getDescription());
        }
    }

    @Getter
    public static class BooleanOption {
        private final Boolean value;
        private final String label;

        private BooleanOption(Boolean value, String label) {
            this.value = value;
            this.label = label;
        }
    }

}
