package com.scorenow.scorenow_api.domain.player.entity;

import java.time.LocalDate;

import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "e_name")
	private String eName;

	@Column(name = "k_name")
	private String kName;

	private String cc;
	private LocalDate birthdate;
	private Integer height;

	@Builder.Default
	private boolean teaguk = false; // 태극전사

	public void updateFromApi(String eName, String cc, LocalDate birthdate, Integer height) {
		this.eName = eName;
		this.cc = cc;
		this.birthdate = birthdate;
		this.height = height;
	}

}