package com.scorenow.scorenow_api.domain.user.validator;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

@Component
public class UserValidator {

    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9가-힣._]{2,8}$");

    public void validateNickname(String nickname) {

        if (nickname == null) {
            throw new BusinessException(ErrorCode.NICKNAME_REQUIRE);
        }

        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new BusinessException(ErrorCode.NICKNAME_PATTERN);
        }
    }
}