package com.scorenow.scorenow_api.domain.player.entity;

import java.time.LocalDate;

import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
public class Player extends BaseEntity {
	@Id
	private String id; // = teamId:playerApiId 예) BETS117230:2740907
	private String leagueId; // 예) 프리미어리그: BETS94
	private String teamId; // 예) BETS117230 = BETS1 + teamApiId
	private String season;

	private String eName;
	private String kName;

	private String cc;
	private LocalDate birthdate;
	private String position;
	private Integer height;
	private String shirtnumber;

	private boolean teaguk = false; // 태극전사
	private boolean squadOn = true; // 스쿼드 on/off
}