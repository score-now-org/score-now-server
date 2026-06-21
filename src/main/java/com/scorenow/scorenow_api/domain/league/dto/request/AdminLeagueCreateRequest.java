package com.scorenow.scorenow_api.domain.league.dto.request;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminLeagueCreateRequest {

    @NotNull(message = "종목ID 는 필수입니다.")
    private Long sportId;

    @NotBlank
    private String eName;

    private String kName;

    @NotBlank
    private String sName;

    private TeamDisplayOrder teamDisplayOrder;

    @NotNull(message = "외부 연동 여부는 필수입니다.")
    private Boolean externalLinked;

    private DataOrigin dataOrigin;

    private String apiLeagueId;

    private Boolean syncEnabled;
}
