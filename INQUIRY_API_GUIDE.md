# 문의 API

모든 JSON 응답은 기존 `ApiResponse` 형식이다. 관리자 API는 로그인에서 발급된 토큰을 `Authorization` 헤더에 그대로 전달하고, 문의 API는 사용자 Bearer 토큰을 사용한다. 목록은 Spring `Page` 형식이며 `page`는 0부터 시작하고 `size`는 1~100이다.

현재 관리자 API는 기존 인증과 동일하게 `Authorization: <관리자 토큰 원문>`을 사용한다. 관리자 인증이 Bearer 방식으로 변경되면 이 API의 관리자 인증 테스트와 호출 방식도 함께 변경해야 한다. 관리자 문의 화면은 별도로 구현해야 한다.

## 문의

- `POST /api/v1/inquiries`: `{"title":"제목","content":"내용"}` 등록.
- `GET /api/v1/inquiries?page=0&size=20`: 본인 문의 목록.
- `GET /api/v1/inquiries/{id}`: 본인 문의 상세. `status`는 `WAITING` 또는 `ANSWERED`, 답변은 `answer`, `answeredAt`으로 제공한다.
- `PUT /api/v1/inquiries/{id}/image`: multipart `image` 파트로 이미지 한 장을 추가하거나 교체한다. JPEG/PNG, 최대 5 MiB. 문의 등록 후 별도 호출한다.
- `GET /api/v1/inquiries/{id}/image`: 본인 첨부 이미지 바이너리. 문의 응답의 `hasImage`로 첨부 여부를 확인한다.
- `GET /admin/inquiries`, `GET /admin/inquiries/{id}`, `GET /admin/inquiries/{id}/image`: 관리자 조회.
- `PUT /admin/inquiries/{id}/answer`: `{"content":"답변 내용"}`으로 답변을 등록·수정한다.

문의 이미지 데이터는 DB에 저장되므로 DB 용량 및 MySQL `max_allowed_packet`은 5 MiB 이상 이미지 저장에 맞춰 설정해야 한다. dev 프로필은 multipart 파일 5MB/요청 6MB로 설정했으며 다른 프로필도 동일하게 설정해야 한다.
