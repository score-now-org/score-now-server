# 앱 뉴스·알림 티커 API 연동 요청

`GET /api/v1/news-tickers?platform=IOS&page=0&size=20`은 로그인 없이 게시된 뉴스·알림만 반환합니다. Android는 `platform=ANDROID`를 사용합니다. 지정한 OS와 공통(`ALL`) 항목이 포함되며 `sortOrder` 오름차순입니다. 공통 항목만 보려면 `platform=ALL`을 사용합니다. `page`는 0부터 시작하고 `size`는 1~100입니다.

응답의 `data.content`는 배열이며 각 항목의 `title`, `content`, `titleColor`, `contentColor`, `url`, `platform`, `sortOrder`, `createdAt`을 사용합니다. 색상은 `#RRGGBB` 형식이고 `url`은 없을 수 있습니다. URL이 있으면 제목 클릭 시 해당 HTTP(S) 주소를 웹뷰로 여는 처리가 필요합니다. 빈 목록이면 티커를 숨기거나 빈 상태로 처리해 주세요.

관리자의 변경은 임시 저장 후 해당 메뉴에서 `등록하기`를 눌러야 앱에 보입니다. 일반 공지의 변경사항과 게시 순서는 별도로 관리합니다. 기존 `/api/v1/notices`의 일반 공지·공지 티커 응답 형식과 의미는 유지됩니다.

배포 전 `V20261117__notice_management.sql`을 먼저 적용하고, 이어서 `V20261117__news_ticker_fields.sql`을 적용해야 합니다. 현재 자동 마이그레이션 실행기는 없습니다.
