package com.scorenow.scorenow_api.domain.community.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostImage;

public interface CommunityPostImageRepository extends JpaRepository<CommunityPostImage, Long>, JpaSpecificationExecutor<CommunityPostImage> {

	@Override
	@EntityGraph(attributePaths = {"post", "post.author"})
	Page<CommunityPostImage> findAll(Specification<CommunityPostImage> specification, Pageable pageable);

	@Query("select image.post.id from CommunityPostImage image where image.id = :id")
	Optional<Long> findPostIdByImageId(@Param("id") Long id);

	@Modifying
	@Query("delete from CommunityPostImage image where image.id = :id and image.post.id = :postId")
	int deleteFromPost(@Param("id") Long id, @Param("postId") Long postId);

	List<CommunityPostImage> findByPost_IdOrderBySortOrderAsc(Long postId);
	@Query("""
		select image from CommunityPostImage image
		where image.post.id in :postIds and image.sortOrder = (
			select min(other.sortOrder) from CommunityPostImage other where other.post.id = image.post.id
		)
		order by image.post.id, image.id
		""")
	List<CommunityPostImage> findFirstImagesByPostIds(@Param("postIds") Collection<Long> postIds);

	@Query("select distinct image.post.id from CommunityPostImage image where image.post.id in :postIds")
	List<Long> findPostIdsWithImages(@Param("postIds") Collection<Long> postIds);

	void deleteByPost(CommunityPost post);
}
