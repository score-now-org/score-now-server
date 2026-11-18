# 앱 문의 답변 및 첨부 연동

로그인한 사용자의 `GET /api/v1/inquiries/{id}`와 `GET /api/v1/inquiries` 응답은 기존 `status`, `answer`, `answeredAt` 외에 선택적인 `answerHtml`을 제공한다. `status`가 `WAITING`이면 답변 대기, `ANSWERED`이면 답변 완료다. `answerHtml`이 없는 기존 답변은 `answer`를 일반 텍스트로 표시한다.

`answerHtml`이 있으면 서식 표시를 위해 HTML 렌더러를 사용한다. 서버는 단락·줄바꿈·굵게·기울임·밑줄·취소선·목록·안전한 링크만 허용한다. 앱에서도 스크립트 실행과 외부 리소스 로딩을 비활성화하고, 링크는 사용자 동작을 통해 안전하게 열어야 한다. HTML 태그를 일반 문장처럼 표시하지 않는다.

문의 등록 후 `PUT /api/v1/inquiries/{id}/attachment`에 multipart `file` 파트로 파일 한 개를 등록하거나 교체할 수 있다. 최대 5 MiB이며 JPEG, PNG, PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, HWP/HWPX, TXT, CSV, ZIP을 지원한다. 기존 사진 업로드 API도 계속 사용할 수 있다.

문의의 `hasAttachment`가 참이면 `GET /api/v1/inquiries/{id}/attachment`로 다운로드하고 `attachmentName`을 표시한다. 기존 이미지 문의는 `attachmentName`이 없을 수 있으며 이 경우 서버에서 기본 이름을 제공한다. 다운로드는 사용자 Bearer 토큰이 필요하고 본인이 작성한 문의에만 접근할 수 있다. `hasImage`가 참인 경우 기존 `GET /api/v1/inquiries/{id}/image`로 이미지를 미리 볼 수 있다. 일반 파일 다운로드는 `application/octet-stream`으로 제공한다. 관리자의 답변에 첨부파일을 추가하는 기능은 포함되지 않는다.
