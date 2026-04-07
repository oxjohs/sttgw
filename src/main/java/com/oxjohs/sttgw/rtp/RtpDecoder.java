package com.oxjohs.sttgw.rtp;

import com.oxjohs.sttgw.session.CallSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RtpDecoder {

    private final G711Codec g711Codec;
    private final CallSessionManager callSessionManager;

    public void decode(byte[] rawData, String srcIp, int srcPort) {
        try {
            RtpPacket rtpPacket = RtpPacket.parse(rawData);
            short[] pcmSamples = decodePayload(rtpPacket);
            if (pcmSamples == null) {
                return;
            }

            callSessionManager.appendAudio(rtpPacket.getSsrc(), pcmSamples, srcIp, srcPort);
        } catch (Exception exception) {
            log.warn("RTP 디코딩 실패: srcIp={}, srcPort={}, message={}", srcIp, srcPort, exception.getMessage());
        }
    }

    private short[] decodePayload(RtpPacket rtpPacket) {
        if (rtpPacket.getPayloadType() == 8) {
            return g711Codec.alawToPcm(rtpPacket.getPayload());
        }
        if (rtpPacket.getPayloadType() == 0) {
            return g711Codec.ulawToPcm(rtpPacket.getPayload());
        }

        log.warn("지원하지 않는 코덱: PT={}", rtpPacket.getPayloadType());
        return null;
    }
}
