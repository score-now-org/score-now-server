# 문의 API

모든 JSON 응답은 기존 `ApiResponse` 형식이다. 관리자 API는 로그인에서 발급된 관리자 토큰을 `Authorization: Bearer <관리자 토큰>` 또는 기존 토큰 원문으로 전달한다. 사용자 문의 API는 사용자 Bearer 토큰을 사용한다. 목록은 Spring `Page` 형식이며 `page`는 0부터 시작하고 `size`는 1~100이다.

관리자 문의 화면은 별도로 구현해야 한다.

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

답변 등록은 기존 `content` 또는 서식용 `contentHtml`을 받는다. 둘 다 보내면 `contentHtml`을 기준으로 한다. 서식을 요청하면 서버가 허용된 태그만 남기고 일반 텍스트 `answer`와 서식 버전 `answerHtml`을 함께 저장한다. 앱은 `answerHtml`이 있으면 안전하게 렌더링하고, 없으면 `answer`를 일반 텍스트로 표시한다. 허용 태그는 p, div, br, strong, b, em, i, u, s, strike, ul, ol, li, a이며 링크는 http, https, mailto만 허용한다. 스크립트 실행이나 외부 리소스 로딩을 허용하지 않아야 한다. 답변 첨부 기능은 없다.

문의 첨부 데이터는 DB에 저장되므로 DB 용량 및 MySQL `max_allowed_packet`은 5 MiB 이상 파일 저장에 맞춰 설정해야 한다. dev 프로필은 multipart 파일 5MB/요청 6MB로 설정했으며 다른 프로필도 동일하게 설정해야 한다.

기존 DB에는 `inquiries.answer_html`, `inquiries.attachment_name` 컬럼이 필요하다. 자동 마이그레이션이 선행 배포되면 서버 시작 시 필요한 컬럼을 적용한다. 실제 DB의 스키마와 권한은 배포 전에 확인하고, 자동 적용한 SQL을 수동으로 중복 실행하지 않는다. 자세한 내용은 `database/MIGRATIONS.md`를 참고한다.

동시 업로드 MySQL 테스트는 기본적으로 건너뛴다. 포트 13307의 `scorenow_inquiry_test_only`는 테스트 중 모든 테이블을 생성·삭제하므로, 반드시 별도의 임시 MySQL 컨테이너에서만 `INQUIRY_MYSQL_DISPOSABLE_TEST=true`로 실행한다. 실제 개발·운영 DB에서는 실행하지 않는다.

```powershell
docker run --rm -d --name scorenow-inquiry-test -p 127.0.0.1:13307:3306 -e MYSQL_ROOT_PASSWORD=localtest -e MYSQL_DATABASE=scorenow_inquiry_test_only mysql:8.0
# 컨테이너가 준비된 후 실행
$env:INQUIRY_MYSQL_DISPOSABLE_TEST = 'true'
.\gradlew.bat test --tests '*InquiryMySqlConcurrencyTest'
Remove-Item Env:INQUIRY_MYSQL_DISPOSABLE_TEST
docker stop scorenow-inquiry-test
```
