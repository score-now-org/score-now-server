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

### 4. 서버 실행
```bash
./gradlew bootRun
```

### 5. API 문서 확인
```
http://localhost:8080/swagger-ui/index.html
```

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

