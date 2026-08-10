package com.scorenow.scorenow_api.domain.league.dto.response;

import java.util.Arrays;
import java.util.List;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "관리자 리그 관리 옵션 응답")
public class AdminLeagueSearchOptionsResponse {

    @Schema(description = "종목 옵션 목록")
    private final List<SportOption> sports;

    @Schema(description = "팀 표시 순서 옵션 목록")
    private final List<EnumOption> teamDisplayOrders;

    @Schema(description = "외부 데이터 원천 옵션 목록. MANUAL은 제외합니다.")
    private final List<EnumOption> dataOrigins;

    private AdminLeagueSearchOptionsResponse(List<SportOption> sports) {
        this.sports = sports;
        this.teamDisplayOrders = Arrays.stream(TeamDisplayOrder.values())
                .map(teamDisplayOrder -> new EnumOption(teamDisplayOrder.name(), teamDisplayOrder.getDescription()))
                .toList();
        this.dataOrigins = Arrays.stream(DataOrigin.values())
                .filter(dataOrigin -> dataOrigin != DataOrigin.MANUAL)
                .map(dataOrigin -> new EnumOption(dataOrigin.name(), dataOrigin.name()))
                .toList();
    }

    public static AdminLeagueSearchOptionsResponse of(List<Sport> sports) {
        return new AdminLeagueSearchOptionsResponse(
                sports.stream()
                        .map(SportOption::from)
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
            return new SportOption(sport.getId(), sport.resolveSportName());
        }
    }

    @Getter
    @Schema(description = "enum 옵션")
    public static class EnumOption {
        @Schema(description = "옵션 값", example = "BETS")
        private final String value;

        @Schema(description = "옵션 라벨", example = "BETS")
        private final String label;

        private EnumOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }
}
