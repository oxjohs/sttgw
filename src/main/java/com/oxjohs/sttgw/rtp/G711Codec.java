package com.oxjohs.sttgw.rtp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class G711Codec {

    public short[] alawToPcm(byte[] alawData) {
        short[] pcm = new short[alawData.length];
        for (int i = 0; i < alawData.length; i++) {
            pcm[i] = decodeAlaw(alawData[i]);
        }
        return pcm;
    }

    public short[] ulawToPcm(byte[] ulawData) {
        short[] pcm = new short[ulawData.length];
        for (int i = 0; i < ulawData.length; i++) {
            pcm[i] = decodeUlaw(ulawData[i]);
        }
        return pcm;
    }

    private short decodeAlaw(byte value) {
        int input = (value ^ 0x55) & 0xFF;
        int mantissa = (input & 0x0F) << 4;
        int segment = (input & 0x70) >> 4;

        int sample;
        if (segment == 0) {
            sample = mantissa + 8;
        } else {
            sample = (mantissa + 0x108) << (segment - 1);
        }

        return (short) ((input & 0x80) == 0 ? -sample : sample);
    }

    private short decodeUlaw(byte value) {
        int input = (~value) & 0xFF;
        int sign = input & 0x80;
        int exponent = (input >> 4) & 0x07;
        int mantissa = input & 0x0F;
        int sample = ((mantissa << 3) + 0x84) << exponent;
        sample -= 0x84;
        return (short) (sign == 0 ? sample : -sample);
    }
}
