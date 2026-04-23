package com.scorenow.scorenow_api.domain.stadium.entity;

import jakarta.persistence.Embeddable;
import lombok.*;


/**
 * 관리자 중계 페이지에서 수동으로 입력한 경기장 정보 (일회성) <br>
 * Stadium 에서 관리하지 않고, Match 에서만 관리하여 해당 경기와의 라이프 사이클이 동일함.
 */

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class TemporaryStadium {
    private String temporaryStadiumName;
    private String temporaryStadiumCity;
}
