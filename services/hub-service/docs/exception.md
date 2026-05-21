# 🚨 Hub 서비스 예외 처리 구현 (Step 4)

## 1. 구현 내용
- **HubErrorCode**: Hub 서비스 전용 에러 코드(`HUB-xxx`)를 정의했습니다. HTTP 상태 코드, 고유 에러 코드, 사용자 메시지를 관리합니다.
- **커스트 예외 클래스**: `BusinessException`을 상속받아 `HubNotFoundException`, `HubRouteNotFoundException` 등 도메인 특화 예외를 구현했습니다.

## 2. 왜 이 방식을 선택했는가 (설계 의도)
- **일관된 응답 구조**: 모든 예외는 `common` 모듈의 `GlobalExceptionHandler`에 의해 잡혀서 표준화된 `ApiResponse` 형태로 변환됩니다.
- **코드 기반 예외 식별**: 문자열 메시지 대신 `ErrorCode` enum을 사용하여 프론트엔드나 타 서비스에서 에러 원인을 정확히 파악할 수 있도록 했습니다.

## 3. 에러 코드 상세
| 에러 코드 | HTTP 상태 | 메시지 |
| :--- | :--- | :--- |
| HUB-001 | 404 Not Found | 허브를 찾을 수 없습니다. |
| HUB-002 | 404 Not Found | 허브 간 이동 정보를 찾을 수 없습니다. |
| HUB-003 | 422 Unprocessable | 출발 허브에서 목적지 허브까지 경로가 없습니다. |
| HUB-005 | 400 Bad Request | 페이지 크기는 10, 30, 50만 허용됩니다. |

## 4. 주의사항 및 한계
- 새로운 비즈니스 예외 추가 시 `HubErrorCode`에 먼저 코드를 할당하고 전용 예외 클래스를 생성해야 합니다.

## 5. 팀원 공유 사항
- `common` 모듈의 ArchUnit 규칙에 따라, 모든 커스텀 예외는 `domain.exception` 패키지에 위치해야 하며 이름이 `Exception` 또는 `ErrorCode`로 끝나야 합니다.
