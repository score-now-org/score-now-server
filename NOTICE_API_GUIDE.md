# 공지사항·문의 API

모든 JSON 응답은 기존 `ApiResponse` 형식이다. 관리자 API는 관리자 로그인에서 발급된 토큰을 `Authorization` 헤더에 그대로 전달해야 한다. 공지 조회는 비로그인 가능하다. 목록은 Spring `Page` 형식이며 `page`는 0부터 시작하고 `size`는 1~100이다.

## 공지

- `GET /api/v1/notices?platform=IOS&tickerOnly=false&page=0&size=20`: 배포된 노출 공지 목록. `platform`은 `ALL`(기본), `IOS`, `ANDROID`; IOS/ANDROID 조회에는 `ALL` 공지도 포함된다. 티커만 조회하려면 `tickerOnly=true`. 순서는 `sortOrder` 오름차순이다.
- `GET /api/v1/notices/{id}?platform=IOS`: 배포된 공지 상세. 플랫폼을 지정하지 않으면 ALL 공지만 조회한다.
- `GET /api/v1/admin/notices?platform=IOS&page=0&size=20`: 관리용 임시 목록. platform을 생략하면 전체다. OS 필터는 선택한 OS에 직접 지정된 공지만 반환한다.
- `GET /api/v1/admin/notices/status`: 앱에 반영되지 않은 변경이 있는지 `pending`으로 반환한다.
- `POST /api/v1/admin/notices`: 임시 공지 생성. 새 공지는 맨 위에 배치된다.
- `PUT /api/v1/admin/notices/{id}`: 임시 공지 수정. `sortOrder`를 지정하면 해당 순서로 이동한다. 생략하면 기존 순서를 유지한다.
- `PUT /api/v1/admin/notices/{id}/order`: `{ "sortOrder": 1 }`로 임시 순서를 변경한다.
- `PATCH /api/v1/admin/notices/{id}/visibility`: `{ "visible": true }`로 임시 노출 설정을 변경한다.
- `DELETE /api/v1/admin/notices/{id}`: 임시 삭제.
- `POST /api/v1/admin/notices/publish`: 생성·수정·노출 여부·순서·삭제를 앱에 일괄 반영한다. 호출 전까지 기존 배포 내용은 유지된다.

기존 `/admin/notices` 경로도 같은 동작을 유지한다. `/api/v1/admin/notices` 경로는 관리자 프론트의 API 프록시를 통해 접근할 때 사용한다.

등록·수정 요청 예시:
```json
{"title":"점검 안내","content":"점검 내용","ticker":true,"visible":true,"platform":"ALL","sortOrder":1,"url":null}
```
`url`은 선택 사항이며, 링크의 안전한 열기 방식은 앱에서 결정한다. `visible`은 다음 배포에서의 노출 여부이고 관리자 응답의 `published`는 현재 배포본의 노출 여부다.

배포 전 기존 MySQL에 `database/migrations/V20261117__notice_management.sql`을 적용해야 한다. 공지 생성·수정·순서 변경·삭제·일괄 반영은 마이그레이션에서 생성하는 잠금 행으로 직렬화한다. 현재 자동 마이그레이션 실행기는 없다.
