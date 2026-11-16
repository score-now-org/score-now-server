package com.scorenow.scorenow_api.domain.admin.community;

import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

public interface AdminCommunityCommentRepository extends Repository<CommunityComment, Long>, JpaSpecificationExecutor<CommunityComment> {
    @Override
    @EntityGraph(attributePaths = {"author", "post"})
    Page<CommunityComment> findAll(Specification<CommunityComment> specification, Pageable pageable);
}
