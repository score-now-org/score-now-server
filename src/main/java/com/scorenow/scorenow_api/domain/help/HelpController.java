package com.scorenow.scorenow_api.domain.help;

import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
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
        if (image.isEmpty() || !hasImageSignature(image)) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        String key = storage.uploadFileAndReturnKey(image, "help");
        return ApiResponse.success(new UploadedImage(key, storage.getFileUrl(key)));
    }

    private static boolean hasImageSignature(MultipartFile image) {
        try (InputStream input = image.getInputStream()) {
            byte[] header = input.readNBytes(12);
            return switch (image.getContentType() == null ? "" : image.getContentType()) {
                case "image/jpeg" -> startsWith(header, 0xff, 0xd8, 0xff);
                case "image/png" -> startsWith(header, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
                case "image/webp" -> startsWith(header, 0x52, 0x49, 0x46, 0x46)
                        && header.length >= 12 && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                default -> false;
            };
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private static boolean startsWith(byte[] actual, int... expected) {
        if (actual.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) if ((actual[i] & 0xff) != expected[i]) return false;
        return true;
    }
}
