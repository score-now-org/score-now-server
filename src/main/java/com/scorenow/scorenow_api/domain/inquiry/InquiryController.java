package com.scorenow.scorenow_api.domain.inquiry;

import java.time.LocalDateTime;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;

@RestController @RequiredArgsConstructor
public class InquiryController {
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private final InquiryRepository inquiries;
    private final InquiryImageRepository images;
    private final UserReferenceService users;
    public record Request(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 10000) String content) {}
    public record Answer(@NotBlank @Size(max = 10000) String content) {}
    public record Response(Long id, String title, String content, String status, String answer,
                           LocalDateTime createdAt, LocalDateTime answeredAt, boolean hasImage) {
        static Response of(Inquiry i) { return new Response(i.getId(), i.getTitle(), i.getContent(),
                i.getAnsweredAt() == null ? "WAITING" : "ANSWERED", i.getAnswer(), i.getCreatedAt(), i.getAnsweredAt(), i.isHasImage()); }
    }
    @PostMapping("/api/v1/inquiries") @Transactional
    public ApiResponse<Response> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody Request request) {
        return ApiResponse.success(Response.of(inquiries.save(new Inquiry(users.requireActiveUser(userId), request.title(), request.content()))));
    }
    @GetMapping("/api/v1/inquiries") @Transactional(readOnly = true)
    public ApiResponse<Page<Response>> list(@AuthenticationPrincipal Long userId, @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        users.requireActiveUser(userId);
        return ApiResponse.success(inquiries.findByUser_Id(userId, pagination(page, size)).map(Response::of));
    }
    @GetMapping("/api/v1/inquiries/{id}") @Transactional(readOnly = true)
    public ApiResponse<Response> detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        users.requireActiveUser(userId);
        return ApiResponse.success(Response.of(inquiries.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))));
    }
    @PutMapping("/api/v1/inquiries/{id}/image") @Transactional
    public ApiResponse<Void> uploadImage(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                         @RequestPart("image") MultipartFile image) throws IOException {
        users.requireActiveUser(userId);
        Inquiry inquiry = inquiries.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (image.isEmpty() || image.getSize() > 5 * 1024 * 1024) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        byte[] bytes = image.getBytes();
        String type;
        if (bytes.length >= 3 && (bytes[0] & 255) == 0xff && (bytes[1] & 255) == 0xd8 && (bytes[2] & 255) == 0xff) type = "image/jpeg";
        else if (bytes.length >= 8 && java.util.Arrays.equals(bytes, 0, 8, PNG_SIGNATURE, 0, PNG_SIGNATURE.length)) type = "image/png";
        else throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        images.save(new InquiryImage(id, type, bytes));
        inquiry.attachImage();
        return ApiResponse.success();
    }
    @GetMapping("/api/v1/inquiries/{id}/image") @Transactional(readOnly = true)
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        users.requireActiveUser(userId);
        inquiries.findByIdAndUser_Id(id, userId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return imageResponse(id);
    }
    @GetMapping("/admin/inquiries/{id}/image") @Transactional(readOnly = true)
    public ResponseEntity<byte[]> adminImage(@PathVariable Long id) { return imageResponse(id); }
    @GetMapping("/admin/inquiries/{id}") @Transactional(readOnly = true)
    public ApiResponse<Response> adminDetail(@PathVariable Long id) {
        return ApiResponse.success(Response.of(inquiries.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))));
    }
    private ResponseEntity<byte[]> imageResponse(Long id) {
        InquiryImage image = images.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(image.getContentType()))
                .header("X-Content-Type-Options", "nosniff").body(image.getData());
    }
    @GetMapping("/admin/inquiries") @Transactional(readOnly = true)
    public ApiResponse<Page<Response>> adminList(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(inquiries.findAll(pagination(page, size)).map(Response::of));
    }
    @PutMapping("/admin/inquiries/{id}/answer") @Transactional
    public ApiResponse<Response> answer(@PathVariable Long id, @Valid @RequestBody Answer request) {
        Inquiry inquiry = inquiries.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        inquiry.answer(request.content());
        return ApiResponse.success(Response.of(inquiry));
    }
    private static Pageable pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }
}
