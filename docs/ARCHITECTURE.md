# STTGateway (sttgw) — 전체 설계 문서

> **버전**: 1.0  
> **작성일**: 2026-04-07  
> **작성자**: AICC솔루션기술파트

---

## 1. 프로젝트 개요

### 1.1 목적

STTGateway는 기존 IPCC 시스템에서 고객과 상담사가 통화하는 **음성 데이터를 실시간으로 캡처**하고, 이를 **Google STT(Speech-to-Text)** 등 다양한 STT 엔진을 통해 **텍스트로 변환**하는 미들웨어 애플리케이션입니다.

### 1.2 핵심 가치

- **실시간 스트리밍 STT**: 통화 중 실시간으로 음성→텍스트 변환
- **배치 STT**: 통화 종료 후 녹음 파일 기반 일괄 변환
- **멀티 STT 벤더 지원**: Google STT 외에도 국산 STT, 금융권 STT 등 어댑터 패턴으로 확장
- **RX/TX 분리**: 고객(RX)과 상담사(TX) 음성을 별도 채널로 분리하여 화자 구분

### 1.3 용어 사전 (초보 개발자용)

| 용어 | 쉬운 설명 |
|------|-----------|
| **IPCC** | IP 기반 컨택센터. 전화를 인터넷 프로토콜로 처리하는 콜센터 시스템 |
| **PBX** | 전화 교환기. 외부 전화를 내부 상담사에게 연결해주는 장비 |
| **IVR** | 자동응답시스템. "1번을 누르시면~" 하는 그것 |
| **CTI** | 컴퓨터-전화 통합. 상담사 PC 화면에 전화 정보를 보여주는 기술 |
| **SIP** | 전화 연결/종료를 제어하는 신호 프로토콜 (HTTP와 비슷한 텍스트 기반) |
| **RTP** | 실제 음성 데이터를 실시간으로 전송하는 프로토콜 |
| **RX** | Receive. 수신측 음성 (고객 음성) |
| **TX** | Transmit. 송신측 음성 (상담사 음성) |
| **PCM** | 디지털 음성 데이터의 가장 기본적인 형태 (압축 없는 raw 오디오) |
| **WAV** | PCM 데이터에 헤더를 붙인 오디오 파일 형식 |
| **패킷 미러링** | 스위치에서 특정 포트의 네트워크 트래픽을 다른 포트로 복사해서 보내주는 기능 |
| **STT** | Speech-to-Text. 음성을 텍스트로 변환하는 기술 |
| **G.711** | 전화 통화에 사용되는 음성 코덱. μ-law(북미)와 A-law(유럽/한국) 두 종류 |
| **WebSocket** | 서버와 클라이언트 간 실시간 양방향 통신을 위한 프로토콜 |

---

## 2. 전체 시스템 구성도

```
┌─────────────────────────────────────────────────────────────────────┐
│                        전체 시스템 구성도                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   [고객 전화]                                                        │
│   070-4140-5642                                                     │
│       │                                                             │
│       ▼                                                             │
│   ┌─────────────────────────────────┐                               │
│   │  IPCC 서버 (211.192.89.43)      │                               │
│   │  ┌─────┐ ┌─────┐ ┌─────┐       │                               │
│   │  │ PBX │ │ IVR │ │ CTI │       │     ┌───────────────────────┐  │
│   │  └─────┘ └─────┘ └──┬──┘       │     │ 상담AP (192.168.201.39)│ │
│   │  ┌──────┐ ┌──────┐  │          │     │ (상담사 PC 애플리케이션)│  │
│   │  │ 통계 │ │ 녹취 │  └──────────┼────▶│                       │  │
│   │  └──────┘ └──────┘             │     └───────────────────────┘  │
│   └───────────┬─────────────────────┘                               │
│               │ 음성 패킷 (RTP)                                      │
│               ▼                                                     │
│   ┌───────────────────┐                                             │
│   │  GS1900 스위치     │                                             │
│   │  (패킷 미러링)     │                                             │
│   └───────────┬───────┘                                             │
│               │ 미러링된 패킷                                        │
│               ▼                                                     │
│   ┌─────────────────────────────────────────────────┐               │
│   │  STTGateway 서버 (211.192.89.96)                 │               │
│   │  eth3 포트로 패킷 수신                            │               │
│   │                                                  │               │
│   │  ┌────────────────────────────────────────────┐  │               │
│   │  │         STTGateway 애플리케이션              │  │               │
│   │  │                                            │  │               │
│   │  │  ┌──────────┐   ┌──────────┐               │  │               │
│   │  │  │패킷 캡처 │──▶│SIP 파싱  │               │  │               │
│   │  │  │ (eth3)   │   │(콜 정보) │               │  │               │
│   │  │  └────┬─────┘   └──────────┘               │  │               │
│   │  │       │                                    │  │               │
│   │  │       ▼                                    │  │               │
│   │  │  ┌──────────┐   ┌──────────┐               │  │               │
│   │  │  │RTP 디코딩│──▶│오디오    │               │  │               │
│   │  │  │RX/TX분리 │   │파일 생성 │               │  │               │
│   │  │  └────┬─────┘   └──────────┘               │  │               │
│   │  │       │                                    │  │               │
│   │  │       ▼                                    │  │               │
│   │  │  ┌──────────┐   ┌──────────────────┐       │  │               │
│   │  │  │STT 어댑터│──▶│Google STT / 기타 │       │  │               │
│   │  │  │(확장 가능)│   │STT 엔진 연동     │       │  │               │
│   │  │  └────┬─────┘   └──────────────────┘       │  │               │
│   │  │       │                                    │  │               │
│   │  │       ▼                                    │  │               │
│   │  │  ┌──────────┐   ┌──────────────────┐       │  │               │
│   │  │  │WebSocket │──▶│Thymeleaf 대시보드│       │  │               │
│   │  │  │실시간푸시 │   │(설정 + 모니터링) │       │  │               │
│   │  │  └──────────┘   └──────────────────┘       │  │               │
│   │  └────────────────────────────────────────────┘  │               │
│   └──────────────────────────────────────────────────┘               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. 핵심 개념 설명 (초보 개발자용)

### 3.1 패킷 미러링이란?

네트워크 스위치(GS1900)는 원래 A→B로 가는 데이터를 그대로 전달하는 장비입니다. "미러링"이란 이 데이터를 **복사해서 C에게도 보내주는 것**입니다.

```
[IPCC 서버] ──음성패킷──▶ [상담사 PC]
                  │
                  └──복사──▶ [STTGateway 서버 eth3]  ← 이게 미러링!
```

우리 STTGateway는 이 복사된 패킷을 읽어서 음성 데이터를 추출합니다.

### 3.2 SIP와 RTP의 관계

전화 한 통에는 두 종류의 데이터가 오갑니다.

- **SIP**: "전화 걸겠습니다" / "전화 받겠습니다" / "전화 끊겠습니다" 같은 **신호**
- **RTP**: 실제 "여보세요~" 하는 **음성 데이터**

SIP 패킷에서 콜 정보(고객 전화번호, Call-ID 등)를 추출하고, RTP 패킷에서 음성 데이터를 추출합니다.

```
SIP INVITE (전화 시작)
  → From: <sip:07041405642@...>    ← 고객 전화번호
  → Call-ID: abc123@ipcc           ← 콜 고유 식별자
  → SDP 정보: 미디어 포트, 코덱 정보

RTP 패킷 (음성 데이터)
  → SSRC로 RX/TX 구분
  → Payload: G.711 인코딩된 음성 데이터
```

### 3.3 RX/TX 분리란?

한 통화에는 두 사람의 목소리가 있습니다.

- **RX (수신)**: 고객의 음성
- **TX (송신)**: 상담사의 음성

이 두 음성을 분리하는 이유는 STT 변환 시 **누가 한 말인지 구분**하기 위함입니다. RTP 패킷의 **SSRC**(동기화 소스 식별자) 값이 다르므로 이를 기준으로 분리합니다.

### 3.4 G.711 코덱

전화 통화에 사용되는 오디오 코덱입니다. 한국에서는 주로 **A-law** 방식을 사용합니다.

- 샘플링 레이트: 8000Hz (1초에 8000번 소리를 측정)
- 비트 수: 8bit
- 1초 데이터 크기: 8,000 bytes = 약 8KB

### 3.5 STT 어댑터 패턴

지금은 Google STT만 사용하지만, 나중에 다른 회사의 STT로 바꿀 수 있어야 합니다. 이를 위해 **어댑터 패턴**을 사용합니다.

```
                    ┌─── GoogleSttAdapter ──▶ Google Cloud STT
                    │
[STTGateway] ──▶ [SttAdapter 인터페이스] ─┼─── NaverSttAdapter ──▶ Naver Clova STT
                    │
                    └─── CustomSttAdapter ──▶ 금융권 국산 STT
```

마치 **USB 허브**처럼, 어떤 STT를 꽂아도 동일한 방식으로 사용할 수 있습니다.

---

## 4. 서버 디렉토리 구조

### 4.1 운영 서버 디렉토리 (211.192.89.96)

```
/
├── APP/                          ← 애플리케이션 배포 경로
│   └── sttgw/
│       ├── sttgw-1.0.0.jar       ← 실행 가능한 JAR 파일
│       ├── application.yml        ← 운영 환경 설정 파일
│       ├── credentials/
│       │   └── google-stt-key.json← Google Cloud 인증키
│       └── bin/
│           ├── start.sh           ← 기동 스크립트
│           └── stop.sh            ← 종료 스크립트
│
├── APP_DATA/                     ← 데이터 저장 경로
│   ├── DUMP/                     ← 캡처된 패킷 데이터 (.pcap)
│   │   └── 2026/04/07/
│   │       ├── call_abc123.pcap
│   │       └── ...
│   ├── audio/                    ← RX/TX 분리된 오디오 파일
│   │   └── 2026/04/07/
│   │       ├── abc123_rx.wav      ← 고객 음성
│   │       ├── abc123_tx.wav      ← 상담사 음성
│   │       └── abc123_mix.wav     ← 혼합 음성 (선택)
│   └── db/
│       └── sttgw.mv.db           ← H2 데이터베이스 파일
│
└── APP_LOGS/                     ← 로그 파일 경로
    └── sttgw/
        ├── sttgw.log             ← 현재 로그
        ├── sttgw.2026-04-06.log  ← 일자별 아카이브
        └── ...
```

### 4.2 프로젝트 소스 디렉토리 (start.spring.io 기준)

```
sttgw/
├── build.gradle                   ← Gradle 빌드 스크립트
├── settings.gradle                ← 프로젝트 설정
├── gradlew / gradlew.bat          ← Gradle 래퍼
├── AGENTS.md                      ← AI 에이전트 지침서
├── docs/
│   ├── ARCHITECTURE.md            ← 전체 설계 문서 (본 문서)
│   ├── CONVENTIONS.md             ← 코딩 컨벤션
│   ├── SCHEMA.md                  ← 데이터베이스 스키마 설계
│   └── DEPLOYMENT.md              ← 배포 가이드
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── java/com/oxjohs/sttgw/
    │   │   ├── SttgwApplication.java           ← 메인 엔트리포인트
    │   │   ├── config/                          ← 설정 클래스
    │   │   │   ├── AppConfig.java
    │   │   │   ├── AsyncConfig.java
    │   │   │   ├── WebSocketConfig.java
    │   │   │   └── H2Config.java
    │   │   ├── capture/                         ← 패킷 캡처 모듈
    │   │   │   ├── PacketCaptureService.java
    │   │   │   ├── PacketDumpWriter.java
    │   │   │   └── CaptureProperties.java
    │   │   ├── sip/                             ← SIP 파싱 모듈
    │   │   │   ├── SipParser.java
    │   │   │   ├── SipMessage.java
    │   │   │   └── SipMethodType.java
    │   │   ├── rtp/                             ← RTP 디코딩 모듈
    │   │   │   ├── RtpDecoder.java
    │   │   │   ├── RtpPacket.java
    │   │   │   └── G711Codec.java
    │   │   ├── session/                         ← 통화 세션 관리 모듈
    │   │   │   ├── CallSession.java
    │   │   │   ├── CallSessionManager.java
    │   │   │   ├── CallState.java
    │   │   │   └── SessionEventListener.java
    │   │   ├── audio/                           ← 오디오 파일 생성 모듈
    │   │   │   ├── AudioFileService.java
    │   │   │   ├── WavFileWriter.java
    │   │   │   ├── PcmBuffer.java
    │   │   │   └── AudioProperties.java
    │   │   ├── stt/                             ← STT 연동 모듈 (어댑터 패턴)
    │   │   │   ├── SttAdapter.java              ← 인터페이스
    │   │   │   ├── SttResult.java
    │   │   │   ├── SttMode.java                 ← BATCH / STREAMING 열거형
    │   │   │   ├── SttAdapterFactory.java
    │   │   │   ├── SttProperties.java
    │   │   │   └── google/
    │   │   │       ├── GoogleSttAdapter.java
    │   │   │       └── GoogleSttProperties.java
    │   │   ├── api/                             ← REST API 모듈
    │   │   │   ├── CallController.java
    │   │   │   ├── SttController.java
    │   │   │   ├── SettingController.java
    │   │   │   └── dto/
    │   │   │       ├── CallInfoResponse.java
    │   │   │       ├── SttResultResponse.java
    │   │   │       └── SttSettingRequest.java
    │   │   ├── websocket/                       ← WebSocket 실시간 푸시 모듈
    │   │   │   ├── SttWebSocketHandler.java
    │   │   │   └── WebSocketSessionRegistry.java
    │   │   ├── dashboard/                       ← Thymeleaf 대시보드 모듈
    │   │   │   └── DashboardController.java
    │   │   ├── domain/                          ← 엔티티(DB 테이블 매핑)
    │   │   │   ├── CallRecord.java
    │   │   │   ├── SttTranscript.java
    │   │   │   └── SttVendorConfig.java
    │   │   ├── repository/                      ← 데이터베이스 접근 계층
    │   │   │   ├── CallRecordRepository.java
    │   │   │   ├── SttTranscriptRepository.java
    │   │   │   └── SttVendorConfigRepository.java
    │   │   └── common/                          ← 공통 유틸리티
    │   │       ├── DateUtils.java
    │   │       ├── FileUtils.java
    │   │       └── Constants.java
    │   ├── resources/
    │   │   ├── application.yml                  ← 기본 설정
    │   │   ├── application-dev.yml              ← 개발 프로파일
    │   │   ├── application-prod.yml             ← 운영 프로파일
    │   │   ├── logback-spring.xml               ← 로그 설정
    │   │   ├── schema.sql                       ← H2 DDL
    │   │   ├── data.sql                         ← 초기 데이터
    │   │   ├── static/
    │   │   │   ├── css/
    │   │   │   │   └── dashboard.css
    │   │   │   └── js/
    │   │   │       ├── dashboard.js
    │   │   │       └── websocket-client.js
    │   │   └── templates/
    │   │       ├── layout/
    │   │       │   └── default.html
    │   │       ├── dashboard.html               ← 메인 대시보드
    │   │       └── settings.html                ← STT 설정 화면
    │   └── native/                              ← (선택) libpcap JNI 필요시
    └── test/
        └── java/com/oxjohs/sttgw/
            ├── SttgwApplicationTests.java
            ├── capture/
            │   └── PacketCaptureServiceTest.java
            ├── sip/
            │   └── SipParserTest.java
            ├── rtp/
            │   └── RtpDecoderTest.java
            └── stt/
                └── GoogleSttAdapterTest.java
```

---

## 5. Spring Boot 프로젝트 설정

### 5.1 start.spring.io 세팅

| 항목 | 값 |
|------|-----|
| Project | Gradle - Groovy |
| Language | Java |
| Spring Boot | 3.5.13 |
| Group | com.oxjohs |
| Artifact | sttgw |
| Name | sttgw |
| Package name | com.oxjohs.sttgw |
| Packaging | Jar |
| Java | 17 |

### 5.2 Dependencies 선택

start.spring.io에서 선택할 의존성과 수동 추가가 필요한 의존성을 구분합니다.

#### start.spring.io에서 선택

| 의존성 | 목적 |
|--------|------|
| **Spring Web** | REST API 제공, HTTP 요청 처리 |
| **Spring WebSocket** | 실시간 텍스트 푸시를 위한 WebSocket 지원 |
| **Spring Data JPA** | H2 데이터베이스 ORM |
| **H2 Database** | 경량 임베디드 데이터베이스 |
| **Thymeleaf** | 대시보드 HTML 템플릿 엔진 |
| **Spring Boot Actuator** | 애플리케이션 헬스체크, 모니터링 |
| **Lombok** | 보일러플레이트 코드 제거 |
| **Spring Boot DevTools** | 개발 시 핫리로드 |
| **Validation** | DTO 유효성 검사 |

#### build.gradle에 수동 추가

```groovy
dependencies {
    // --- start.spring.io 선택 항목 ---
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'com.h2database:h2'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    // --- 수동 추가: 패킷 캡처 ---
    implementation 'org.pcap4j:pcap4j-core:1.8.2'
    implementation 'org.pcap4j:pcap4j-packetfactory-static:1.8.2'

    // --- 수동 추가: Google Cloud STT ---
    implementation 'com.google.cloud:google-cloud-speech:4.31.0'

    // --- 수동 추가: 오디오 처리 보조 ---
    implementation 'commons-io:commons-io:2.15.1'

    // --- 테스트 ---
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

> **Pcap4J란?** Java에서 네트워크 패킷을 캡처할 수 있게 해주는 라이브러리입니다. C언어의 libpcap을 Java로 감싼 것으로, `tcpdump`와 같은 원리로 동작합니다. 서버에 `libpcap-dev`가 설치되어 있어야 합니다.

---

## 6. 핵심 모듈 상세 설계

### 6.1 모듈 간 관계도

```
┌────────────────────────────────────────────────────────────────────────┐
│                      STTGateway 모듈 관계도                            │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌─────────────┐                                                       │
│  │PacketCapture│─── eth3에서 패킷 읽기                                  │
│  │  Service    │                                                       │
│  └──────┬──────┘                                                       │
│         │ 패킷 데이터                                                   │
│         ▼                                                              │
│  ┌─────────────┐    SIP 패킷    ┌─────────────┐                        │
│  │ 패킷 분류기 │──────────────▶│  SipParser   │──▶ 콜 정보 추출         │
│  │(SIP vs RTP) │               └──────┬──────┘   (From, Call-ID)      │
│  └──────┬──────┘                      │                                │
│         │ RTP 패킷                     │ 세션 생성/종료 이벤트           │
│         ▼                             ▼                                │
│  ┌─────────────┐             ┌──────────────┐                          │
│  │ RtpDecoder  │────────────▶│CallSession   │                          │
│  │ (G.711→PCM) │  PCM 데이터  │  Manager     │◀── 세션별 상태 관리       │
│  │ RX/TX 분리  │             └──────┬───────┘                          │
│  └──────┬──────┘                    │                                  │
│         │                           │ 세션 이벤트                       │
│         ▼                           ▼                                  │
│  ┌─────────────┐             ┌──────────────┐                          │
│  │AudioFile    │             │  SttAdapter   │◀── 인터페이스            │
│  │  Service    │────────────▶│  (Factory)    │                          │
│  │(WAV 생성)   │  파일 경로   │              │                          │
│  └─────────────┘             └──────┬───────┘                          │
│                                     │                                  │
│                    ┌────────────────┼────────────────┐                 │
│                    ▼                ▼                ▼                 │
│              ┌──────────┐  ┌──────────────┐  ┌──────────┐             │
│              │ Google   │  │ Naver Clova  │  │ 국산 STT │             │
│              │STTAdapter│  │ STTAdapter   │  │ Adapter  │             │
│              └────┬─────┘  └──────────────┘  └──────────┘             │
│                   │                                                    │
│                   │ STT 결과 (텍스트)                                   │
│                   ▼                                                    │
│  ┌──────────────────────────────────────┐                              │
│  │         WebSocket 푸시               │                              │
│  │  (실시간 텍스트 → 브라우저 대시보드)    │                              │
│  └──────────────────────────────────────┘                              │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

### 6.2 모듈 1: 패킷 캡처 (capture)

#### 역할
eth3 인터페이스에서 미러링된 네트워크 패킷을 캡처하고, SIP/RTP 패킷을 구분하여 다음 모듈로 전달합니다.

#### 핵심 클래스

**CaptureProperties.java** — 설정값 바인딩
```java
@Component
@ConfigurationProperties(prefix = "sttgw.capture")
@Data
public class CaptureProperties {
    private String interfaceName = "eth3";      // 캡처할 네트워크 인터페이스
    private int snapLen = 65536;                // 패킷 최대 크기
    private int timeoutMillis = 10;             // 캡처 타임아웃
    private String dumpPath = "/APP_DATA/DUMP"; // pcap 파일 저장 경로
    private boolean dumpEnabled = true;         // pcap 저장 여부
    private String bpfFilter = "udp";           // BPF 필터 (UDP만 캡처)
}
```

**PacketCaptureService.java** — 핵심 캡처 로직
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class PacketCaptureService implements SmartLifecycle {

    private final CaptureProperties props;
    private final SipParser sipParser;
    private final RtpDecoder rtpDecoder;
    private final CallSessionManager sessionManager;
    private final PacketDumpWriter dumpWriter;

    private volatile boolean running = false;
    private PcapHandle handle;
    private Thread captureThread;

    @Override
    public void start() {
        running = true;
        captureThread = new Thread(this::captureLoop, "packet-capture");
        captureThread.setDaemon(true);
        captureThread.start();
        log.info("패킷 캡처 시작: interface={}", props.getInterfaceName());
    }

    private void captureLoop() {
        try {
            PcapNetworkInterface nif = Pcaps.getDevByName(props.getInterfaceName());
            handle = nif.openLive(
                props.getSnapLen(),
                PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
                props.getTimeoutMillis()
            );

            // BPF 필터 설정 (UDP 패킷만)
            handle.setFilter(props.getBpfFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);

            while (running) {
                Packet packet = handle.getNextPacket();
                if (packet == null) continue;

                // pcap 파일 저장 (비동기)
                if (props.isDumpEnabled()) {
                    dumpWriter.writeAsync(packet);
                }

                // 패킷 분류 및 처리
                UdpPacket udp = packet.get(UdpPacket.class);
                if (udp == null) continue;

                int dstPort = udp.getHeader().getDstPort().valueAsInt();
                byte[] payload = udp.getPayload().getRawData();

                if (isSipPacket(dstPort, payload)) {
                    // SIP 패킷 → 콜 세션 관리
                    sipParser.parse(payload, sessionManager);
                } else {
                    // RTP 패킷 → 음성 디코딩
                    IpV4Packet ipPacket = packet.get(IpV4Packet.class);
                    String srcIp = ipPacket.getHeader().getSrcAddr().getHostAddress();
                    rtpDecoder.decode(payload, srcIp, dstPort, sessionManager);
                }
            }
        } catch (Exception e) {
            log.error("패킷 캡처 오류", e);
        }
    }

    /**
     * SIP 패킷 판별: 포트 5060 또는 페이로드가 SIP 메서드로 시작
     */
    private boolean isSipPacket(int port, byte[] payload) {
        if (port == 5060) return true;
        String header = new String(payload, 0, Math.min(10, payload.length));
        return header.startsWith("INVITE") || header.startsWith("SIP/")
            || header.startsWith("BYE") || header.startsWith("ACK")
            || header.startsWith("CANCEL");
    }

    @Override
    public void stop() {
        running = false;
        if (handle != null && handle.isOpen()) handle.close();
        log.info("패킷 캡처 종료");
    }

    @Override
    public boolean isRunning() { return running; }
}
```

**PacketDumpWriter.java** — 비동기 pcap 저장 (디스크 I/O 분리)
```java
@Component
@Slf4j
public class PacketDumpWriter {

    private final ExecutorService dumpExecutor =
        Executors.newSingleThreadExecutor(r -> new Thread(r, "pcap-dump"));
    private final CaptureProperties props;

    // 일자별 PcapDumper 캐시
    private final ConcurrentHashMap<String, PcapDumper> dumperCache = new ConcurrentHashMap<>();

    public void writeAsync(Packet packet) {
        dumpExecutor.submit(() -> {
            try {
                String dateKey = LocalDate.now().toString();
                PcapDumper dumper = dumperCache.computeIfAbsent(dateKey, this::createDumper);
                dumper.dump(packet);
            } catch (Exception e) {
                log.warn("패킷 덤프 쓰기 실패", e);
            }
        });
    }
}
```

#### 플로우

```
eth3 인터페이스
    │
    ▼
[Pcap4J: PcapHandle.getNextPacket()]
    │
    ├── UDP 5060 또는 SIP 키워드 → SipParser.parse()
    │
    └── 그 외 UDP → RtpDecoder.decode()
    │
    └── (비동기) PacketDumpWriter → /APP_DATA/DUMP/yyyy/MM/dd/*.pcap
```

---

### 6.3 모듈 2: SIP 헤더 파싱 (sip)

#### 역할
SIP 패킷에서 통화 정보를 추출합니다. INVITE(통화시작), BYE(통화종료), ACK, CANCEL 등의 SIP 메서드를 해석합니다.

#### 핵심 클래스

**SipMessage.java** — SIP 메시지 파싱 결과
```java
@Data
@Builder
public class SipMessage {
    private SipMethodType method;      // INVITE, BYE, ACK 등
    private String callId;             // Call-ID 헤더 (콜 고유 키)
    private String fromUri;            // From 헤더 (고객 전화번호)
    private String toUri;              // To 헤더 (상담사 내선번호)
    private String fromTag;            // From 태그
    private String toTag;              // To 태그
    private int mediaPort;             // SDP에서 추출한 RTP 미디어 포트
    private String mediaIp;            // SDP에서 추출한 미디어 IP
    private String codec;              // SDP에서 추출한 코덱 정보
}
```

**SipParser.java** — SIP 헤더 파싱
```java
@Component
@Slf4j
public class SipParser {

    /**
     * SIP 패킷을 파싱하여 CallSessionManager에 이벤트 전달
     */
    public void parse(byte[] payload, CallSessionManager sessionManager) {
        String sipText = new String(payload, StandardCharsets.UTF_8);
        SipMessage msg = parseSipMessage(sipText);

        if (msg == null) return;

        switch (msg.getMethod()) {
            case INVITE -> {
                log.info("SIP INVITE 수신: callId={}, from={}", msg.getCallId(), msg.getFromUri());
                sessionManager.onCallStart(msg);
            }
            case BYE -> {
                log.info("SIP BYE 수신: callId={}", msg.getCallId());
                sessionManager.onCallEnd(msg);
            }
            case CANCEL -> {
                log.info("SIP CANCEL 수신: callId={}", msg.getCallId());
                sessionManager.onCallCancel(msg);
            }
            default -> log.debug("SIP {} 무시: callId={}", msg.getMethod(), msg.getCallId());
        }
    }

    private SipMessage parseSipMessage(String sipText) {
        SipMessage.SipMessageBuilder builder = SipMessage.builder();

        // 1. 메서드 추출 (첫 줄)
        String firstLine = sipText.substring(0, sipText.indexOf("\r\n"));
        builder.method(SipMethodType.fromFirstLine(firstLine));

        // 2. Call-ID 추출
        builder.callId(extractHeader(sipText, "Call-ID"));

        // 3. From 헤더에서 전화번호 추출
        //    예: From: <sip:07041405642@211.192.89.43>;tag=abc
        String from = extractHeader(sipText, "From");
        builder.fromUri(extractUriUser(from));   // → "07041405642"
        builder.fromTag(extractTag(from));

        // 4. To 헤더
        String to = extractHeader(sipText, "To");
        builder.toUri(extractUriUser(to));
        builder.toTag(extractTag(to));

        // 5. SDP 파싱 (INVITE에만 존재)
        if (sipText.contains("v=0")) {
            parseSdp(sipText, builder);
        }

        return builder.build();
    }

    /**
     * SDP(Session Description Protocol)에서 미디어 정보 추출
     * m=audio 10000 RTP/AVP 8    ← 포트 10000, 코덱 8(G.711 A-law)
     * c=IN IP4 211.192.89.43     ← 미디어 IP
     */
    private void parseSdp(String sipText, SipMessage.SipMessageBuilder builder) {
        // m= 라인에서 포트와 코덱 추출
        Pattern mPattern = Pattern.compile("m=audio (\\d+) RTP/AVP (\\d+)");
        Matcher mMatcher = mPattern.matcher(sipText);
        if (mMatcher.find()) {
            builder.mediaPort(Integer.parseInt(mMatcher.group(1)));
            builder.codec(mMatcher.group(2)); // "8" = G.711 A-law
        }

        // c= 라인에서 미디어 IP 추출
        Pattern cPattern = Pattern.compile("c=IN IP4 ([\\d.]+)");
        Matcher cMatcher = cPattern.matcher(sipText);
        if (cMatcher.find()) {
            builder.mediaIp(cMatcher.group(1));
        }
    }

    private String extractHeader(String sipText, String headerName) {
        Pattern p = Pattern.compile(headerName + ":\\s*(.+?)\\r\\n", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sipText);
        return m.find() ? m.group(1).trim() : "";
    }

    private String extractUriUser(String headerValue) {
        // <sip:07041405642@host> → 07041405642
        Pattern p = Pattern.compile("sip:([^@]+)@");
        Matcher m = p.matcher(headerValue);
        return m.find() ? m.group(1) : "";
    }

    private String extractTag(String headerValue) {
        Pattern p = Pattern.compile("tag=([^;\\s]+)");
        Matcher m = p.matcher(headerValue);
        return m.find() ? m.group(1) : "";
    }
}
```

---

### 6.4 모듈 3: RTP 디코딩 및 RX/TX 분리 (rtp)

#### 역할
RTP 패킷에서 음성 페이로드를 추출하고, G.711 코덱을 PCM으로 변환합니다. SSRC(동기화 소스) 값을 기준으로 RX/TX를 분리합니다.

#### RTP 패킷 구조 (쉬운 설명)

```
┌───────────────────────────────────────────────────┐
│                  RTP 패킷 (12바이트 헤더 + 페이로드) │
├───────────────────────────────────────────────────┤
│ V=2 │ P │ X │ CC │ M │ PT(7bit) │ Seq(16bit)     │
│     Timestamp (32bit)                             │
│     SSRC (32bit) ← 이 값으로 RX/TX 구분!           │
├───────────────────────────────────────────────────┤
│     Payload (G.711 인코딩된 음성 데이터)             │
│     160 bytes = 20ms 분량의 음성                    │
└───────────────────────────────────────────────────┘
```

**RtpPacket.java**
```java
@Data
public class RtpPacket {
    private int version;           // RTP 버전 (항상 2)
    private int payloadType;       // 코덱 타입 (8=G.711 A-law, 0=G.711 μ-law)
    private int sequenceNumber;    // 패킷 순서 번호
    private long timestamp;        // 타임스탬프
    private long ssrc;             // 동기화 소스 식별자 (RX/TX 구분 키)
    private byte[] payload;        // 음성 데이터

    public static RtpPacket parse(byte[] data) {
        RtpPacket pkt = new RtpPacket();
        pkt.version = (data[0] >> 6) & 0x03;
        pkt.payloadType = data[1] & 0x7F;
        pkt.sequenceNumber = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        pkt.timestamp = ((long)(data[4] & 0xFF) << 24) | ((long)(data[5] & 0xFF) << 16)
                       | ((long)(data[6] & 0xFF) << 8) | (data[7] & 0xFF);
        pkt.ssrc = ((long)(data[8] & 0xFF) << 24) | ((long)(data[9] & 0xFF) << 16)
                  | ((long)(data[10] & 0xFF) << 8) | (data[11] & 0xFF);
        pkt.payload = Arrays.copyOfRange(data, 12, data.length);
        return pkt;
    }
}
```

**RtpDecoder.java**
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class RtpDecoder {

    private final G711Codec g711Codec;
    private final CallSessionManager sessionManager;

    /**
     * RTP 패킷 디코딩 및 세션에 PCM 데이터 전달
     */
    public void decode(byte[] rawData, String srcIp, int srcPort, CallSessionManager mgr) {
        RtpPacket rtp = RtpPacket.parse(rawData);

        // G.711 → 16bit Linear PCM 변환
        short[] pcmSamples;
        if (rtp.getPayloadType() == 8) {
            pcmSamples = g711Codec.alawToPcm(rtp.getPayload());   // A-law
        } else if (rtp.getPayloadType() == 0) {
            pcmSamples = g711Codec.ulawToPcm(rtp.getPayload());   // μ-law
        } else {
            log.warn("지원하지 않는 코덱: PT={}", rtp.getPayloadType());
            return;
        }

        // CallSession에 PCM 데이터 전달 (SSRC로 RX/TX 구분)
        mgr.appendAudio(rtp.getSsrc(), pcmSamples, srcIp, srcPort);
    }
}
```

**G711Codec.java**
```java
@Component
public class G711Codec {

    // A-law 디코딩 테이블 (256개 엔트리)
    private static final short[] ALAW_TABLE = buildAlawTable();

    /**
     * G.711 A-law → 16bit Linear PCM
     * 전화 음성(8bit)을 일반 오디오(16bit)로 변환
     */
    public short[] alawToPcm(byte[] alawData) {
        short[] pcm = new short[alawData.length];
        for (int i = 0; i < alawData.length; i++) {
            pcm[i] = ALAW_TABLE[alawData[i] & 0xFF];
        }
        return pcm;
    }

    public short[] ulawToPcm(byte[] ulawData) {
        short[] pcm = new short[ulawData.length];
        for (int i = 0; i < ulawData.length; i++) {
            pcm[i] = MuLawDecompressTable[ulawData[i] & 0xFF];
        }
        return pcm;
    }

    private static short[] buildAlawTable() {
        // ITU-T G.711 A-law 디코딩 알고리즘 구현
        short[] table = new short[256];
        for (int i = 0; i < 256; i++) {
            int input = i ^ 0x55;
            int mantissa = (input & 0x0F) << 4;
            int segment = (input & 0x70) >> 4;
            int value;
            if (segment == 0) {
                value = mantissa + 8;
            } else {
                value = (mantissa + 0x108) << (segment - 1);
            }
            table[i] = (short) ((input & 0x80) == 0 ? value : -value);
        }
        return table;
    }
}
```

---

### 6.5 모듈 4: 통화 세션 관리 (session)

#### 역할
SIP INVITE/BYE 이벤트로 콜 세션을 생성/종료하고, RTP 데이터를 세션별로 관리합니다.

**CallState.java**
```java
public enum CallState {
    RINGING,     // 벨 울리는 중
    CONNECTED,   // 통화 중
    COMPLETED,   // 통화 종료
    CANCELLED    // 취소됨
}
```

**CallSession.java**
```java
@Data
@Slf4j
public class CallSession {
    private final String callId;               // SIP Call-ID
    private final String callerNumber;         // 고객 전화번호 (From)
    private final String agentExtension;       // 상담사 내선번호 (To)
    private final LocalDateTime startTime;     // 통화 시작 시각
    private volatile CallState state;
    private LocalDateTime endTime;

    // SSRC → PCM 버퍼 (RX/TX 분리)
    // 첫 번째로 등록되는 SSRC = RX(고객), 두 번째 = TX(상담사)
    private final ConcurrentHashMap<Long, PcmBuffer> ssrcBuffers = new ConcurrentHashMap<>();
    private final AtomicInteger ssrcCount = new AtomicInteger(0);

    // SSRC 등록 순서 기록
    private Long rxSsrc;  // 고객 SSRC
    private Long txSsrc;  // 상담사 SSRC

    /**
     * RTP 패킷의 SSRC별로 PCM 데이터를 누적
     */
    public void appendPcm(long ssrc, short[] samples) {
        PcmBuffer buffer = ssrcBuffers.computeIfAbsent(ssrc, k -> {
            int order = ssrcCount.getAndIncrement();
            if (order == 0) { rxSsrc = ssrc; }
            else if (order == 1) { txSsrc = ssrc; }
            log.info("SSRC 등록: callId={}, ssrc={}, channel={}",
                     callId, ssrc, order == 0 ? "RX" : "TX");
            return new PcmBuffer();
        });
        buffer.append(samples);
    }

    public PcmBuffer getRxBuffer() { return rxSsrc != null ? ssrcBuffers.get(rxSsrc) : null; }
    public PcmBuffer getTxBuffer() { return txSsrc != null ? ssrcBuffers.get(txSsrc) : null; }
}
```

**CallSessionManager.java**
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class CallSessionManager {

    // Call-ID → CallSession
    private final ConcurrentHashMap<String, CallSession> activeSessions = new ConcurrentHashMap<>();
    private final List<SessionEventListener> listeners;
    private final CallRecordRepository callRecordRepo;

    // 미디어 포트 → Call-ID 매핑 (RTP 패킷에서 세션 찾기 위함)
    private final ConcurrentHashMap<Integer, String> portToCallId = new ConcurrentHashMap<>();

    /**
     * SIP INVITE 수신 → 새 통화 세션 생성
     */
    public void onCallStart(SipMessage msg) {
        CallSession session = new CallSession(
            msg.getCallId(),
            msg.getFromUri(),    // 고객 번호
            msg.getToUri(),      // 상담사 내선
            LocalDateTime.now()
        );
        session.setState(CallState.CONNECTED);
        activeSessions.put(msg.getCallId(), session);
        portToCallId.put(msg.getMediaPort(), msg.getCallId());

        // 리스너에게 알림 (STT 스트리밍 시작 등)
        listeners.forEach(l -> l.onSessionCreated(session));

        log.info("통화 세션 생성: callId={}, caller={}", msg.getCallId(), msg.getFromUri());
    }

    /**
     * SIP BYE 수신 → 통화 세션 종료
     */
    public void onCallEnd(SipMessage msg) {
        CallSession session = activeSessions.remove(msg.getCallId());
        if (session == null) return;

        session.setState(CallState.COMPLETED);
        session.setEndTime(LocalDateTime.now());

        // 리스너에게 알림 (오디오 파일 생성, 배치 STT 등)
        listeners.forEach(l -> l.onSessionCompleted(session));

        // DB 저장
        saveCallRecord(session);

        log.info("통화 세션 종료: callId={}, duration={}s",
                 msg.getCallId(),
                 Duration.between(session.getStartTime(), session.getEndTime()).getSeconds());
    }

    /**
     * RTP 디코더에서 호출 — PCM 데이터를 세션에 전달
     */
    public void appendAudio(long ssrc, short[] pcmSamples, String srcIp, int srcPort) {
        // srcPort 또는 srcIp로 세션 탐색 (실제로는 더 정교한 매핑 필요)
        String callId = findCallIdByPort(srcPort);
        if (callId == null) return;

        CallSession session = activeSessions.get(callId);
        if (session != null && session.getState() == CallState.CONNECTED) {
            session.appendPcm(ssrc, pcmSamples);

            // 스트리밍 STT 리스너에게도 실시간 전달
            listeners.forEach(l -> l.onAudioChunk(session, ssrc, pcmSamples));
        }
    }

    public Collection<CallSession> getActiveSessions() {
        return Collections.unmodifiableCollection(activeSessions.values());
    }

    public int getActiveSessionCount() { return activeSessions.size(); }
}
```

---

### 6.6 모듈 5: 오디오 파일 생성 (audio)

#### 역할
통화 종료 후, 세션에 누적된 PCM 데이터를 WAV 파일로 저장합니다.

**PcmBuffer.java**
```java
/**
 * 스레드 안전한 PCM 샘플 버퍼
 * 20ms마다 160 샘플(short)이 누적됨
 */
public class PcmBuffer {
    // 초기 용량: 8000Hz * 600초(10분) = 4,800,000 샘플
    private final List<short[]> chunks = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger totalSamples = new AtomicInteger(0);

    public void append(short[] samples) {
        chunks.add(samples);
        totalSamples.addAndGet(samples.length);
    }

    /**
     * 모든 청크를 하나의 byte[] (16bit LE)로 병합
     */
    public byte[] toByteArray() {
        byte[] result = new byte[totalSamples.get() * 2]; // 16bit = 2bytes
        int offset = 0;
        for (short[] chunk : chunks) {
            for (short sample : chunk) {
                result[offset++] = (byte) (sample & 0xFF);         // Low byte
                result[offset++] = (byte) ((sample >> 8) & 0xFF);  // High byte
            }
        }
        return result;
    }

    public int getSampleCount() { return totalSamples.get(); }
}
```

**WavFileWriter.java**
```java
@Component
@Slf4j
public class WavFileWriter {

    private static final int SAMPLE_RATE = 8000;   // 8kHz
    private static final int BITS_PER_SAMPLE = 16;  // 16bit
    private static final int CHANNELS = 1;           // 모노

    /**
     * PCM 데이터를 WAV 파일로 저장
     * WAV = 44바이트 헤더 + PCM 데이터
     */
    public Path writeWav(byte[] pcmData, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             DataOutputStream dos = new DataOutputStream(fos)) {

            int dataSize = pcmData.length;
            int fileSize = 36 + dataSize;

            // WAV 헤더 (44 bytes)
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(fileSize));
            dos.writeBytes("WAVE");
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16));              // chunk size
            dos.writeShort(Short.reverseBytes((short) 1));       // PCM format
            dos.writeShort(Short.reverseBytes((short) CHANNELS));
            dos.writeInt(Integer.reverseBytes(SAMPLE_RATE));
            dos.writeInt(Integer.reverseBytes(SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8));
            dos.writeShort(Short.reverseBytes((short) (CHANNELS * BITS_PER_SAMPLE / 8)));
            dos.writeShort(Short.reverseBytes((short) BITS_PER_SAMPLE));
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(dataSize));

            // PCM 데이터
            dos.write(pcmData);
        }

        log.info("WAV 파일 생성: path={}, size={}KB", outputPath, pcmData.length / 1024);
        return outputPath;
    }
}
```

**AudioFileService.java** — SessionEventListener 구현
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class AudioFileService implements SessionEventListener {

    private final WavFileWriter wavWriter;
    private final AudioProperties audioProps;
    private final ExecutorService audioExecutor =
        Executors.newFixedThreadPool(4, r -> new Thread(r, "audio-writer"));

    @Override
    public void onSessionCompleted(CallSession session) {
        audioExecutor.submit(() -> generateAudioFiles(session));
    }

    private void generateAudioFiles(CallSession session) {
        try {
            String dateDir = session.getStartTime().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path baseDir = Path.of(audioProps.getBasePath(), dateDir);

            // RX (고객 음성) WAV 생성
            PcmBuffer rxBuf = session.getRxBuffer();
            if (rxBuf != null && rxBuf.getSampleCount() > 0) {
                wavWriter.writeWav(rxBuf.toByteArray(),
                    baseDir.resolve(session.getCallId() + "_rx.wav"));
            }

            // TX (상담사 음성) WAV 생성
            PcmBuffer txBuf = session.getTxBuffer();
            if (txBuf != null && txBuf.getSampleCount() > 0) {
                wavWriter.writeWav(txBuf.toByteArray(),
                    baseDir.resolve(session.getCallId() + "_tx.wav"));
            }

            log.info("오디오 파일 생성 완료: callId={}", session.getCallId());
        } catch (Exception e) {
            log.error("오디오 파일 생성 실패: callId={}", session.getCallId(), e);
        }
    }

    @Override
    public void onSessionCreated(CallSession session) { /* no-op */ }

    @Override
    public void onAudioChunk(CallSession session, long ssrc, short[] samples) { /* no-op */ }
}
```

---

### 6.7 모듈 6: Google STT 연동 — 어댑터 패턴 (stt)

#### 역할
음성 데이터를 텍스트로 변환합니다. 어댑터 패턴으로 다양한 STT 벤더를 교체 가능하게 설계합니다.

#### 어댑터 패턴 구조

```
┌──────────────────────────────────────────────────────────────┐
│                    STT 어댑터 패턴 구조                        │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│         <<interface>>                                        │
│        ┌──────────────┐                                      │
│        │  SttAdapter  │                                      │
│        ├──────────────┤                                      │
│        │ +recognize() │  ← 배치 방식 (파일 → 텍스트)           │
│        │ +stream()    │  ← 스트리밍 방식 (실시간 음성 → 텍스트) │
│        │ +getVendor() │  ← 벤더 이름 반환                     │
│        │ +isAvail()   │  ← 사용 가능 여부                     │
│        └──────┬───────┘                                      │
│               │ implements                                   │
│    ┌──────────┼──────────────┬────────────────┐              │
│    ▼          ▼              ▼                ▼              │
│ ┌────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐          │
│ │Google  │ │Naver     │ │Kakao     │ │Custom     │          │
│ │SttAdap.│ │SttAdap.  │ │SttAdap.  │ │SttAdap.   │          │
│ └────────┘ └──────────┘ └──────────┘ └───────────┘          │
│                                                              │
│  SttAdapterFactory: 설정에 따라 적절한 어댑터 생성              │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**SttAdapter.java** — 인터페이스
```java
/**
 * STT 어댑터 인터페이스
 * 모든 STT 벤더는 이 인터페이스를 구현해야 함
 */
public interface SttAdapter {

    /**
     * 배치 인식: 오디오 파일을 텍스트로 변환
     * @param audioFilePath WAV/PCM 파일 경로
     * @param languageCode  언어 코드 (예: "ko-KR")
     * @return STT 변환 결과
     */
    SttResult recognize(Path audioFilePath, String languageCode);

    /**
     * 스트리밍 인식: 실시간 오디오 청크를 텍스트로 변환
     * @param audioStream   PCM 오디오 스트림
     * @param languageCode  언어 코드
     * @param callback      결과 콜백 (중간 결과 + 최종 결과)
     * @return 스트리밍 세션 (stop() 호출로 종료)
     */
    SttStreamingSession startStreaming(String languageCode, SttResultCallback callback);

    /** 벤더 이름 (예: "GOOGLE", "NAVER", "CUSTOM") */
    String getVendorName();

    /** 현재 사용 가능한지 확인 (인증키 유효성 등) */
    boolean isAvailable();
}
```

**SttResult.java**
```java
@Data
@Builder
public class SttResult {
    private String transcript;        // 변환된 텍스트
    private float confidence;         // 신뢰도 (0.0 ~ 1.0)
    private boolean isFinal;          // 최종 결과 여부 (스트리밍에서 중간결과 구분)
    private long timestampMs;         // 결과 시간
    private String languageCode;      // 감지된 언어
}
```

**SttStreamingSession.java** — 스트리밍 세션
```java
public interface SttStreamingSession {
    /** PCM 오디오 청크를 STT에 전송 */
    void sendAudio(short[] pcmSamples);
    /** 스트리밍 세션 종료 */
    void stop();
    /** 세션 활성 여부 */
    boolean isActive();
}
```

**SttResultCallback.java** — 스트리밍 결과 콜백
```java
@FunctionalInterface
public interface SttResultCallback {
    void onResult(SttResult result);
}
```

**GoogleSttAdapter.java** — Google Cloud STT 구현
```java
@Component
@Slf4j
@RequiredArgsConstructor
public class GoogleSttAdapter implements SttAdapter {

    private final GoogleSttProperties props;

    @Override
    public SttResult recognize(Path audioFilePath, String languageCode) {
        try (SpeechClient speechClient = SpeechClient.create()) {
            byte[] audioBytes = Files.readAllBytes(audioFilePath);
            ByteString audioData = ByteString.copyFrom(audioBytes);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(8000)
                .setLanguageCode(languageCode)
                .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioData)
                .build();

            RecognizeResponse response = speechClient.recognize(config, audio);

            // 결과 조합
            StringBuilder transcript = new StringBuilder();
            float totalConfidence = 0;
            int count = 0;
            for (SpeechRecognitionResult result : response.getResultsList()) {
                SpeechRecognitionAlternative alt = result.getAlternativesList().get(0);
                transcript.append(alt.getTranscript());
                totalConfidence += alt.getConfidence();
                count++;
            }

            return SttResult.builder()
                .transcript(transcript.toString())
                .confidence(count > 0 ? totalConfidence / count : 0)
                .isFinal(true)
                .timestampMs(System.currentTimeMillis())
                .languageCode(languageCode)
                .build();
        } catch (Exception e) {
            log.error("Google STT 배치 인식 실패", e);
            throw new RuntimeException("STT 배치 인식 실패", e);
        }
    }

    @Override
    public SttStreamingSession startStreaming(String languageCode, SttResultCallback callback) {
        return new GoogleSttStreamingSession(props, languageCode, callback);
    }

    @Override
    public String getVendorName() { return "GOOGLE"; }

    @Override
    public boolean isAvailable() {
        return props.getCredentialPath() != null
            && Files.exists(Path.of(props.getCredentialPath()));
    }
}
```

**GoogleSttStreamingSession.java** — 스트리밍 세션 구현
```java
@Slf4j
public class GoogleSttStreamingSession implements SttStreamingSession {

    private final StreamingRecognizeRequest initialRequest;
    private final SttResultCallback callback;
    private ClientStream<StreamingRecognizeRequest> clientStream;
    private volatile boolean active = true;

    public GoogleSttStreamingSession(GoogleSttProperties props,
                                      String languageCode,
                                      SttResultCallback callback) {
        this.callback = callback;

        // 스트리밍 설정
        StreamingRecognitionConfig streamConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(8000)
                .setLanguageCode(languageCode)
                .setEnableAutomaticPunctuation(true)
                .build())
            .setInterimResults(true)  // 중간 결과도 받기
            .build();

        this.initialRequest = StreamingRecognizeRequest.newBuilder()
            .setStreamingConfig(streamConfig)
            .build();

        startStream();
    }

    private void startStream() {
        try {
            SpeechClient client = SpeechClient.create();
            ResponseObserver<StreamingRecognizeResponse> observer =
                new ResponseObserver<>() {
                    @Override
                    public void onResponse(StreamingRecognizeResponse response) {
                        for (StreamingRecognitionResult result : response.getResultsList()) {
                            StreamingRecognitionResult.Builder rb = result.toBuilder();
                            SttResult sttResult = SttResult.builder()
                                .transcript(result.getAlternatives(0).getTranscript())
                                .confidence(result.getAlternatives(0).getConfidence())
                                .isFinal(result.getIsFinal())
                                .timestampMs(System.currentTimeMillis())
                                .build();
                            callback.onResult(sttResult);
                        }
                    }
                    @Override public void onStart(StreamController ctrl) {}
                    @Override public void onError(Throwable t) {
                        log.error("Google STT 스트리밍 오류", t);
                        active = false;
                    }
                    @Override public void onComplete() { active = false; }
                };

            clientStream = client.streamingRecognizeCallable().splitCall(observer);
            clientStream.send(initialRequest);
        } catch (Exception e) {
            log.error("Google STT 스트리밍 시작 실패", e);
            active = false;
        }
    }

    @Override
    public void sendAudio(short[] pcmSamples) {
        if (!active) return;
        byte[] bytes = new byte[pcmSamples.length * 2];
        for (int i = 0; i < pcmSamples.length; i++) {
            bytes[i * 2] = (byte) (pcmSamples[i] & 0xFF);
            bytes[i * 2 + 1] = (byte) ((pcmSamples[i] >> 8) & 0xFF);
        }
        StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder()
            .setAudioContent(ByteString.copyFrom(bytes))
            .build();
        clientStream.send(request);
    }

    @Override
    public void stop() {
        active = false;
        if (clientStream != null) clientStream.closeSend();
    }

    @Override
    public boolean isActive() { return active; }
}
```

**SttAdapterFactory.java** — 팩토리 패턴으로 어댑터 선택
```java
@Component
@RequiredArgsConstructor
public class SttAdapterFactory {

    private final List<SttAdapter> adapters; // 스프링이 모든 구현체를 자동 주입

    /**
     * 벤더 이름으로 어댑터 조회
     * @param vendorName "GOOGLE", "NAVER", "CUSTOM" 등
     */
    public SttAdapter getAdapter(String vendorName) {
        return adapters.stream()
            .filter(a -> a.getVendorName().equalsIgnoreCase(vendorName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "지원하지 않는 STT 벤더: " + vendorName));
    }

    /** 사용 가능한 어댑터 목록 */
    public List<String> getAvailableVendors() {
        return adapters.stream()
            .filter(SttAdapter::isAvailable)
            .map(SttAdapter::getVendorName)
            .toList();
    }
}
```

---

### 6.8 모듈 7: REST API (api)

**CallController.java**
```java
@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallSessionManager sessionManager;
    private final CallRecordRepository callRecordRepo;

    /** 현재 진행 중인 통화 목록 */
    @GetMapping("/active")
    public List<CallInfoResponse> getActiveCalls() {
        return sessionManager.getActiveSessions().stream()
            .map(CallInfoResponse::from)
            .toList();
    }

    /** 통화 이력 조회 (페이징) */
    @GetMapping("/history")
    public Page<CallRecord> getCallHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return callRecordRepo.findAllByOrderByStartTimeDesc(PageRequest.of(page, size));
    }

    /** 특정 통화의 STT 결과 조회 */
    @GetMapping("/{callId}/transcript")
    public List<SttTranscript> getTranscript(@PathVariable String callId) {
        return sttTranscriptRepo.findByCallIdOrderByTimestampAsc(callId);
    }
}
```

**SttController.java**
```java
@RestController
@RequestMapping("/api/stt")
@RequiredArgsConstructor
public class SttController {

    private final SttAdapterFactory sttAdapterFactory;

    /** 배치 STT: 파일 기반 변환 */
    @PostMapping("/recognize")
    public SttResultResponse recognize(
            @RequestParam String filePath,
            @RequestParam(defaultValue = "ko-KR") String lang,
            @RequestParam(defaultValue = "GOOGLE") String vendor) {
        SttAdapter adapter = sttAdapterFactory.getAdapter(vendor);
        SttResult result = adapter.recognize(Path.of(filePath), lang);
        return SttResultResponse.from(result);
    }

    /** 사용 가능한 STT 벤더 목록 */
    @GetMapping("/vendors")
    public List<String> getAvailableVendors() {
        return sttAdapterFactory.getAvailableVendors();
    }
}
```

**SettingController.java**
```java
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SttVendorConfigRepository configRepo;

    /** 현재 STT 설정 조회 */
    @GetMapping("/stt")
    public SttVendorConfig getSttConfig() {
        return configRepo.findFirstByOrderByIdAsc()
            .orElse(SttVendorConfig.defaultConfig());
    }

    /** STT 벤더 변경 */
    @PutMapping("/stt")
    public SttVendorConfig updateSttConfig(@RequestBody @Valid SttSettingRequest request) {
        SttVendorConfig config = configRepo.findFirstByOrderByIdAsc()
            .orElse(new SttVendorConfig());
        config.setVendorName(request.getVendorName());
        config.setLanguageCode(request.getLanguageCode());
        config.setStreamingEnabled(request.isStreamingEnabled());
        config.setCredentialPath(request.getCredentialPath());
        return configRepo.save(config);
    }
}
```

---

### 6.9 모듈 8: 실시간 WebSocket 푸시 (websocket)

#### 역할
STT 스트리밍 결과를 브라우저에 실시간으로 전송합니다.

**WebSocketConfig.java**
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SttWebSocketHandler sttHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sttHandler, "/ws/stt")
                .setAllowedOrigins("*");
    }
}
```

**SttWebSocketHandler.java**
```java
@Component
@Slf4j
public class SttWebSocketHandler extends TextWebSocketHandler implements SessionEventListener {

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) {
        sessionRegistry.register(wsSession);
        log.info("WebSocket 연결: sessionId={}", wsSession.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) {
        sessionRegistry.unregister(wsSession);
    }

    /**
     * STT 스트리밍 결과를 모든 WebSocket 클라이언트에 브로드캐스트
     */
    public void broadcastSttResult(String callId, SttResult result) {
        String message = String.format(
            "{\"callId\":\"%s\",\"text\":\"%s\",\"isFinal\":%b,\"confidence\":%.2f}",
            callId, result.getTranscript(), result.isFinal(), result.getConfidence()
        );

        sessionRegistry.getAll().forEach(ws -> {
            try {
                if (ws.isOpen()) {
                    ws.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.warn("WebSocket 전송 실패: {}", ws.getId());
            }
        });
    }

    // SessionEventListener 구현
    @Override
    public void onAudioChunk(CallSession session, long ssrc, short[] samples) {
        // 이 메서드는 SttStreamingService에서 호출됨
        // STT 결과가 나오면 broadcastSttResult()로 전달
    }
}
```

---

### 6.10 대시보드 (Thymeleaf)

**DashboardController.java**
```java
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CallSessionManager sessionManager;
    private final SttAdapterFactory adapterFactory;
    private final SttVendorConfigRepository configRepo;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("activeCalls", sessionManager.getActiveSessions());
        model.addAttribute("activeCount", sessionManager.getActiveSessionCount());
        return "dashboard";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("currentConfig", configRepo.findFirstByOrderByIdAsc()
            .orElse(SttVendorConfig.defaultConfig()));
        model.addAttribute("vendors", adapterFactory.getAvailableVendors());
        return "settings";
    }
}
```

**dashboard.html** — 핵심 구조
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>STTGateway 대시보드</title>
    <link rel="stylesheet" th:href="@{/css/dashboard.css}"/>
</head>
<body>
    <header>
        <h1>STTGateway 대시보드</h1>
        <a href="/settings">STT 설정</a>
    </header>

    <!-- 현재 진행중 통화 목록 -->
    <section id="active-calls">
        <h2>진행중 통화 (<span id="call-count" th:text="${activeCount}">0</span>건)</h2>
        <div id="call-list"></div>
    </section>

    <!-- 실시간 STT 텍스트 표시 영역 -->
    <section id="stt-output">
        <h2>실시간 STT 변환 결과</h2>
        <div id="transcript-container">
            <!-- WebSocket으로 수신되는 텍스트가 여기에 표시됨 -->
        </div>
    </section>

    <script th:src="@{/js/websocket-client.js}"></script>
    <script th:src="@{/js/dashboard.js}"></script>
</body>
</html>
```

**websocket-client.js** — WebSocket 클라이언트
```javascript
const ws = new WebSocket(`ws://${window.location.host}/ws/stt`);

ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    const container = document.getElementById('transcript-container');

    // callId별 영역 생성 또는 업데이트
    let callDiv = document.getElementById(`call-${data.callId}`);
    if (!callDiv) {
        callDiv = document.createElement('div');
        callDiv.id = `call-${data.callId}`;
        callDiv.className = 'call-transcript';
        callDiv.innerHTML = `<h3>통화: ${data.callId}</h3><div class="text-area"></div>`;
        container.prepend(callDiv);
    }

    const textArea = callDiv.querySelector('.text-area');
    if (data.isFinal) {
        // 최종 결과: 확정 텍스트로 추가
        const p = document.createElement('p');
        p.className = 'final-text';
        p.textContent = data.text;
        textArea.appendChild(p);
    } else {
        // 중간 결과: 임시 텍스트 업데이트
        let interim = textArea.querySelector('.interim-text');
        if (!interim) {
            interim = document.createElement('p');
            interim.className = 'interim-text';
            textArea.appendChild(interim);
        }
        interim.textContent = data.text;
    }
};

ws.onclose = function() {
    console.log('WebSocket 연결 종료. 3초 후 재연결...');
    setTimeout(() => { location.reload(); }, 3000);
};
```

---

## 7. 성능 최적화 및 디스크 I/O 대응

### 7.1 하루 1,000콜 부하 분석

| 항목 | 계산 | 결과 |
|------|------|------|
| 평균 통화 시간 | 5분 가정 | 300초 |
| 동시 통화 최대치 | 50콜 (피크) | — |
| RTP 패킷/초 (1통화) | 8000Hz / 160샘플 | 50패킷/초 |
| RTP 패킷/초 (50동시) | 50 × 50 × 2(RX+TX) | 5,000패킷/초 |
| 음성 데이터/초 (50동시) | 50 × 2 × 8KB | 800KB/초 |
| 1콜 WAV 크기 (5분) | 300 × 16KB (RX+TX) | 약 4.8MB |
| 일일 총 WAV 용량 | 1,000 × 4.8MB | 약 4.7GB |

### 7.2 디스크 I/O 최적화 전략

```
┌────────────────────────────────────────────────────────┐
│              디스크 I/O 최적화 전략                       │
├────────────────────────────────────────────────────────┤
│                                                        │
│  1. 비동기 쓰기 (Async Write)                           │
│     ┌─────────────────────────────────────────────┐    │
│     │ 패킷캡처 스레드 → [메모리 큐] → 별도 쓰기 스레드 │   │
│     │                                             │    │
│     │ 캡처 스레드가 디스크 I/O에 블로킹되지 않음       │    │
│     └─────────────────────────────────────────────┘    │
│                                                        │
│  2. 메모리 버퍼링                                       │
│     ┌─────────────────────────────────────────────┐    │
│     │ PCM 데이터를 메모리(PcmBuffer)에 누적            │    │
│     │ → 통화 종료 시 한번에 파일로 기록                  │    │
│     │ → 중간에 디스크 쓰기 없음 (메모리만 사용)          │    │
│     └─────────────────────────────────────────────┘    │
│                                                        │
│  3. pcap 덤프 선택적 저장                                │
│     ┌─────────────────────────────────────────────┐    │
│     │ application.yml에서 on/off 설정 가능              │    │
│     │ sttgw.capture.dump-enabled: false (운영 시)      │    │
│     │ 디버깅 필요시에만 활성화                           │    │
│     └─────────────────────────────────────────────┘    │
│                                                        │
│  4. 로그 레벨 관리                                       │
│     ┌─────────────────────────────────────────────┐    │
│     │ 운영: INFO 레벨 (최소 로그)                       │    │
│     │ 개발: DEBUG 레벨 (상세 로그)                      │    │
│     │ RTP 패킷 로그: TRACE 이하에서만 출력               │    │
│     └─────────────────────────────────────────────┘    │
│                                                        │
│  5. 일자별 디렉토리 분리 + 자동 정리                      │
│     ┌─────────────────────────────────────────────┐    │
│     │ /APP_DATA/audio/2026/04/07/ ← 일자별 분리        │    │
│     │ 30일 경과 파일 자동 삭제 (스케줄러)                │    │
│     └─────────────────────────────────────────────┘    │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### 7.3 네트워크 부하 최적화

| 전략 | 설명 |
|------|------|
| **Google STT 스트리밍 연결 풀링** | 50 동시콜 = 최대 100개 스트리밍 세션 (RX+TX). gRPC 커넥션 풀 관리 |
| **STT 호출 간격 조절** | 매 20ms가 아닌, 200~300ms 단위로 오디오 버퍼를 모아서 전송 |
| **VAD(Voice Activity Detection)** | 무음 구간은 STT에 보내지 않음 (네트워크 절약) |
| **스트리밍/배치 하이브리드** | 실시간 모니터링이 불필요한 콜은 배치 처리로 전환 |

### 7.4 application.yml (핵심 설정)

```yaml
server:
  port: 8080

spring:
  application:
    name: sttgw
  datasource:
    url: jdbc:h2:file:/APP_DATA/db/sttgw;AUTO_SERVER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  thymeleaf:
    cache: false  # 개발 시

# STTGateway 커스텀 설정
sttgw:
  capture:
    interface-name: eth3
    snap-len: 65536
    timeout-millis: 10
    dump-path: /APP_DATA/DUMP
    dump-enabled: true
    bpf-filter: udp
  audio:
    base-path: /APP_DATA/audio
    retention-days: 30
  stt:
    default-vendor: GOOGLE
    default-language: ko-KR
    streaming-enabled: true
    google:
      credential-path: /APP/sttgw/credentials/google-stt-key.json
      streaming-buffer-ms: 200

# 로그 설정
logging:
  file:
    path: /APP_LOGS/sttgw
    name: /APP_LOGS/sttgw/sttgw.log
  level:
    root: INFO
    com.oxjohs.sttgw: DEBUG
    com.oxjohs.sttgw.capture: INFO
    com.oxjohs.sttgw.rtp: INFO
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

## 8. 스트리밍 STT 통합 플로우

전체 데이터 흐름을 시간 순서대로 정리합니다.

```
시간 ──────────────────────────────────────────────────────────────▶

[1] 고객 전화 → IPCC → GS1900 미러링 → eth3

[2] SIP INVITE 패킷 수신
    → SipParser: Call-ID, From(고객번호) 추출
    → CallSessionManager: 새 CallSession 생성
    → SttStreamingService: Google STT 스트리밍 세션 시작

[3] RTP 패킷 수신 (20ms 간격으로 계속)
    → RtpDecoder: G.711 → PCM 변환, SSRC로 RX/TX 구분
    → CallSession.appendPcm(): 메모리 버퍼에 누적
    → SttStreamingService: PCM 청크를 Google STT로 전송

[4] Google STT 중간 결과 수신
    → SttWebSocketHandler.broadcastSttResult()
    → 브라우저 대시보드에 실시간 텍스트 표시 (중간 결과)

[5] Google STT 최종 결과 수신
    → DB 저장 (SttTranscript)
    → 브라우저 대시보드에 확정 텍스트 표시

[6] SIP BYE 패킷 수신
    → CallSessionManager: 세션 종료
    → AudioFileService: PCM → WAV 파일 저장 (RX/TX 분리)
    → 배치 STT도 필요시 실행 (통화 후 재처리)
    → DB에 CallRecord 저장
```

---

## 9. 추후 확장 계획

| 영역 | 확장 내용 |
|------|-----------|
| **STT 벤더 추가** | SttAdapter 인터페이스 구현체만 추가하면 됨 |
| **TTS 연동** | 텍스트→음성 변환 (향후 AI 응답 자동 생성 시) |
| **NLU/감정 분석** | STT 결과 텍스트를 NLU 엔진으로 분석 |
| **실시간 알림** | 특정 키워드 감지 시 관리자 알림 |
| **클러스터링** | 다중 서버 구성 시 Redis 기반 세션 공유 |
| **녹취 연동** | 기존 IPCC 녹취 시스템과 메타데이터 연동 |
