# sttgw — 코딩 컨벤션

## 1. Java 코드 스타일

### 1.1 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 패키지 | 소문자, 단수 | `com.oxjohs.sttgw.capture` |
| 클래스 | PascalCase | `PacketCaptureService` |
| 인터페이스 | PascalCase, 접미사 없음 | `SttAdapter` (~~ISttAdapter~~ 사용 금지) |
| 메서드 | camelCase, 동사로 시작 | `parseHeader()`, `decodeRtp()` |
| 상수 | UPPER_SNAKE_CASE | `MAX_PACKET_SIZE` |
| 변수 | camelCase | `callSession`, `rtpPacket` |
| Enum 값 | UPPER_SNAKE_CASE | `CallState.CONNECTED` |
| 설정 프로퍼티 | kebab-case (YAML) | `sttgw.capture.interface-name` |

### 1.2 클래스 구조 순서

```java
public class ExampleService {
    // 1. static 상수
    // 2. 인스턴스 필드 (final 먼저)
    // 3. 생성자
    // 4. public 메서드
    // 5. package-private 메서드
    // 6. private 메서드
    // 7. equals/hashCode/toString
}
```

### 1.3 필수 어노테이션

- 서비스: `@Service`, `@Slf4j`, `@RequiredArgsConstructor`
- 컨트롤러: `@RestController` 또는 `@Controller`, `@RequiredArgsConstructor`
- 설정: `@Component`, `@ConfigurationProperties(prefix = "...")`
- 엔티티: `@Entity`, `@Table`, `@Data` (또는 `@Getter`/`@Setter`)

### 1.4 의존성 주입

- **생성자 주입**만 사용한다 (`@RequiredArgsConstructor` + `final` 필드).
- `@Autowired` 필드 주입을 사용하지 않는다.

```java
// ✅ 올바른 방법
@Service
@RequiredArgsConstructor
public class SttService {
    private final SttAdapterFactory adapterFactory;
    private final CallSessionManager sessionManager;
}

// ❌ 잘못된 방법
@Service
public class SttService {
    @Autowired
    private SttAdapterFactory adapterFactory;
}
```

### 1.5 예외 처리

- 비즈니스 예외: `com.oxjohs.sttgw.common.SttgwException` 상속
- 패킷 처리 중 예외: catch 후 로그만 남기고 다음 패킷 처리 계속 (서비스 중단 방지)
- STT 호출 실패: 재시도 1회 후 실패 로그, 세션은 유지

```java
// 패킷 처리 루프에서의 예외 처리 패턴
while (running) {
    try {
        Packet packet = handle.getNextPacket();
        processPacket(packet);
    } catch (Exception e) {
        log.warn("패킷 처리 오류 (무시하고 계속): {}", e.getMessage());
        // 절대 throw하지 않음 - 캡처 루프가 죽으면 안 됨
    }
}
```

## 2. 로깅 규칙

### 2.1 로그 레벨 기준

| 레벨 | 사용 시점 |
|------|-----------|
| `ERROR` | 시스템 기능 장애 (STT 연결 실패, DB 오류) |
| `WARN` | 복구 가능한 문제 (패킷 파싱 실패, 단일 WebSocket 전송 실패) |
| `INFO` | 주요 비즈니스 이벤트 (통화 시작/종료, STT 결과) |
| `DEBUG` | 상세 처리 정보 (SIP 헤더 값, 세션 상태 변경) |
| `TRACE` | 패킷 단위 로그 (RTP 시퀀스, PCM 샘플 수) — 운영에서 절대 켜지 않음 |

### 2.2 로그 포맷

```java
// ✅ 파라미터 바인딩 사용
log.info("통화 시작: callId={}, caller={}", callId, callerNumber);

// ❌ 문자열 연결 금지 (성능 저하)
log.info("통화 시작: callId=" + callId + ", caller=" + callerNumber);
```

### 2.3 민감 정보

- 고객 전화번호: 마스킹 처리 (`070****5642`)
- 로그 파일에 음성 데이터(byte[])를 출력하지 않는다

## 3. 설정 관리

### 3.1 프로파일

| 프로파일 | 용도 | 활성화 |
|----------|------|--------|
| `dev` | 로컬 개발 | `--spring.profiles.active=dev` |
| `prod` | 운영 서버 | `--spring.profiles.active=prod` |

### 3.2 설정 오버라이드 우선순위

1. 환경 변수 (`STTGW_CAPTURE_INTERFACE_NAME`)
2. 외부 `application.yml` (`/APP/sttgw/application.yml`)
3. JAR 내부 `application-{profile}.yml`
4. JAR 내부 `application.yml`

### 3.3 하드코딩 금지

모든 경로, 포트, 타임아웃 값은 `@ConfigurationProperties`로 관리한다.

```java
// ✅
@ConfigurationProperties(prefix = "sttgw.capture")
public class CaptureProperties {
    private String interfaceName = "eth3";
}

// ❌
private static final String INTERFACE = "eth3"; // 하드코딩
```

## 4. API 설계 규칙

### 4.1 URL 패턴

- REST API: `/api/{리소스}` (복수형)
- 대시보드 페이지: `/`, `/settings`
- WebSocket: `/ws/stt`
- H2 콘솔: `/h2-console`

### 4.2 응답 형식

```java
// 성공
{ "data": { ... }, "timestamp": "2026-04-07T10:00:00" }

// 에러
{ "error": "STT_ADAPTER_NOT_FOUND", "message": "지원하지 않는 STT 벤더입니다", "timestamp": "..." }
```

## 5. 테스트

### 5.1 테스트 파일 위치

`src/test/java/com/oxjohs/sttgw/{모듈명}/` — 소스와 동일한 패키지 구조

### 5.2 테스트 네이밍

```java
@Test
void SIP_INVITE_패킷에서_CallID를_정상_추출한다() { ... }

@Test
void G711_Alaw_데이터를_16bit_PCM으로_변환한다() { ... }
```

### 5.3 필수 테스트 대상

- `SipParser`: INVITE, BYE, CANCEL 파싱
- `RtpPacket`: 헤더 파싱, SSRC 추출
- `G711Codec`: A-law/μ-law 디코딩 정확도
- `WavFileWriter`: WAV 헤더 유효성
- `SttAdapterFactory`: 벤더 선택 로직

## 6. Git 커밋 메시지

```
feat: Google STT 스트리밍 어댑터 구현
fix: RTP 시퀀스 번호 오버플로우 처리
refactor: CallSessionManager 이벤트 리스너 분리
docs: ARCHITECTURE.md 모듈 구성도 추가
test: SipParser 단위 테스트 추가
```

## 7. 스레드 모델

| 스레드 | 이름 패턴 | 역할 |
|--------|-----------|------|
| 패킷 캡처 | `packet-capture` | eth3 패킷 수신 (1개, 데몬) |
| pcap 덤프 | `pcap-dump` | pcap 파일 비동기 쓰기 (1개) |
| 오디오 쓰기 | `audio-writer-N` | WAV 파일 생성 (최대 4개) |
| STT 스트리밍 | `stt-stream-N` | Google STT gRPC 스트림 (콜당 1~2개) |
| WebSocket | Spring 관리 | WebSocket 메시지 브로드캐스트 |
| HTTP 요청 | `http-nio-*` | REST API 및 Thymeleaf 요청 처리 |
