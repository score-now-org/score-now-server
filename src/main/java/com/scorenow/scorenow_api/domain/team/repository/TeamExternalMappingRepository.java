package com.scorenow.scorenow_api.domain.team.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;

public interface TeamExternalMappingRepository extends JpaRepository<TeamExternalMapping, Long> {

    Optional<TeamExternalMapping> findByProviderAndApiTeamId(
            DataOrigin provider,
            String apiTeamId
    );

    Optional<TeamExternalMapping> findByProviderAndInternalTeamId(
            DataOrigin provider,
            Long internalTeamId
    );

    List<TeamExternalMapping> findAllByProviderAndApiTeamIdIn(DataOrigin provider, Collection<String> apiTeamIds);
}
