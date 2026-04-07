# AGENTS.md — sttgw 프로젝트 AI 에이전트 지침서

## 프로젝트 개요

**STTGateway(sttgw)**는 IPCC 컨택센터의 미러링된 음성 패킷을 캡처하여 Google STT 등으로 텍스트 변환하는 Spring Boot 미들웨어입니다.

## 기술 스택

- **언어**: Java 17
- **프레임워크**: Spring Boot 3.5.13
- **빌드**: Gradle (Groovy DSL)
- **DB**: H2 (파일 모드, `/APP_DATA/db/sttgw`)
- **프론트엔드**: Thymeleaf + Vanilla JS + WebSocket
- **설정 포맷**: YAML (`application.yml`)
- **패키지**: `com.oxjohs.sttgw`

## 핵심 규칙

### 아키텍처 원칙

1. **모듈 경계 준수**: `capture`, `sip`, `rtp`, `session`, `audio`, `stt`, `api`, `websocket`, `dashboard`, `domain`, `repository`, `common` 패키지를 절대 합치지 않는다.
2. **STT 어댑터 패턴**: 모든 STT 연동은 `SttAdapter` 인터페이스를 구현해야 한다. 특정 벤더에 직접 의존하는 코드를 서비스 계층에 작성하지 않는다.
3. **이벤트 기반 연동**: 모듈 간 통신은 `SessionEventListener`를 통해 이루어진다. 순환 의존을 만들지 않는다.

### 코딩 규칙

- 상세 내용은 `docs/CONVENTIONS.md`를 참조한다.
- 모든 클래스에 `@Slf4j`를 사용하고, `System.out.println`을 쓰지 않는다.
- DTO와 엔티티를 분리한다. 컨트롤러에서 엔티티를 직접 반환하지 않는다 (단순 조회 제외).
- `@ConfigurationProperties`로 설정값을 바인딩하고, 하드코딩하지 않는다.

### 파일 경로

| 용도 | 경로 |
|------|------|
| 애플리케이션 JAR | `/APP/sttgw/` |
| 패킷 덤프 | `/APP_DATA/DUMP/yyyy/MM/dd/` |
| 오디오 파일 | `/APP_DATA/audio/yyyy/MM/dd/` |
| H2 DB | `/APP_DATA/db/sttgw` |
| 로그 | `/APP_LOGS/sttgw/` |
| Google 인증키 | `/APP/sttgw/credentials/google-stt-key.json` |

### 서버 정보

| 서버 | IP | 역할 |
|------|-----|------|
| IPCC | 211.192.89.43 | PBX, IVR, CTI, 통계, 녹취 |
| 상담AP | 192.168.201.39 | 상담사 애플리케이션 |
| STTGateway | 211.192.89.96 | 본 애플리케이션 배포 대상 |

### 패킷 캡처

- 인터페이스: `eth3` (이더넷 4번 포트)
- BPF 필터: `udp` (SIP + RTP만 캡처)
- SIP 포트: 5060
- 캡처 라이브러리: Pcap4J (서버에 `libpcap-dev` 필요)

### 테스트 규칙

- 단위 테스트: SipParser, RtpDecoder, G711Codec은 반드시 테스트 작성
- 통합 테스트: `@SpringBootTest`로 H2 기반 테스트
- 패킷 캡처 테스트: 실제 eth3 접근이 필요하므로 Mock 사용

### 금지 사항

- `Thread.sleep()`으로 타이밍 제어하지 않는다
- `Runtime.exec()`으로 외부 명령 실행하지 않는다
- 패킷 캡처 스레드에서 동기 I/O를 수행하지 않는다
- Google STT 클라이언트를 싱글턴으로 관리하지 않는다 (통화별 세션)
- WebSocket 메시지에 민감 정보(고객 전화번호 전체)를 포함하지 않는다

## 관련 문서

- `docs/ARCHITECTURE.md` — 전체 설계
- `docs/CONVENTIONS.md` — 코딩 컨벤션
- `docs/SCHEMA.md` — DB 스키마
- `docs/DEPLOYMENT.md` — 배포 가이드
