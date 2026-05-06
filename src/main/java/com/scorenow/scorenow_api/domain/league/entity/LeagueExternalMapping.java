package com.scorenow.scorenow_api.domain.league.entity;

import com.scorenow.scorenow_api.external.common.ExternalProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "league_external_mappings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_external_stadium_id_sport_id",   // TODO: 유니크키 제약조건 이름 변경
                        columnNames = {"provider", "external_league_id", "internal_league_id"})})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class LeagueExternalMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false)
    private ExternalProvider provider;

    @Column(name = "external_league_id", nullable = false)
    private String externalLeagueId;   // 외부API 를 통해 전달받은 리그 ID

    @Column(name = "internal_league_id", nullable = false)
    private Long internalLeagueId;     // 시스템 내부에서 채번한 리그 ID

    public static LeagueExternalMapping of(ExternalProvider provider, String externalLeagueId, Long internalLeagueId) {
        return new LeagueExternalMapping(null, provider, externalLeagueId, internalLeagueId);
    }
}
