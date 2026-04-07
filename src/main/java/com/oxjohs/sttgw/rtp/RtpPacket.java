package com.oxjohs.sttgw.rtp;

import com.oxjohs.sttgw.common.SttgwException;
import java.util.Arrays;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class RtpPacket {

    private int version;
    private int payloadType;
    private int sequenceNumber;
    private long timestamp;
    private long ssrc;
    private byte[] payload;

    public static RtpPacket parse(byte[] data) {
        if (data == null || data.length < 12) {
            throw new SttgwException("유효하지 않은 RTP 패킷입니다");
        }

        RtpPacket packet = new RtpPacket();
        packet.version = (data[0] >> 6) & 0x03;
        packet.payloadType = data[1] & 0x7F;
        packet.sequenceNumber = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        packet.timestamp = ((long) (data[4] & 0xFF) << 24)
            | ((long) (data[5] & 0xFF) << 16)
            | ((long) (data[6] & 0xFF) << 8)
            | (data[7] & 0xFF);
        packet.ssrc = ((long) (data[8] & 0xFF) << 24)
            | ((long) (data[9] & 0xFF) << 16)
            | ((long) (data[10] & 0xFF) << 8)
            | (data[11] & 0xFF);
        packet.payload = Arrays.copyOfRange(data, 12, data.length);
        return packet;
    }
}
