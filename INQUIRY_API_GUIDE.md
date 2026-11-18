# 문의 API

모든 JSON 응답은 기존 `ApiResponse` 형식이다. 관리자 API는 로그인에서 발급된 토큰을 `Authorization` 헤더에 그대로 전달하고, 문의 API는 사용자 Bearer 토큰을 사용한다. 목록은 Spring `Page` 형식이며 `page`는 0부터 시작하고 `size`는 1~100이다.

현재 관리자 API는 기존 인증과 동일하게 `Authorization: <관리자 토큰 원문>`을 사용한다. 관리자 인증이 Bearer 방식으로 변경되면 이 API의 관리자 인증 테스트와 호출 방식도 함께 변경해야 한다. 관리자 문의 화면은 별도로 구현해야 한다.

## 문의

- `POST /api/v1/inquiries`: `{"title":"제목","content":"내용"}` 등록.
- `GET /api/v1/inquiries?page=0&size=20`: 본인 문의 목록.
- `GET /api/v1/inquiries/{id}`: 본인 문의 상세. `status`는 `WAITING` 또는 `ANSWERED`, 답변은 `answer`, `answeredAt`으로 제공한다.
- `PUT /api/v1/inquiries/{id}/image`: multipart `image` 파트로 이미지 한 장을 추가하거나 교체한다. JPEG/PNG, 최대 5 MiB. 문의 등록 후 별도 호출한다.
- `GET /api/v1/inquiries/{id}/image`: 본인 첨부 이미지 바이너리. 문의 응답의 `hasImage`로 첨부 여부를 확인한다.
- `GET /api/v1/inquiries/{id}/image/download`: 본인 첨부 이미지를 다운로드한다. 위 이미지 조회와 동일하게 사용자 Bearer 토큰이 필요하다.
- `PUT /api/v1/inquiries/{id}/attachment`: 문의 등록 후 multipart `file` 파트로 첨부파일 한 개를 등록하거나 기존 첨부를 교체한다. 최대 5 MiB. JPEG, PNG, PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, HWP/HWPX, TXT, CSV, ZIP을 지원한다. 확장자별 기본 파일 서명을 검사하며 TXT/CSV는 UTF-8이어야 한다.
- `GET /api/v1/inquiries/{id}/attachment`: 작성자 본인의 첨부파일을 다운로드한다. 응답의 `hasAttachment`와 `attachmentName`을 이용한다. 일반 파일은 미리보기가 아닌 다운로드로 제공한다.
- `GET /admin/inquiries`, `GET /admin/inquiries/{id}`, `GET /admin/inquiries/{id}/image`: 관리자 조회.
- `GET /admin/inquiries/{id}/attachment`: 관리자 첨부파일 다운로드.
- `PUT /admin/inquiries/{id}/answer`: `{"content":"답변 내용"}`으로 답변을 등록·수정한다.

관리자 API는 Firebase 프록시용 `/api/v1/admin/inquiries` 경로도 제공한다. 목록에는 `nickname`, `userId`, `from`, `to`, `status` 검색 조건을 조합할 수 있다. 날짜는 `yyyy-MM-dd`, 상태는 `WAITING` 또는 `ANSWERED`다. 관리자 목록·상세는 기존 문의 필드에 `userId`, `nickname`을 추가한다.

답변 등록은 기존 `content` 외에 선택적으로 `contentHtml`을 받는다. 서식을 요청하면 서버가 허용된 태그만 남기고 일반 텍스트 `answer`와 서식 버전 `answerHtml`을 함께 저장한다. 앱은 `answerHtml`이 있으면 안전하게 렌더링하고, 없으면 `answer`를 일반 텍스트로 표시한다. 허용 태그는 p, div, br, strong, b, em, i, u, s, strike, ul, ol, li, a이며 링크는 http, https, mailto만 허용한다. 스크립트 실행이나 외부 리소스 로딩을 허용하지 않아야 한다. 답변 첨부 기능은 없다.

문의 이미지 데이터는 DB에 저장되므로 DB 용량 및 MySQL `max_allowed_packet`은 5 MiB 이상 이미지 저장에 맞춰 설정해야 한다. dev 프로필은 multipart 파일 5MB/요청 6MB로 설정했으며 다른 프로필도 동일하게 설정해야 한다.

기존 DB는 `inquiries.answer_html`, `inquiries.attachment_name` 컬럼이 필요하다. dev의 Hibernate `ddl-auto: update`가 시작 시 추가할 수 있으나 실제 DB 권한·스키마를 배포 전에 확인해야 한다. 수동 적용 시 `database/migrations/V20261118__inquiry_answer_html.sql`과 `V20261118__inquiry_attachment_name.sql`은 해당 컬럼이 아직 없는 경우에만 한 번 실행한다.
