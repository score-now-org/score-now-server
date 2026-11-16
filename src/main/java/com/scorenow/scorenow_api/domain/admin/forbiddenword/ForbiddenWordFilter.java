package com.scorenow.scorenow_api.domain.admin.forbiddenword;

import java.util.Comparator;
import java.util.List;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForbiddenWordFilter {
    private final ForbiddenWordRepository repository;

    public String apply(String text, int maxLength) {
        if (text == null || text.isBlank()) return text;
        text = text.trim();
        if (text.length() > maxLength) return text;
        // ponytail: load the current rules for each write; consider a shared invalidated cache if traffic warrants it.
        List<ForbiddenWord> rules = repository.findAll().stream()
                .sorted(Comparator.comparingInt((ForbiddenWord rule) -> rule.getWord().length()).reversed())
                .toList();
        if (rules.isEmpty()) return text;
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < text.length();) {
            ForbiddenWord match = null;
            for (ForbiddenWord rule : rules) {
                if (text.startsWith(rule.getWord(), index)) { match = rule; break; }
            }
            if (match == null) result.append(text.charAt(index++));
            else { result.append(match.getReplacement()); index += match.getWord().length(); }
            if (result.length() > maxLength)
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "금칙어 변환 후 글자 수가 제한을 초과했습니다.");
        }
        return result.toString();
    }
}
