# STTGateway (sttgw)

IPCC 컨택센터 미러링 트래픽에서 SIP/RTP 패킷을 수집하고, 통화 오디오를 분리/저장한 뒤 STT 결과를 API와 WebSocket으로 제공하는 Spring Boot 기반 미들웨어입니다.

## 1. 프로젝트 개요

- 프로젝트명: `sttgw`
- 기본 패키지: `com.oxjohs.sttgw`
- 목표:
  - 미러링 UDP 트래픽(eth3)에서 SIP/RTP 분리
  - 콜 세션 상태 관리
  - RX/TX 오디오 분리 및 WAV 저장
  - Google STT 어댑터 기반 텍스트 변환
  - REST API / Dashboard / WebSocket 제공

## 2. 기술 스택

- Java 17
- Spring Boot `3.5.13`
- Gradle (Groovy DSL)
- Spring Data JPA + H2
- Thymeleaf + Vanilla JS
- WebSocket
- Pcap4J (`1.8.2`)
- Google Cloud Speech-to-Text (`4.31.0`)

의존성 상세는 [build.gradle](/Users/johs/Documents/IntelliJ/sttgw/build.gradle) 참고.

## 3. 패키지 구성

현재 구현된 주요 패키지:

- `capture`: 패킷 캡처 및 pcap dump
- `sip`: SIP 파싱
- `rtp`: RTP 파싱/G.711 디코딩
- `session`: 콜 세션 상태 관리
- `audio`: PCM/WAV 파일 처리
- `stt`: STT 공통 계약/팩토리
- `stt.google`: Google STT 어댑터
- `api`: REST API
- `websocket`: 실시간 이벤트 전송
- `dashboard`: 대시보드 화면 컨트롤러
- `domain`, `repository`: JPA 엔티티/리포지토리
- `common`, `config`: 공통 유틸/설정

## 4. 실행 프로파일

설정 파일:

- 공통: [application.yaml](/Users/johs/Documents/IntelliJ/sttgw/src/main/resources/application.yaml)
- 개발: [application-dev.yaml](/Users/johs/Documents/IntelliJ/sttgw/src/main/resources/application-dev.yaml)
- 운영: [application-prod.yaml](/Users/johs/Documents/IntelliJ/sttgw/src/main/resources/application-prod.yaml)

### 4.1 개발환경(dev)

- 로컬 경로 사용:
  - DB: `/Users/johs/Documents/IntelliJ/sttgw/.local/db/sttgw`
  - audio: `/Users/johs/Documents/IntelliJ/sttgw/.local/audio`
  - dump: `/Users/johs/Documents/IntelliJ/sttgw/.local/dump`
  - logs: `/Users/johs/Documents/IntelliJ/sttgw/.local/logs`
  - Google key: `/Users/johs/Documents/IntelliJ/sttgw/.local/credentials/google-stt-key.json`
- `capture.enabled=false`

실행 예시:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 4.2 운영환경(prod)

- 운영 경로 사용:
  - DB: `/APP_DATA/db/sttgw`
  - audio: `/APP_DATA/audio`
  - dump: `/APP_DATA/DUMP`
  - logs: `/APP_LOGS/sttgw`
  - Google key: `/APP/sttgw/credentials/google-stt-key.json`
- `capture.enabled=true`, `interface-name=eth3`

실행 예시:

```bash
java -jar sttgw-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 5. 빌드/테스트

```bash
./gradlew compileJava
./gradlew test
```

## 6. REST API

Base path: `/api`

### 6.1 통화 API

- `GET /api/calls/active`
- `GET /api/calls/history?page=0&size=20`
- `GET /api/calls/{callId}/transcript`

구현: [CallController.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/api/CallController.java)

### 6.2 STT API

- `GET /api/stt/vendors`
- `POST /api/stt/recognize?filePath={path}&lang=ko-KR&vendor=GOOGLE`

구현: [SttController.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/api/SttController.java)

### 6.3 설정 API

- `GET /api/settings/stt`
- `PUT /api/settings/stt`

구현: [SettingController.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/api/SettingController.java)

## 7. Dashboard / WebSocket

화면:

- `/` 대시보드
- `/settings` 설정

WebSocket endpoint:

- `/ws/stt`

주요 이벤트:

- `WS_READY`
- `SESSION_CREATED`
- `SESSION_COMPLETED`
- `STT_RESULT`

구현:

- [DashboardController.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/dashboard/DashboardController.java)
- [WebSocketConfig.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/config/WebSocketConfig.java)
- [SttWebSocketHandler.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/websocket/SttWebSocketHandler.java)

보안 주의:

- WebSocket payload에서 고객 전화번호는 마스킹되어 전송됩니다.

## 8. Google STT 연동

### 8.1 필수 사전 조건

1. Google Cloud Speech-to-Text API 활성화
2. 서비스 계정 준비
3. 키 또는 ADC 인증 구성

### 8.2 로컬 개발 권장

- 현재 개발환경은 JSON 키 파일 경로를 사용하도록 구성되어 있습니다.
- 키 파일 경로:
  - `/Users/johs/Documents/IntelliJ/sttgw/.local/credentials/google-stt-key.json`

### 8.3 운영 환경 권장

- 운영 키 경로:
  - `/APP/sttgw/credentials/google-stt-key.json`
- 필요 시 환경변수:
  - `GOOGLE_APPLICATION_CREDENTIALS=/APP/sttgw/credentials/google-stt-key.json`

### 8.4 관련 코드

- [GoogleSttProperties.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/stt/google/GoogleSttProperties.java)
- [GoogleSttAdapter.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/stt/google/GoogleSttAdapter.java)
- [GoogleSttStreamingSession.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/stt/google/GoogleSttStreamingSession.java)

## 9. 캡처 모듈 주의사항

- `capture`는 `SmartLifecycle` 기반입니다.
- `dev`에서는 기본 비활성화, `prod`에서는 활성화 구성입니다.
- 운영에서 실제 캡처를 사용하려면 서버에서 `libpcap` 및 네트워크 권한 구성이 필요합니다.

관련 코드:

- [CaptureProperties.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/capture/CaptureProperties.java)
- [PacketCaptureService.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/capture/PacketCaptureService.java)
- [PacketDumpWriter.java](/Users/johs/Documents/IntelliJ/sttgw/src/main/java/com/oxjohs/sttgw/capture/PacketDumpWriter.java)

## 10. 현재 구현 상태

완료:

- 1차: 기본 패키지 골격
- 2차: 도메인/리포지토리 및 핵심 서비스 골격
- 3차: 핵심 단위 테스트
- 4차: REST API 계층
- 5차: Dashboard + WebSocket
- 6차: capture 패키지
- 7차: Google STT 어댑터
- dev/prod 프로파일 분리

남은 작업(요약):

- 실제 샘플 오디오 기반 Google STT 호출 시나리오 검증
- 운영 환경 통합 점검
- 최종 배포/운영 문서 보강

## 11. 참고 문서

- [AGENTS.md](/Users/johs/Documents/IntelliJ/sttgw/AGENTS.md)
- [ARCHITECTURE.md](/Users/johs/Documents/IntelliJ/sttgw/docs/ARCHITECTURE.md)
- [CONVENTIONS.md](/Users/johs/Documents/IntelliJ/sttgw/docs/CONVENTIONS.md)
- [SCHEMA.md](/Users/johs/Documents/IntelliJ/sttgw/docs/SCHEMA.md)
- [DEPLOYMENT.md](/Users/johs/Documents/IntelliJ/sttgw/docs/DEPLOYMENT.md)
