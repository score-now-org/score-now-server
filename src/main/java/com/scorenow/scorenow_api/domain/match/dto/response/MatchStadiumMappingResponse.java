package com.scorenow.scorenow_api.domain.match.dto.response;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.TemporaryStadium;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "경기 경기장 매핑 응답")
public class MatchStadiumMappingResponse {

    @Schema(description = "경기장 매핑 유형", example = "REGISTERED")
    private MappingType mappingType;

    @Schema(description = "등록 경기장 ID", example = "101", nullable = true)
    private Long stadiumId;

    @Schema(description = "경기장명", example = "서울월드컵경기장", nullable = true)
    private String stadiumName;

    @Schema(description = "도시명", example = "서울", nullable = true)
    private String city;

    @Schema(description = "경기장 종목 ID", example = "1", nullable = true)
    private Long sportId;

    @Schema(description = "경기장 종목 코드", example = "FOOTBALL", nullable = true)
    private SportCode sportCode;

    @Schema(description = "경기장 종목명", example = "축구", nullable = true)
    private String sportName;

    @Schema(description = "경기장 데이터 원천", example = "MANUAL", nullable = true)
    private DataOrigin dataOrigin;

    @Schema(description = "등록 경기장 활성 여부", example = "true", nullable = true)
    private Boolean active;

    public static MatchStadiumMappingResponse registered(Stadium stadium, Sport sport) {
        return MatchStadiumMappingResponse.builder()
                .mappingType(MappingType.REGISTERED)
                .stadiumId(stadium.getId())
                .stadiumName(stadium.getName())
                .city(stadium.getCity())
                .sportId(stadium.getSportId())
                .sportCode(sport.getSportCode())
                .sportName(sport.resolveSportName())
                .dataOrigin(stadium.getDataOrigin())
                .active(stadium.isActive())
                .build();
    }

    public static MatchStadiumMappingResponse temporary(TemporaryStadium stadium) {
        return MatchStadiumMappingResponse.builder()
                .mappingType(MappingType.TEMPORARY)
                .stadiumName(stadium.getTemporaryStadiumName())
                .city(stadium.getTemporaryStadiumCity())
                .build();
    }

    public static MatchStadiumMappingResponse none() {
        return MatchStadiumMappingResponse.builder()
                .mappingType(MappingType.NONE)
                .build();
    }

    public enum MappingType {
        REGISTERED,
        TEMPORARY,
        NONE
    }
}
