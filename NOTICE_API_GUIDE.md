# 공지사항·문의 API

모든 JSON 응답은 기존 `ApiResponse` 형식이다. 관리자 API는 관리자 로그인에서 발급된 토큰을 `Authorization` 헤더에 그대로 전달해야 한다. 공지 조회는 비로그인 가능하다. 목록은 Spring `Page` 형식이며 `page`는 0부터 시작하고 `size`는 1~100이다.

## 공지

- `GET /api/v1/notices?platform=IOS&tickerOnly=false&page=0&size=20`: 배포된 노출 공지 목록. `platform`은 `ALL`(기본), `IOS`, `ANDROID`; IOS/ANDROID 조회에는 `ALL` 공지도 포함된다. 티커만 조회하려면 `tickerOnly=true`. 순서는 `sortOrder` 오름차순이다.
- `GET /api/v1/notices/{id}?platform=IOS`: 배포된 공지 상세. 플랫폼을 지정하지 않으면 ALL 공지만 조회한다.
- `GET /admin/notices?platform=IOS&page=0&size=20`: 관리용 임시 목록. platform을 생략하면 전체다.
- `POST /admin/notices`: 임시 공지 생성. 새 공지는 맨 위에 배치된다.
- `PUT /admin/notices/{id}`: 임시 공지 수정. `sortOrder`를 지정하면 해당 순서로 이동한다. 생략하면 기존 순서를 유지한다.
- `DELETE /admin/notices/{id}`: 임시 삭제.
- `POST /admin/notices/publish`: 생성·수정·노출 여부·순서·삭제를 앱에 일괄 반영한다. 호출 전까지 기존 배포 내용은 유지된다.

등록·수정 요청 예시:
```json
{"title":"점검 안내","content":"점검 내용","ticker":true,"visible":true,"platform":"ALL","sortOrder":1,"url":null}
```
`url`은 선택 사항이며, 링크의 안전한 열기 방식은 앱에서 결정한다. `visible`은 다음 배포에서의 노출 여부이고 관리자 응답의 `published`는 현재 배포본의 노출 여부다.
