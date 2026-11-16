package com.scorenow.scorenow_api.domain.admin.forbiddenword;

import java.util.List;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForbiddenWordFilter {
    private final ForbiddenWordRepository repository;
    public record FilteredPair(String first, String second) { }

    public String apply(String text, int maxLength) {
        text = validateLength(text, maxLength);
        if (text == null || text.isBlank()) return text;
        return replace(text, maxLength, repository.findAllForFiltering());
    }

    public FilteredPair applyPair(String first, int firstLimit, String second, int secondLimit) {
        first = validateLength(first, firstLimit);
        second = validateLength(second, secondLimit);
        if ((first == null || first.isBlank()) && (second == null || second.isBlank())) return new FilteredPair(first, second);
        // ponytail: read rules once per write for cross-server consistency; use a shared invalidated cache if traffic warrants it.
        List<ForbiddenWord> rules = repository.findAllForFiltering();
        return new FilteredPair(replace(first, firstLimit, rules), replace(second, secondLimit, rules));
    }

    private String validateLength(String text, int maxLength) {
        if (text == null) return null;
        text = text.trim();
        if (text.length() > maxLength)
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "입력 글자 수가 제한을 초과했습니다.");
        return text;
    }

    private String replace(String text, int maxLength, List<ForbiddenWord> rules) {
        if (text == null || text.isBlank()) return text;
        if (rules.isEmpty()) return text;
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < text.length();) {
            ForbiddenWord match = null;
            for (ForbiddenWord rule : rules) {
                if (text.regionMatches(true, index, rule.getWord(), 0, rule.getWord().length())) { match = rule; break; }
            }
            if (match == null) result.append(text.charAt(index++));
            else { result.append(match.getReplacement()); index += match.getWord().length(); }
            if (result.length() > maxLength)
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "금칙어 변환 후 글자 수가 제한을 초과했습니다.");
        }
        return result.toString();
    }
}
