package com.oxjohs.sttgw.rtp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class G711CodecTest {

    private final G711Codec codec = new G711Codec();

    @Test
    void G711_Alaw_데이터를_16bit_PCM으로_변환한다() {
        short[] pcm = codec.alawToPcm(new byte[] {(byte) 0xD5, (byte) 0x55});

        assertThat(pcm).hasSize(2);
        assertThat(pcm[0]).isEqualTo((short) 8);
        assertThat(pcm[1]).isEqualTo((short) -8);
    }

    @Test
    void G711_Ulaw_데이터를_16bit_PCM으로_변환한다() {
        short[] pcm = codec.ulawToPcm(new byte[] {(byte) 0xFF, (byte) 0x00});

        assertThat(pcm).hasSize(2);
        assertThat(pcm[0]).isEqualTo((short) 0);
        assertThat(pcm[1]).isEqualTo((short) -32124);
    }
}
