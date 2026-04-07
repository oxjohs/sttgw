# sttgw — 배포 가이드

## 1. 대상 서버 정보

| 항목 | 값 |
|------|-----|
| 서버 IP | 211.192.89.96 (가상서버) |
| OS | Linux (CentOS/Ubuntu) |
| 패킷 캡처 인터페이스 | eth3 (이더넷 4번) |
| Java | OpenJDK 17 |
| 포트 | 8080 (HTTP), WebSocket 동일 포트 |

## 2. 사전 준비

### 2.1 서버 패키지 설치

```bash
# Java 17
sudo yum install -y java-17-openjdk java-17-openjdk-devel   # CentOS
# 또는
sudo apt install -y openjdk-17-jdk                            # Ubuntu

# libpcap (패킷 캡처용)
sudo yum install -y libpcap libpcap-devel    # CentOS
# 또는
sudo apt install -y libpcap-dev              # Ubuntu

# 확인
java -version       # openjdk 17.x.x
tcpdump -ni eth3 -c 5   # 패킷 미러링 수신 확인
```

### 2.2 디렉토리 생성

```bash
sudo mkdir -p /APP/sttgw/credentials
sudo mkdir -p /APP/sttgw/bin
sudo mkdir -p /APP_DATA/DUMP
sudo mkdir -p /APP_DATA/audio
sudo mkdir -p /APP_DATA/db
sudo mkdir -p /APP_LOGS/sttgw

# 실행 사용자에게 권한 부여 (예: sttgw 사용자)
sudo useradd -r -s /bin/false sttgw
sudo chown -R sttgw:sttgw /APP/sttgw /APP_DATA /APP_LOGS/sttgw
```

### 2.3 Google Cloud STT 인증키

1. Google Cloud Console → IAM → 서비스 계정 생성
2. Cloud Speech-to-Text API 활성화
3. JSON 키 파일 다운로드
4. 서버에 배치:

```bash
scp google-stt-key.json root@211.192.89.96:/APP/sttgw/credentials/
chmod 600 /APP/sttgw/credentials/google-stt-key.json
chown sttgw:sttgw /APP/sttgw/credentials/google-stt-key.json
```

### 2.4 패킷 캡처 권한

Pcap4J가 eth3에서 패킷을 읽으려면 root 권한 또는 `CAP_NET_RAW` 능력이 필요합니다.

```bash
# 방법 1: Java 바이너리에 capability 부여 (권장)
sudo setcap cap_net_raw,cap_net_admin=eip $(readlink -f $(which java))

# 방법 2: 전용 사용자로 실행 시 capability
sudo setcap cap_net_raw+ep /usr/lib/jvm/java-17-openjdk/bin/java
```

## 3. 빌드

### 3.1 로컬 빌드

```bash
cd sttgw/
./gradlew clean bootJar

# 결과물: build/libs/sttgw-1.0.0.jar
```

### 3.2 서버 배포

```bash
# JAR 전송
scp build/libs/sttgw-1.0.0.jar root@211.192.89.96:/APP/sttgw/

# 운영 설정 파일 배치 (JAR 외부 설정 — 최우선 적용)
scp src/main/resources/application-prod.yml root@211.192.89.96:/APP/sttgw/application.yml
```

## 4. 실행 스크립트

### 4.1 start.sh

```bash
#!/bin/bash
# /APP/sttgw/bin/start.sh

APP_NAME="sttgw"
APP_HOME="/APP/sttgw"
JAR_FILE="${APP_HOME}/sttgw-1.0.0.jar"
LOG_DIR="/APP_LOGS/sttgw"
PID_FILE="${APP_HOME}/sttgw.pid"

# Google Cloud 인증 환경변수
export GOOGLE_APPLICATION_CREDENTIALS="${APP_HOME}/credentials/google-stt-key.json"

# JVM 옵션
JAVA_OPTS="-Xms512m -Xmx1024m"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -Djava.net.preferIPv4Stack=true"
JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=UTF-8"

# 이미 실행 중인지 확인
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "${APP_NAME} 이미 실행 중 (PID: ${PID})"
        exit 1
    fi
fi

# 실행
echo "${APP_NAME} 시작..."
nohup java ${JAVA_OPTS} \
    -jar "${JAR_FILE}" \
    --spring.profiles.active=prod \
    --spring.config.additional-location=file:${APP_HOME}/application.yml \
    > "${LOG_DIR}/startup.log" 2>&1 &

echo $! > "$PID_FILE"
echo "${APP_NAME} 시작됨 (PID: $(cat $PID_FILE))"
```

### 4.2 stop.sh

```bash
#!/bin/bash
# /APP/sttgw/bin/stop.sh

APP_NAME="sttgw"
PID_FILE="/APP/sttgw/sttgw.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "${APP_NAME} PID 파일 없음"
    exit 1
fi

PID=$(cat "$PID_FILE")
echo "${APP_NAME} 종료 중 (PID: ${PID})..."

kill "$PID"

# 30초 대기 후 강제 종료
TIMEOUT=30
while [ $TIMEOUT -gt 0 ]; do
    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo "${APP_NAME} 정상 종료됨"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
    TIMEOUT=$((TIMEOUT - 1))
done

echo "${APP_NAME} 강제 종료..."
kill -9 "$PID"
rm -f "$PID_FILE"
```

### 4.3 스크립트 권한

```bash
chmod +x /APP/sttgw/bin/start.sh
chmod +x /APP/sttgw/bin/stop.sh
```

## 5. 운영 프로파일 설정 (application-prod.yml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:/APP_DATA/db/sttgw;AUTO_SERVER=TRUE
  h2:
    console:
      enabled: false    # 운영에서는 비활성화
  jpa:
    hibernate:
      ddl-auto: validate  # 운영에서는 validate만
    show-sql: false
  thymeleaf:
    cache: true

sttgw:
  capture:
    interface-name: eth3
    dump-enabled: false    # 운영에서는 pcap 저장 비활성화
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

logging:
  file:
    path: /APP_LOGS/sttgw
    name: /APP_LOGS/sttgw/sttgw.log
  level:
    root: INFO
    com.oxjohs.sttgw: INFO
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
```

## 6. 서비스 등록 (systemd)

```bash
sudo vi /etc/systemd/system/sttgw.service
```

```ini
[Unit]
Description=STTGateway Application
After=network.target

[Service]
Type=simple
User=sttgw
Group=sttgw
Environment="GOOGLE_APPLICATION_CREDENTIALS=/APP/sttgw/credentials/google-stt-key.json"
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -XX:+UseG1GC \
    -Dfile.encoding=UTF-8 \
    -jar /APP/sttgw/sttgw-1.0.0.jar \
    --spring.profiles.active=prod \
    --spring.config.additional-location=file:/APP/sttgw/application.yml
ExecStop=/bin/kill -TERM $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=append:/APP_LOGS/sttgw/startup.log
StandardError=append:/APP_LOGS/sttgw/startup.log
AmbientCapabilities=CAP_NET_RAW

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable sttgw
sudo systemctl start sttgw
sudo systemctl status sttgw
```

## 7. 검증 절차

### 7.1 기동 확인

```bash
# 프로세스 확인
ps -ef | grep sttgw

# 로그 확인
tail -f /APP_LOGS/sttgw/sttgw.log

# 헬스체크
curl http://localhost:8080/actuator/health
# 기대: {"status":"UP"}

# 대시보드 접근
curl -s http://localhost:8080/ | head -20
```

### 7.2 패킷 캡처 확인

```bash
# eth3에서 패킷이 수신되는지 확인
tcpdump -ni eth3 -c 20

# 로그에서 패킷 캡처 시작 메시지 확인
grep "패킷 캡처 시작" /APP_LOGS/sttgw/sttgw.log
```

### 7.3 STT 연동 확인

```bash
# 사용 가능한 STT 벤더 목록
curl http://localhost:8080/api/stt/vendors

# 테스트 통화 후 결과 확인
curl http://localhost:8080/api/calls/active
```

## 8. 운영 모니터링

| 항목 | 확인 방법 |
|------|-----------|
| 애플리케이션 상태 | `curl localhost:8080/actuator/health` |
| 메트릭 | `curl localhost:8080/actuator/metrics` |
| 활성 통화 수 | `curl localhost:8080/api/calls/active` |
| 로그 | `tail -f /APP_LOGS/sttgw/sttgw.log` |
| 디스크 사용량 | `df -h /APP_DATA` |
| 메모리 | `curl localhost:8080/actuator/metrics/jvm.memory.used` |

## 9. 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| 패킷 캡처 안 됨 | 권한 부족 | `setcap cap_net_raw+ep` 확인 |
| STT 결과 없음 | Google 인증키 문제 | `GOOGLE_APPLICATION_CREDENTIALS` 환경변수 및 파일 존재 확인 |
| WebSocket 끊김 | 프록시/방화벽 | WebSocket 업그레이드 허용 확인 |
| 디스크 부족 | 오디오/로그 누적 | retention-days 설정 및 스케줄러 확인 |
| H2 잠금 오류 | 다중 프로세스 접근 | `AUTO_SERVER=TRUE` 설정 확인, 중복 실행 방지 |
