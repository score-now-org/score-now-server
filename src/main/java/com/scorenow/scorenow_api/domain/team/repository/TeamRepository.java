package com.scorenow.scorenow_api.domain.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.team.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

	@Modifying
	@Query(value = """
		INSERT IGNORE INTO teams (id, sport_id, type, k_name, e_name, s_name, cc, image_url, is_active, created_at, updated_at)
		VALUES (:id, :sportId, :type, :kName, :eName, :sName, :cc, :imageUrl, true, NOW(), NOW())
		""", nativeQuery = true)
	void insertIgnore(
		@Param("id") String id,
		@Param("sportId") String sportId,
		@Param("type") String type,
		@Param("kName") String kName,
		@Param("eName") String eName,
		@Param("sName") String sName,
		@Param("cc") String cc,
		@Param("imageUrl") String imageUrl
	);
}