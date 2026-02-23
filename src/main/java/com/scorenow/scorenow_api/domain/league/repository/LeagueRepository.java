package com.scorenow.scorenow_api.domain.league.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.league.entity.League;

public interface LeagueRepository extends JpaRepository<League, String> {

	@Modifying
	@Query(value = """
		INSERT IGNORE INTO leagues (id, sport_id, k_name, e_name, s_name, cc, is_active, created_at, updated_at)
		VALUES (:id, :sportId, :kName, :eName, :sName, :cc, true, NOW(), NOW())
		""", nativeQuery = true)
	void insertIgnore(
		@Param("id") String id,
		@Param("sportId") String sportId,
		@Param("kName") String kName,
		@Param("eName") String eName,
		@Param("sName") String sName,
		@Param("cc") String cc
	);

	@Query("SELECT l FROM League l WHERE " +
		"(:keyword IS NULL OR l.kName LIKE %:keyword% OR " +
		"l.eName LIKE %:keyword% OR l.id LIKE %:keyword%)")
	List<League> searchByKeyword(@Param("keyword") String keyword);
}
