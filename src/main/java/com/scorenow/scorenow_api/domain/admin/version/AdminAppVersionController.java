package com.scorenow.scorenow_api.domain.admin.version;

import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/app-versions")
@RequiredArgsConstructor
public class AdminAppVersionController {
    private final AdminAppVersionService versions;

    @GetMapping
    public ApiResponse<Page<AdminAppVersionService.Response>> list(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(versions.list(page, size));
    }

    @PostMapping
    public ApiResponse<AdminAppVersionService.Response> create(@Valid @RequestBody AdminAppVersionService.Request request) {
        return ApiResponse.success(versions.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminAppVersionService.Response> update(@PathVariable Long id,
                                                              @Valid @RequestBody AdminAppVersionService.Request request) {
        return ApiResponse.success(versions.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        versions.delete(id);
        return ApiResponse.success();
    }
}
