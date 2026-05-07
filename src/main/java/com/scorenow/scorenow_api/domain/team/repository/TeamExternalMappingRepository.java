package com.scorenow.scorenow_api.domain.team.repository;

import java.util.Optional;

import com.scorenow.scorenow_api.external.common.ApiProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;

public interface TeamExternalMappingRepository extends JpaRepository<TeamExternalMapping, Long> {

    Optional<TeamExternalMapping> findByProviderAndApiTeamId(
            ApiProvider provider,
            String apiTeamId
    );

    @Query("""
            	select tem
            	from TeamExternalMapping tem
            	where tem.provider = :provider
            	  and tem.internalTeamId = :teamId
            """)
    Optional<TeamExternalMapping> findByProviderAndTeamId(
            @Param("provider") ApiProvider provider,
            @Param("teamId") Long teamId
    );
}
