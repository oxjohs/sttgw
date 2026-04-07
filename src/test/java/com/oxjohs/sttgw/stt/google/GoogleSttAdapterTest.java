package com.oxjohs.sttgw.stt.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oxjohs.sttgw.common.SttgwException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GoogleSttAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void 자격증명_파일이_존재하면_사용가능하다() throws Exception {
        Path credentialFile = tempDir.resolve("google-key.json");
        Files.writeString(credentialFile, "{\"type\":\"service_account\"}");

        GoogleSttProperties properties = new GoogleSttProperties();
        properties.setCredentialPath(credentialFile.toString());

        GoogleSttAdapter adapter = new GoogleSttAdapter(properties);

        assertThat(adapter.isAvailable()).isTrue();
    }

    @Test
    void 자격증명_파일이_없으면_사용불가능하다() {
        GoogleSttProperties properties = new GoogleSttProperties();
        properties.setCredentialPath(tempDir.resolve("missing-key.json").toString());

        GoogleSttAdapter adapter = new GoogleSttAdapter(properties);

        assertThat(adapter.isAvailable()).isFalse();
    }

    @Test
    void 오디오_파일이_없으면_배치인식에서_예외가_발생한다() {
        GoogleSttProperties properties = new GoogleSttProperties();
        GoogleSttAdapter adapter = new GoogleSttAdapter(properties);

        assertThatThrownBy(() -> adapter.recognize(tempDir.resolve("missing.wav"), "ko-KR"))
            .isInstanceOf(SttgwException.class)
            .hasMessageContaining("오디오 파일을 찾을 수 없습니다");
    }

    @Test
    void 벤더명은_GOOGLE_이다() {
        GoogleSttProperties properties = new GoogleSttProperties();
        GoogleSttAdapter adapter = new GoogleSttAdapter(properties);

        assertThat(adapter.getVendorName()).isEqualTo("GOOGLE");
    }
}
