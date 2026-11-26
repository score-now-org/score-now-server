package com.scorenow.scorenow_api.domain.terms;

import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class TermsController {
    private final TermsService terms;

    @GetMapping("/api/v1/terms")
    public ApiResponse<TermsService.Snapshot> current() { return ApiResponse.success(terms.current()); }

    @GetMapping("/api/v1/admin/terms")
    public ApiResponse<TermsService.Snapshot> adminCurrent() { return ApiResponse.success(terms.current()); }

    @PostMapping("/api/v1/admin/terms/publish")
    public ApiResponse<TermsService.Snapshot> publish(@Valid @RequestBody TermsService.PublishRequest request) {
        return ApiResponse.success(terms.publish(request));
    }
}
