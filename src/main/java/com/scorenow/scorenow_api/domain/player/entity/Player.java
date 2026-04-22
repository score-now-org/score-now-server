package com.scorenow.scorenow_api.domain.player.entity;

import java.time.LocalDate;

import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player extends BaseEntity {
	@Id
	private String id;
	private String leagueId;
	private String teamId;
	private String season;

	private String eName;
	private String kName;

	private String cc;
	private LocalDate birthdate;
	private String position;
	private Integer height;
	private String shirtnumber;

	@Builder.Default
	private boolean teaguk = false; // 태극전사

	@Builder.Default
	private boolean squadOn = false; // 스쿼드 on/off
}