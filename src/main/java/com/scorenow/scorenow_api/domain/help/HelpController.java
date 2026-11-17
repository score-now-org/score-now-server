package com.scorenow.scorenow_api.domain.help;

import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class HelpController {
    private final HelpService help;
    private final FileStorage storage;

    public record UploadedImage(String key, String url) { }

    @GetMapping("/api/v1/help")
    public ApiResponse<HelpService.Snapshot> published() {
        return ApiResponse.success(help.get(true));
    }

    @GetMapping("/api/v1/admin/help")
    public ApiResponse<HelpService.Snapshot> adminList() {
        return ApiResponse.success(help.get(false));
    }

    @PutMapping("/api/v1/admin/help")
    public ApiResponse<HelpService.Snapshot> saveDraft(@Valid @RequestBody HelpService.PublishRequest request) {
        return ApiResponse.success(help.saveDraft(request));
    }

    @PostMapping("/api/v1/admin/help/publish")
    public ApiResponse<HelpService.Snapshot> publish(@Valid @RequestBody HelpService.VersionRequest request) {
        return ApiResponse.success(help.publish(request));
    }

    @PostMapping("/api/v1/admin/help/images")
    public ApiResponse<UploadedImage> upload(@RequestPart("image") MultipartFile image) {
        if (image.isEmpty()) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String key = storage.uploadFileAndReturnKey(image, "help");
        return ApiResponse.success(new UploadedImage(key, storage.getFileUrl(key)));
    }
}
