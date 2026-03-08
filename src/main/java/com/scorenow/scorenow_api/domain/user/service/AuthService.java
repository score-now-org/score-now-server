package com.scorenow.scorenow_api.domain.user.service;

import com.scorenow.scorenow_api.domain.user.dto.*;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.enums.UserStatus;
import com.scorenow.scorenow_api.domain.user.jwt.JwtProvider;
import com.scorenow.scorenow_api.domain.user.jwt.TokenPair;
import com.scorenow.scorenow_api.domain.user.jwt.TokenService;
import com.scorenow.scorenow_api.domain.user.redis.service.RedisService;
import com.scorenow.scorenow_api.domain.user.repository.RefreshTokenRepository;
import com.scorenow.scorenow_api.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final GeoService geoService;
    private final RedisService redisService;
    private final TokenService tokenService;
    /**
     * 로그인 API
     * @param request
     * @return
     */
    @Transactional
    public SocialLoginDto socialLogin(SocialLoginRequestDto request, HttpServletRequest httpRequest)  {

        //ip 가져오기
        String ip = extractTrustedClientIp(httpRequest);
        //ip -> 국가 변환
        String country = geoService.getCountryFromIp(ip);

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
                    .countryCode(country)
                    .nickname(request.getName()) // 닉네임 디폴트 : 소셜이름
                    .warningCnt(0)
                    .status(UserStatus.ACTIVE)
                    .build();

            userRepository.save(user);
            isNewUser = true;
        } else {
            user.updateLoginCountry(country);
        }
        //JWT 발급
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        //freshtoken redis 저장
        redisService.saveRefreshToken(user.getId(), refreshToken);

        UserDto userDto = UserDto.builder()
                .socialId(user.getSocialId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();

        return SocialLoginDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .user(userDto)
                .build();
    }

    /**
     *  닉네임 설정 API
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
                .socialId(user.getSocialId())
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
     * @param id
     * @return
     */
    public UserDto getProfile(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return UserDto.builder()
                .socialId(user.getSocialId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    /**
     * 사용자 탈퇴 API
     * @param id
     */
    public void deactivate(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        userRepository.delete(user);
    }


    /**
     * ip return 메서드
     */
    private String extractTrustedClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim(); // 첫 번째 IP만 사용
        }
        return request.getRemoteAddr();
    }

    /**
     * refresh토큰 갱신
     * @param refreshToken
     * @return
     */
    @Transactional
    public TokenResponseDto refresh(String refreshToken) {
        TokenPair tokens = tokenService.reRefreshToken(refreshToken);

        return TokenResponseDto.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build();
    }
}
