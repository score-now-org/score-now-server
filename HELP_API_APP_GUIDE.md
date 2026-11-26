# 앱 도움말 API 연동 안내

## 게시된 도움말 조회

`GET /api/v1/help`는 로그인 없이 호출할 수 있습니다. 관리자 화면에서 `등록하기`를 누른 내용만 반환하며, 배열 순서가 앱 캐러셀 표시 순서입니다. 데이터가 없으면 `items`는 빈 배열입니다.

```json
{
  "success": true,
  "data": {
    "version": 1,
    "unpublished": false,
    "items": [
      {
        "id": "9fb7d093-9a6c-4e73-af2d-b1f729bf659f",
        "content": "도움말 내용",
        "imageKey": "help/example.jpg",
        "imageUrl": "https://storage.googleapis.com/example/help/example.jpg",
        "createdAt": "2026-11-17T12:00:00"
      }
    ]
  },
  "timestamp": "2026-11-17T12:00:00"
}
```

`content`는 빈 문자열일 수 있고 `imageUrl`은 없을 수 있습니다. 둘 중 하나 이상이 제공됩니다. 이미지는 `imageUrl`을 사용하고 내부 저장소 키인 `imageKey`를 조합하지 마세요. `version`은 관리자 동시 편집 제어용이며 앱에서 순서로 사용하지 않습니다. `createdAt`은 서버의 로컬 날짜·시간입니다.

## 관리자 동작 참고

관리자는 `GET /api/v1/admin/help`로 임시 저장된 목록과 버전을 읽습니다. `PUT /api/v1/admin/help`에 `{ "version": 0, "items": [{ "id": "...", "content": "...", "imageKey": null }] }`를 보내면 임시 저장됩니다. 목록이 아직 없으면 버전은 `-1`입니다. `POST /api/v1/admin/help/publish`에 최신 `{ "version": 0 }`을 보내야 공개 목록에 반영됩니다. 다른 관리자가 먼저 수정하면 `409`가 반환되므로 다시 조회해야 합니다. 이미지는 `POST /api/v1/admin/help/images`에 multipart 필드 `image`로 전송합니다.

자동 마이그레이션이 선행 배포되면 도움말 기능 배포 시 서버가 필요한 테이블을 자동으로 준비합니다. 배포 전 실제 DB의 스키마와 권한을 확인하고, 자동 적용한 SQL을 수동으로 중복 실행하지 않습니다. 자세한 내용은 `database/MIGRATIONS.md`를 참고하세요.
