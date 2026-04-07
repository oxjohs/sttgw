package com.oxjohs.sttgw.rtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oxjohs.sttgw.common.SttgwException;
import org.junit.jupiter.api.Test;

class RtpPacketTest {

    @Test
    void RTP_헤더에서_버전_코덱_시퀀스_타임스탬프_SSRC를_정상_추출한다() {
        byte[] data = new byte[] {
            (byte) 0x80, (byte) 0x08,
            0x12, 0x34,
            0x01, 0x02, 0x03, 0x04,
            0x11, 0x22, 0x33, 0x44,
            0x55, 0x66, 0x77
        };

        RtpPacket packet = RtpPacket.parse(data);

        assertThat(packet.getVersion()).isEqualTo(2);
        assertThat(packet.getPayloadType()).isEqualTo(8);
        assertThat(packet.getSequenceNumber()).isEqualTo(0x1234);
        assertThat(packet.getTimestamp()).isEqualTo(0x01020304L);
        assertThat(packet.getSsrc()).isEqualTo(0x11223344L);
        assertThat(packet.getPayload()).containsExactly(0x55, 0x66, 0x77);
    }

    @Test
    void 헤더보다_짧은_RTP_데이터면_예외가_발생한다() {
        assertThatThrownBy(() -> RtpPacket.parse(new byte[] {0x01, 0x02}))
            .isInstanceOf(SttgwException.class)
            .hasMessageContaining("유효하지 않은 RTP 패킷");
    }
}
