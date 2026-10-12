package com.scorenow.scorenow_api.domain.match.repository.jpa;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.domain.match.repository.MatchSyncLeagueTargetQueryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MatchSyncLeagueTargetQueryRepositoryImpl implements MatchSyncLeagueTargetQueryRepository {
    private final EntityManager entityManager;

    @Override
    public List<MatchSyncLeagueTarget> findAllEnabledByDataOrigin(DataOrigin dataOrigin) {
        return entityManager.createQuery("""
                        select new com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget(
                            lem.dataOrigin,
                            sem.apiSportId,
                            lem.apiLeagueId,
                            s.id,
                            s.sportCode,
                            l.id,
                            case
                                when l.kName is not null and trim(l.kName) <> '' then l.kName
                                when l.eName is not null and trim(l.eName) <> '' then l.eName
                                when l.sName is not null and trim(l.sName) <> '' then l.sName
                                else ''
                            end
                        )
                        from LeagueExternalMapping lem
                        join League l on l.id = lem.internalLeagueId
                        join Sport s on s.id = l.sportId
                        join SportExternalMapping sem on (sem.provider=lem.dataOrigin and sem.internalSportId = s.id)
                        where lem.dataOrigin = :dataOrigin and lem.syncEnabled = true
                        order by sem.apiSportId, lem.apiLeagueId
                        """, MatchSyncLeagueTarget.class)
                .setParameter("dataOrigin", dataOrigin)
                .getResultList();
    }
}
