package com.scorenow.scorenow_api.domain.admin.forbiddenword;

import java.time.LocalDateTime;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/forbidden-words")
@RequiredArgsConstructor
public class ForbiddenWordController {
    private final ForbiddenWordRepository words;

    public record Request(@NotBlank @Size(max = 100) String word, @Size(max = 100) String replacement) { }
    public record Response(Long id, String word, String replacement, LocalDateTime createdAt) {
        static Response from(ForbiddenWord word) {
            return new Response(word.getId(), word.getWord(), word.getReplacement(), word.getCreatedAt());
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<Page<Response>> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100 || keyword.length() > 100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return ApiResponse.success(words.findByWordContainingIgnoreCase(keyword.trim(), pageable).map(Response::from));
    }

    @PostMapping
    @Transactional
    public ApiResponse<Response> create(@Valid @RequestBody Request request) {
        String word = request.word().trim();
        if (words.existsByWord(word)) throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이미 등록된 금칙어입니다.");
        String replacement = request.replacement() == null || request.replacement().isBlank() ? "???" : request.replacement();
        try {
            return ApiResponse.success(Response.from(words.saveAndFlush(new ForbiddenWord(word, replacement))));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이미 등록된 금칙어입니다.");
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ForbiddenWord word = words.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        words.delete(word);
        return ApiResponse.success();
    }
}
