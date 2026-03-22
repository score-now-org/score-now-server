package com.scorenow.scorenow_api.domain.user.enums;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.scorenow.scorenow_api.domain.user.repository.UserRepository;

@Component
public class NicknameGenerator {

    @Autowired
    private UserRepository userRepository;

    private static final List<String> ADJECTIVES = List.of(
            "화창한", "재밌는", "신나는", "따뜻한", "시원한", "졸린", "배고픈", "용감한",
            "귀여운", "엉뚱한", "느긋한", "활발한", "수줍은", "당당한", "유쾌한", "엉성한",
            "깜찍한", "진지한", "허술한", "날쌘", "얌전한", "반짝이는", "포근한", "짓궂은",
            "씩씩한", "투덜대는", "설레는", "흐릿한", "산뜻한", "두근거리는");
    private static final List<String> NOUNS = List.of(
            "안경", "선풍기", "냉장고", "에어컨", "리모컨", "슬리퍼", "텀블러", "노트북",
            "마우스", "키보드", "충전기", "이어폰", "우산", "지갑", "시계", "백팩",
            "모자", "장갑", "목도리", "쿠션", "담요", "화분", "스탠드", "액자",
            "달력", "지우개", "형광펜", "포스트잇", "클립", "자석");

    private final Random random = new Random();

    public String generate() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        return adjective + noun;
    }

    public String generateUnique() {
        String base = generate(); // ex) "화창한안경"

        if (!userRepository.existsByNickname(base)) {
            return base; // 중복 없으면 그대로
        }

        // 중복이면 DB에서 같은 베이스 닉네임 개수 조회
        int count = userRepository.countByNicknameStartingWith(base);
        return base + (count + 1); // ex) "화창한안경2"
    }

}
