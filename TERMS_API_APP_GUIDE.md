# 앱 약관 API 연동

`GET /api/v1/terms`는 로그인 없이 현재 게시된 약관을 조회합니다.

```json
{
  "success": true,
  "data": {
    "version": 1,
    "published": true,
    "content": "관리자가 등록한 약관 본문",
    "publishedAt": "2026-11-18T12:00:00"
  }
}
```

- `content`는 줄바꿈을 포함한 원문입니다. `version`은 게시된 개정본의 식별자입니다.
- 관리자가 최초 등록하기 전에는 `published`가 `false`, `version`이 `0`이며 본문은 `null`입니다. 앱은 이 상태와 통신 실패를 처리하고, 빈 약관을 정상 게시된 내용으로 표시하지 않아야 합니다.
- 앱의 약관 화면은 이 API를 호출하도록 변경해야 합니다. 기존 앱에 하드코딩된 약관이 있다면 실제 문구를 확인해 관리자가 최초 등록해야 합니다.
- 이 API는 약관 조회 기능이며, 사용자별 동의 기록이나 동의 강제 기능을 제공하지 않습니다.

서버 배포 전 `database/migrations/V20261118__terms_management.sql`을 대상 DB에 적용해야 합니다. 관리자 API는 관리자 인증이 필요합니다.
