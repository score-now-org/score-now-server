package com.scorenow.scorenow_api.domain.terms;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class TermsService {
    private final TermsRevisionRepository revisions;
    private final TermsLockRepository lock;

    public record PublishRequest(@NotNull Long expectedVersion, @NotBlank @Size(max = 100000) String content) { }
    public record Snapshot(long version, boolean published, String content, LocalDateTime publishedAt) {
        static Snapshot from(TermsRevision revision) {
            return new Snapshot(revision.getId(), true, revision.getContent(), revision.getPublishedAt());
        }
    }

    @Transactional(readOnly = true)
    public Snapshot current() {
        return revisions.findFirstByOrderByIdDesc().map(Snapshot::from).orElseGet(() -> new Snapshot(0, false, null, null));
    }

    @Transactional
    public Snapshot publish(PublishRequest request) {
        if (request == null || request.expectedVersion() == null || request.content() == null
                || request.content().isBlank() || request.content().length() > 100000) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
        lock.lockForPublish().orElseThrow(() -> new IllegalStateException("약관 관리 DB 마이그레이션이 필요합니다."));
        Snapshot current = current();
        if (current.version() != request.expectedVersion()) throw new BusinessException(ErrorCode.TERMS_EDIT_CONFLICT);
        if (request.content().equals(current.content())) return current;
        return Snapshot.from(revisions.saveAndFlush(new TermsRevision(request.content())));
    }
}
