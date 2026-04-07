package com.oxjohs.sttgw.capture;

import com.oxjohs.sttgw.rtp.RtpDecoder;
import com.oxjohs.sttgw.session.CallSessionManager;
import com.oxjohs.sttgw.sip.SipParser;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.BpfProgram;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PacketCaptureService implements SmartLifecycle {

    private final CaptureProperties captureProperties;
    private final SipParser sipParser;
    private final RtpDecoder rtpDecoder;
    private final CallSessionManager callSessionManager;
    private final PacketDumpWriter packetDumpWriter;

    private volatile boolean running;
    private volatile PcapHandle handle;
    private volatile Thread captureThread;

    @Override
    public void start() {
        if (!captureProperties.isEnabled()) {
            log.info("패킷 캡처 비활성화 상태로 시작 생략");
            return;
        }
        if (running) {
            return;
        }

        running = true;
        captureThread = new Thread(this::captureLoop, "packet-capture");
        captureThread.setDaemon(true);
        captureThread.start();
        log.info("패킷 캡처 시작: interface={}, filter={}",
            captureProperties.getInterfaceName(), captureProperties.getBpfFilter());
    }

    @Override
    public void stop() {
        running = false;
        closeHandle();

        Thread currentThread = captureThread;
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return captureProperties.isEnabled();
    }

    private void captureLoop() {
        try {
            PcapNetworkInterface networkInterface = Pcaps.getDevByName(captureProperties.getInterfaceName());
            if (networkInterface == null) {
                log.error("캡처 인터페이스를 찾을 수 없습니다: interface={}", captureProperties.getInterfaceName());
                return;
            }

            handle = networkInterface.openLive(
                captureProperties.getSnapLen(),
                PcapNetworkInterface.PromiscuousMode.PROMISCUOUS,
                captureProperties.getTimeoutMillis()
            );
            handle.setFilter(captureProperties.getBpfFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);

            while (running) {
                Packet packet = handle.getNextPacket();
                if (packet == null) {
                    continue;
                }
                routePacket(packet);
            }
        } catch (PcapNativeException exception) {
            log.error("패킷 캡처 초기화 실패: {}", exception.getMessage(), exception);
        } catch (NotOpenException exception) {
            if (running) {
                log.error("패킷 캡처 중 핸들 오류: {}", exception.getMessage(), exception);
            } else {
                log.debug("패킷 캡처 핸들 종료");
            }
        } catch (RuntimeException exception) {
            log.error("패킷 캡처 런타임 오류: {}", exception.getMessage(), exception);
        } finally {
            running = false;
            closeHandle();
            log.info("패킷 캡처 루프 종료");
        }
    }

    private void routePacket(Packet packet) {
        if (captureProperties.isDumpEnabled()) {
            packetDumpWriter.writeAsync(packet);
        }

        UdpPacket udpPacket = packet.get(UdpPacket.class);
        if (udpPacket == null || udpPacket.getPayload() == null) {
            return;
        }

        byte[] payload = udpPacket.getPayload().getRawData();
        if (payload.length == 0) {
            return;
        }

        int dstPort = udpPacket.getHeader().getDstPort().valueAsInt();
        if (isSipPacket(dstPort, payload)) {
            sipParser.parse(payload, callSessionManager);
            return;
        }

        IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
        if (ipV4Packet == null) {
            log.debug("IPv4 헤더 없는 UDP 패킷 무시: dstPort={}", dstPort);
            return;
        }

        String srcIp = ipV4Packet.getHeader().getSrcAddr().getHostAddress();
        rtpDecoder.decode(payload, srcIp, dstPort);
    }

    private boolean isSipPacket(int port, byte[] payload) {
        if (port == 5060) {
            return true;
        }

        String header = new String(payload, 0, Math.min(12, payload.length), StandardCharsets.US_ASCII)
            .toUpperCase();
        return header.startsWith("INVITE")
            || header.startsWith("SIP/")
            || header.startsWith("BYE")
            || header.startsWith("ACK")
            || header.startsWith("CANCEL")
            || header.startsWith("OPTIONS")
            || header.startsWith("REGISTER");
    }

    private void closeHandle() {
        PcapHandle currentHandle = handle;
        handle = null;
        if (currentHandle != null && currentHandle.isOpen()) {
            currentHandle.close();
        }
    }
}
