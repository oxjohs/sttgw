package com.oxjohs.sttgw.sip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.oxjohs.sttgw.session.CallSessionManager;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SipParserTest {

    private final SipParser sipParser = new SipParser();

    @Test
    void SIP_INVITE_패킷에서_CallID와_SDP를_정상_추출한다() {
        SipMessage message = sipParser.parseSipMessage(inviteMessage());

        assertThat(message).isNotNull();
        assertThat(message.getMethod()).isEqualTo(SipMethodType.INVITE);
        assertThat(message.getCallId()).isEqualTo("call-1234@ipcc");
        assertThat(message.getFromUri()).isEqualTo("07041405642");
        assertThat(message.getToUri()).isEqualTo("1001");
        assertThat(message.getFromTag()).isEqualTo("from-tag");
        assertThat(message.getToTag()).isEqualTo("to-tag");
        assertThat(message.getMediaPort()).isEqualTo(10000);
        assertThat(message.getMediaIp()).isEqualTo("211.192.89.43");
        assertThat(message.getCodec()).isEqualTo("8");
        assertThat(message.getHeaders()).containsEntry("Call-ID", "call-1234@ipcc");
    }

    @Test
    void SIP_BYE_메서드를_정상_구분한다() {
        SipMessage message = sipParser.parseSipMessage(byeMessage());

        assertThat(message).isNotNull();
        assertThat(message.getMethod()).isEqualTo(SipMethodType.BYE);
        assertThat(message.getCallId()).isEqualTo("call-1234@ipcc");
    }

    @Test
    void SIP_CANCEL_메서드를_정상_구분한다() {
        SipMessage message = sipParser.parseSipMessage(cancelMessage());

        assertThat(message).isNotNull();
        assertThat(message.getMethod()).isEqualTo(SipMethodType.CANCEL);
        assertThat(message.getCallId()).isEqualTo("call-1234@ipcc");
    }

    @Test
    void 잘못된_SIP_형식이면_null을_반환한다() {
        assertThat(sipParser.parseSipMessage("not-a-sip-message")).isNull();
    }

    @Test
    void parse는_INVITE를_세션관리자_onCallStart로_전달한다() {
        CallSessionManager sessionManager = mock(CallSessionManager.class);

        sipParser.parse(inviteMessage().getBytes(StandardCharsets.UTF_8), sessionManager);

        verify(sessionManager).onCallStart(org.mockito.ArgumentMatchers.argThat(message ->
            message.getMethod() == SipMethodType.INVITE
                && "call-1234@ipcc".equals(message.getCallId())
                && Integer.valueOf(10000).equals(message.getMediaPort())));
    }

    private String inviteMessage() {
        return """
            INVITE sip:1001@211.192.89.43 SIP/2.0\r
            Via: SIP/2.0/UDP 211.192.89.43:5060\r
            From: <sip:07041405642@211.192.89.43>;tag=from-tag\r
            To: <sip:1001@211.192.89.43>;tag=to-tag\r
            Call-ID: call-1234@ipcc\r
            CSeq: 1 INVITE\r
            Content-Type: application/sdp\r
            \r
            v=0\r
            o=- 0 0 IN IP4 211.192.89.43\r
            s=-\r
            c=IN IP4 211.192.89.43\r
            t=0 0\r
            m=audio 10000 RTP/AVP 8\r
            """;
    }

    private String byeMessage() {
        return """
            BYE sip:1001@211.192.89.43 SIP/2.0\r
            From: <sip:07041405642@211.192.89.43>;tag=from-tag\r
            To: <sip:1001@211.192.89.43>;tag=to-tag\r
            Call-ID: call-1234@ipcc\r
            \r
            """;
    }

    private String cancelMessage() {
        return """
            CANCEL sip:1001@211.192.89.43 SIP/2.0\r
            From: <sip:07041405642@211.192.89.43>;tag=from-tag\r
            To: <sip:1001@211.192.89.43>;tag=to-tag\r
            Call-ID: call-1234@ipcc\r
            \r
            """;
    }
}
