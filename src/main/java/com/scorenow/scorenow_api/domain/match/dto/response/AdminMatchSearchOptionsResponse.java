package com.scorenow.scorenow_api.domain.match.dto.response;

import java.util.Arrays;
import java.util.List;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "관리자 경기 검색 옵션 응답")
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

    @Schema(description = "종목 옵션 목록")
    private final List<SportOption> sports;

    @Schema(description = "리그 옵션 목록")
    private final List<LeagueOption> leagues;

    @Schema(description = "경기 상태 옵션 목록")
    private final List<StatusOption> statusOptions = STATUS_OPTIONS;

    @Schema(description = "앱 노출 여부 옵션 목록")
    private final List<BooleanOption> isActiveOptions = IS_ACTIVE_OPTIONS;

    @Schema(description = "자동/수동 경기 여부 옵션 목록")
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
    @Schema(description = "종목 옵션")
    public static class SportOption {
        @Schema(description = "종목 ID", example = "1")
        private final Long id;

        @Schema(description = "종목명", example = "축구")
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
    @Schema(description = "리그 옵션")
    public static class LeagueOption {
        @Schema(description = "리그 ID", example = "2")
        private final Long id;

        @Schema(description = "종목 ID", example = "1")
        private final Long sportId;

        @Schema(description = "리그명", example = "프리미어리그")
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
    @Schema(description = "경기 상태 옵션")
    public static class StatusOption {
        @Schema(description = "경기 상태 코드", example = "IN_PLAY")
        private final String value;

        @Schema(description = "경기 상태명", example = "진행중")
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
    @Schema(description = "boolean 옵션")
    public static class BooleanOption {
        @Schema(description = "옵션 값", example = "true")
        private final Boolean value;

        @Schema(description = "옵션 라벨", example = "사용중")
        private final String label;

        private BooleanOption(Boolean value, String label) {
            this.value = value;
            this.label = label;
        }
    }

}
