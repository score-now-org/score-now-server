package com.scorenow.scorenow_api.domain.player.entity;

import com.scorenow.scorenow_api.external.common.ApiProvider;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "player_external_mappings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerExternalMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private ApiProvider provider;

	@Column(name = "api_player_id", nullable = false)
	private String apiPlayerId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
		name = "internal_player_id",
		nullable = false,
		foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Player player;

	public PlayerExternalMapping(ApiProvider provider, String apiPlayerId, Player player) {
		this.provider = provider;
		this.apiPlayerId = apiPlayerId;
		this.player = player;
	}
}
