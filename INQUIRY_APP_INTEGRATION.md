# 앱 문의 답변 및 첨부 연동

로그인한 사용자의 `GET /api/v1/inquiries/{id}`와 `GET /api/v1/inquiries` 응답은 기존 `status`, `answer`, `answeredAt` 외에 선택적인 `answerHtml`을 제공한다. `status`가 `WAITING`이면 답변 대기, `ANSWERED`이면 답변 완료다. `answerHtml`이 없는 기존 답변은 `answer`를 일반 텍스트로 표시한다.

`answerHtml`이 있으면 서식 표시를 위해 HTML 렌더러를 사용한다. 서버는 단락·줄바꿈·굵게·기울임·밑줄·취소선·목록·안전한 링크만 허용한다. 앱에서도 스크립트 실행과 외부 리소스 로딩을 비활성화하고, 링크는 사용자 동작을 통해 안전하게 열어야 한다. HTML 태그를 일반 문장처럼 표시하지 않는다.

문의의 `hasImage`가 참이면 `GET /api/v1/inquiries/{id}/image`로 이미지를 조회하거나 `GET /api/v1/inquiries/{id}/image/download`로 다운로드한다. 둘 다 사용자 Bearer 토큰이 필요하며 본인이 작성한 문의만 접근할 수 있다. 응답은 JPEG 또는 PNG 바이너리다. 관리자의 답변에 첨부파일을 추가하는 기능은 포함되지 않는다.
