# ⚽ Score Now Server

> **실시간 축구 경기 데이터 수집 및 관리 REST API 서버**
> BetsAPI 연동을 통해 경기 일정, 실시간 스코어, 경기 상세 통계, 라인업 데이터를 수집·저장·제공합니다.

---

## 🛠️ Tech Stack

### 🧩 Backend
![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.3-6DB33F?style=flat&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java%2017-007396?style=flat&logo=openjdk&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring%20Batch-6DB33F?style=flat&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-0769AD?style=flat&logo=java&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black)

### 🗄️ Database
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat&logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-FF4438?style=flat&logo=redis&logoColor=white)

### ☁️ Infrastructure / DevOps
![Google Cloud](https://img.shields.io/badge/Google%20Cloud-4285F4?style=flat&logo=googlecloud&logoColor=white)
![Cloud Run](https://img.shields.io/badge/Cloud%20Run-4285F4?style=flat&logo=googlecloud&logoColor=white)
![Compute Engine](https://img.shields.io/badge/Compute%20Engine-4285F4?style=flat&logo=googlecloud&logoColor=white)
![MongoDB Atlas](https://img.shields.io/badge/MongoDB%20Atlas-47A248?style=flat&logo=mongodb&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat&logo=githubactions&logoColor=white)

---

## 📐 Architecture

```
com.scorenow.scorenow_api
├── domain
│   ├── match       # 경기 (엔티티, 서비스, 배치, 스케줄러, DTO)
│   ├── league      # 리그
│   ├── team        # 팀
│   ├── player      # 선수
│   └── sport       # 종목
├── external
│   └── betsapi     # BetsAPI 외부 연동 클라이언트
└── global
    ├── config      # 공통 설정 (DB, Redis, Batch, Security)
    ├── exception   # 예외처리 (ErrorCode, BusinessException, GlobalExceptionHandler)
    └── constant    # 공통 상수
```

**데이터 저장 전략**

| 저장소 | 용도 |
|--------|------|
| MySQL (JPA) | Match, League, Team, Sport, Player 관계형 데이터 |
| MongoDB | MatchDetailDocument (통계), MatchLineupDocument (라인업) |
| Redis | 리그·팀 데이터 캐시 |

---

## ⚙️ Initial Settings

### 사전 요구사항
- Java 17
- Docker & Docker Compose

### 1. 저장소 클론
```bash
git clone https://github.com/your-org/score-now-server.git
cd score-now-server
```

### 2. 인프라 실행 (MySQL, MongoDB, Redis)
```bash
docker-compose up -d
```

### 3. 환경 변수 설정
`src/main/resources/application.yml`에서 BetsAPI 토큰 및 DB 접속 정보를 설정합니다.

#### 로그인 설정

아래 값은 실행 환경의 환경변수 또는 Spring 설정으로 주입합니다. `.env` 파일만 생성하면 Spring Boot가 자동으로 읽지는 않습니다.

| 환경변수 | Spring 설정 | 설명 |
|---|---|---|
| `ADMIN_ID` | `admin.id` | 관리자 로그인 ID |
| `ADMIN_PASSWORD_HASH` | `admin.password-hash` | 새 관리자 비밀번호의 BCrypt 해시. 평문 비밀번호는 지원하지 않음 |
| `ADMIN_TOKEN_TTL_SECONDS` | `admin.token-ttl-seconds` | 관리자 토큰 유효기간(초). 기본값 28800(8시간) |
| `GOOGLE_CLIENT_IDS` | `auth.social.google-client-ids` | 허용할 Google ID 토큰의 Client ID. 여러 개면 쉼표로 구분 |
| `APPLE_CLIENT_IDS` | `auth.social.apple-client-ids` | 허용할 Apple ID 토큰의 Bundle ID/Services ID. 여러 개면 쉼표로 구분 |
| `KAKAO_APP_ID` | `auth.social.kakao-app-id` | Kakao Developers에 등록한 숫자 앱 ID. REST API 키와 다름 |

관리자 설정이 없으면 관리자 로그인은 503을 반환합니다. 소셜 로그인 설정은 사용하는 공급자의 값만 필요하며, 해당 값이 없으면 그 공급자의 로그인은 503을 반환합니다. 기존 소스에 있던 관리자 비밀번호는 재사용하지 않고 새 비밀번호의 해시를 설정합니다.

BCrypt 해시는 Spring Security의 `new BCryptPasswordEncoder().encode(새비밀번호)`로 생성합니다. 비밀번호는 UTF-8 기준 72바이트 이하로 설정하며, 해시와 원문을 저장소에 커밋하지 않습니다.

#### 앱 로그인 요청

`POST /api/v1/auth/social-login`은 공급자가 발급한 인증 토큰이 필요합니다.

```json
{"provider":"google","idToken":"<Google ID 토큰>"}
```

```json
{"provider":"apple","idToken":"<Apple ID 토큰>","name":"<표시 이름>"}
```

```json
{"provider":"kakao","accessToken":"<Kakao Access 토큰>","name":"<표시 이름>"}
```

Google/Apple 토큰은 공급자 공개키로 서명, 발급자, 대상 앱, 만료를 검증합니다. Kakao 토큰은 토큰 정보 API에서 대상 앱, 만료, 회원번호를 확인합니다. 회원 조회에 사용하는 `socialId`는 검증 결과에서 가져오며 요청 본문의 `socialId`는 사용하지 않습니다. 이름은 표시용 정보입니다.

`POST /api/v1/auth/register-nickname`은 앱 AccessToken을 Bearer 헤더로 보내고 본문에는 `{"nickname":"새닉네임"}`을 전달합니다. 기존 `provider/socialId` 필드를 보내도 변경 대상 선정에는 사용하지 않습니다. 닉네임은 1~30자이며 기존 60일 변경 제한을 유지합니다.

로그인·닉네임 변경·프로필 응답의 `user.id`/`data.id`에는 내부 회원 ID가 포함됩니다. RefreshToken은 로그인과 갱신 모두 14일이며, 계정당 한 개의 RefreshToken을 저장합니다. 탈퇴 시 RefreshToken을 삭제하고 비활성·정지·탈퇴 계정의 기존 AccessToken도 보호된 HTTP API에서 허용하지 않습니다.

#### 관리자 인증과 회원 API

- 로그인: `POST /api/v1/admin/login`, 본문 `{"id":"<관리자 ID>","password":"<새 비밀번호>"}`
- 로그인 성공 응답은 기존 `{"token":"<UUID 토큰>"}` 형식을 유지합니다.
- 관리자 API는 `Authorization: Bearer <UUID 토큰>`을 사용합니다. 기존 클라이언트의 `Authorization: <UUID 토큰>`도 지원합니다.
- 회원 목록: `GET /api/v1/admin/members?page=0&size=15`
- 회원 기본정보: `GET /api/v1/admin/members/{id}`
- 회원 로그인 이력: `GET /api/v1/admin/members/{id}/security`
- 로그아웃: `POST /api/v1/admin/logout`. Redis의 관리자 토큰을 삭제합니다.

기존 `/admin/members`, `/admin/members/{id}`, `/admin/members/{id}/security` 경로도 유지합니다. `/api/v1/admin/**`와 `/admin/**`는 로그인 API를 제외하고 관리자 토큰이 필요합니다. 앱 JWT는 관리자 API에 사용할 수 없습니다. 관리자 토큰은 Redis에 저장하므로 서버 인스턴스 간 공유되며 만료 후에는 다시 로그인해야 합니다. 존재하지 않는 회원의 기본정보·보안정보 조회는 모두 404를 반환합니다.

관리자 로그인 성공과 기존 `/api/v1/admin/test` 성공 응답은 프론트 호환을 위해 기존 형식을 유지합니다. 인증 실패 응답과 나머지 회원 API는 `ApiResponse` 형식을 사용합니다.

검증 기준: [Google ID 토큰 검증](https://developers.google.com/identity/sign-in/web/backend-auth), [Apple 사용자 검증](https://developer.apple.com/documentation/signinwithapple/verifying-a-user), [Kakao 토큰 정보 조회](https://developers.kakao.com/docs/ko/kakaologin/rest-api#access-token-info).

### 4. 서버 실행
```bash
./gradlew bootRun
```

### 5. API 문서 확인
```
http://localhost:8080/swagger-ui/index.html
```

---

## 커뮤니티 API 연동

- 소셜 로그인과 닉네임 변경 응답의 `data.user.id`, 프로필 응답의 `data.id`는 사용자 DB ID입니다. 게시글·댓글의 `authorId`와 비교할 수 있습니다.
- `GET /api/v1/community/posts/{postId}/comments`의 각 댓글에는 `liked`가 포함됩니다. 로그인 사용자의 추천 여부를 받으려면 `Authorization: Bearer {accessToken}` 헤더를 전달합니다. 비로그인 조회와 새 댓글 작성 응답의 `liked`는 `false`입니다.
- `GET /api/v1/community/posts`는 `sort=LATEST`(기본값) 또는 `sort=RECOMMENDED`를 받습니다. 추천순은 `likeCount` 내림차순이며, 추천수가 같으면 게시글 ID 내림차순으로 조회합니다.
- `boardType=POPULAR`는 기존 인기글 후보 범위를 사용하며, `sort=RECOMMENDED`를 지정하면 해당 후보를 추천순으로 조회합니다. 정렬을 생략하면 기존 인기 점수 정렬을 사용합니다.
- 게시글 목록의 `nextCursor`는 숫자 형식을 유지합니다. 다음 페이지 요청의 `cursor`에 그대로 전달합니다.
- 추천순 응답에는 마지막 게시글의 추천수 스냅샷인 `nextCursorLikeCount`도 포함됩니다. 다음 페이지에는 이 값을 `cursorLikeCount`로 함께 전달합니다. 서버가 커서 게시글을 다시 조회하지 않으며, 해당 게시글이 삭제되거나 추천수가 바뀌어도 이전 응답의 조회 경계를 사용합니다.
- `cursorLikeCount`는 `sort=RECOMMENDED`에서 양수인 `cursor`와 함께 사용하고, 값은 0 이상이어야 합니다. 정렬이나 게시판을 변경할 때는 두 커서 값을 모두 비웁니다.
- 기존처럼 숫자 `cursor`만 전달하는 추천순 요청도 지원합니다. 이 경우 커서 게시글의 현재 추천수를 조회하며, 게시글이 DB에서 완전히 삭제되었다면 빈 목록과 `hasNext: false`를 반환합니다.

추천순 응답의 커서 예시:

```json
{
  "nextCursor": 101,
  "nextCursorLikeCount": 12,
  "hasNext": true
}
```

```text
GET /api/v1/community/posts?boardType=SOCCER&sort=RECOMMENDED&size=20
GET /api/v1/community/posts?boardType=SOCCER&sort=RECOMMENDED&cursor=101&cursorLikeCount=12&size=20
```

추천순 조회를 위해 `community_posts`에 `(status, like_count, id)`와 `(category, status, like_count, id)` 복합 인덱스를 정의합니다.

---

## 💻 Coding Convention

### 🔀 Git Flow
1. Issue 생성
2. Issue 기준으로 Branch 생성
3. 개발 진행 → `add` → `commit` → `push`
4. Pull Request 생성
5. 팀원 Code Review 진행
6. Review 완료 후 Merge


### 🌿 Branch Naming Convention
> 형식: `{type}/#{이슈번호}`
```
feat/#24
fix/#31
```

### 💬 Commit Message Convention
> 형식: `{type}: 기능설명`
```
feat: 예정 경기 동기화 배치 구현
fix: 인플레이 감지 스케줄러 NPE 수정
```

### 📝 Issue Title Convention
> 형식: `{깃모지} {Type}: 기능설명`
```
✨ Feat: 예정 경기 배치 동기화
🐛 Fix: 경기 상세 동기화 오류
```

### 🔃 Pull Request Title Convention
> 형식: `{Type}: 기능 설명`
```
Feat: 예정 경기 동기화 배치 구현
Fix: 인플레이 감지 스케줄러 NPE 수정
```

