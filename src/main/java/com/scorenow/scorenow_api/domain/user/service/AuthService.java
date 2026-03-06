package com.scorenow.scorenow_api.domain.user.service;

import com.scorenow.scorenow_api.domain.user.dto.*;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * 로그인 API
     * @param request
     * @return
     */
    public SocialLoginDto socialLogin(SocialLoginRequestDto request)  {

        User user = userRepository
                .findByProviderAndSocialId(request.getProvider(), request.getSocialId())
                .orElse(null);

        boolean isNewUser = false;
        //신규 사용자 생성
        if (user == null) {
            user = User.builder()
                    .provider(request.getProvider())
                    .socialId(request.getSocialId())
                    .name(request.getName())
                    .build();
            userRepository.save(user);
            isNewUser = true;
        }
        //JWT 발급
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        UserDto userDto = UserDto.builder()
                .userId(user.getSocialId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();

        SocialLoginDto data = SocialLoginDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .user(userDto)
                .build();

        return SocialLoginDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .user(userDto)
                .build();
    }

    /**
     * 신규 사용자 닉네임 설정 API
     * @param request
     * @return
     */
    public RegisterNicknameDto registerNickname(RegisterNicknameRequestDto request) {

        User user = userRepository
                .findByProviderAndSocialId(request.getProvider(), request.getSocialId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. "));

        //닉네임 중복체크
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new RuntimeException("이미 사용중인 닉네임입니다.");
        }
        user.setNickname(request.getNickname());

        userRepository.save(user);

        UserDto userDto = UserDto.builder()
                .userId(user.getSocialId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();

        return RegisterNicknameDto.builder()
                .user(userDto)
                .newNickname(user.getNickname())
                .build();
    }

    /**
     * 로그아웃 API
     */
    public void logout() {
        // 현재는 서버 처리 없음 -
    }

    /**
     * 사용자 프로필 조회 API
     * @param accessToken
     * @return
     */
    public UserDto getProfile(String accessToken) {

        String socialId = jwtProvider.getSocialId(accessToken);
        User user = userRepository.findBySocialId(socialId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return UserDto.builder()
                .userId(user.getSocialId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    /**
     * 사용자 탈퇴 API
     * @param socialId
     */
    public void deactivate(String socialId) {

        User user = userRepository.findBySocialId(socialId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        userRepository.delete(user);
    }

}
