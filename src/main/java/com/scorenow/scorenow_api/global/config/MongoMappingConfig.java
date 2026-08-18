package com.scorenow.scorenow_api.global.config;

import com.scorenow.scorenow_api.domain.league.document.standings.football.FootballStandingsData;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoManagedTypes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class MongoMappingConfig {

    // MatchDetailDocument 의 SportDetail 에 대해서 새로운 종목 추가 시 여기도 추가해야 함.
    @Bean
    public MongoManagedTypes mongoManagedTypes(ApplicationContext applicationContext) throws ClassNotFoundException {
        Set<Class<?>> managedTypes = new HashSet<>(
                new EntityScanner(applicationContext).scan(Document.class)
        );

        managedTypes.add(FootballDetail.class);
        managedTypes.add(FootballStandingsData.class);

        return MongoManagedTypes.fromIterable(managedTypes);
    }
}
