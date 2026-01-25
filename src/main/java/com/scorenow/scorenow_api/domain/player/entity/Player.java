package com.scorenow.scorenow_api.domain.player.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "players")
@Getter @Setter
@NoArgsConstructor
public class Player extends BaseEntity{
    @Id
    private String id; // BETS + sportId + playerId

    private String kName;     // 선수 이름
    private String teamId;   // BETS + sportId + teamId (연관관계 없이 ID만 저장)
    private String cc; // 국가코드

    private String eName;     // 영문 이름
    private String backNumber; // 등번호

    private String birthDate; // 생년월일
    private Double height;    // 신장(cm)

    private boolean isInSquad;  // 팀 스쿼드 등록 여부
}