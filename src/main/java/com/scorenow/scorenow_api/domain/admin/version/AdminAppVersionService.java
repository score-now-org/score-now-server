package com.scorenow.scorenow_api.domain.admin.version;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAppVersionService {
    private final AppVersionReleaseRepository releases;

    public record Request(@NotBlank @Size(max = 40) String versionName,
                          @NotNull AppVersionRelease.Platform platform,
                          @NotNull LocalDate releaseDate,
                          @NotBlank @Size(max = 3000) String content) {
        public Request {
            versionName = versionName == null ? null : versionName.trim();
            content = content == null ? null : content.trim();
        }
    }
    public record Response(Long id, String versionName, AppVersionRelease.Platform platform,
                           LocalDate releaseDate, String content) {
        static Response from(AppVersionRelease release) {
            return new Response(release.getId(), release.getVersionName(), release.getPlatform(),
                    release.getReleaseDate(), release.getContent());
        }
    }

    @Transactional(readOnly = true)
    public Page<Response> list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("releaseDate"), Sort.Order.desc("id")));
        return releases.findAll(pageable).map(Response::from);
    }

    @Transactional
    public Response create(Request request) {
        return Response.from(releases.save(new AppVersionRelease(request.versionName(), request.platform(),
                request.releaseDate(), request.content())));
    }

    @Transactional
    public Response update(Long id, Request request) {
        AppVersionRelease release = find(id);
        release.update(request.versionName(), request.platform(), request.releaseDate(), request.content());
        return Response.from(release);
    }

    @Transactional
    public void delete(Long id) {
        releases.delete(find(id));
    }

    private AppVersionRelease find(Long id) {
        return releases.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
