package com.oxjohs.sttgw.stt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oxjohs.sttgw.common.SttgwException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SttAdapterFactoryTest {

    @Test
    void 기본_벤더명을_기준으로_기본_어댑터를_반환한다() {
        SttProperties properties = new SttProperties();
        properties.setDefaultVendor("GOOGLE");

        SttAdapterFactory factory = new SttAdapterFactory(
            List.of(new TestAdapter("GOOGLE", true), new TestAdapter("NAVER", true)),
            properties
        );

        SttAdapter adapter = factory.getDefaultAdapter();

        assertThat(adapter.getVendorName()).isEqualTo("GOOGLE");
    }

    @Test
    void 사용가능한_벤더만_목록에_포함한다() {
        SttProperties properties = new SttProperties();
        SttAdapterFactory factory = new SttAdapterFactory(
            List.of(new TestAdapter("GOOGLE", true), new TestAdapter("NAVER", false)),
            properties
        );

        assertThat(factory.getAvailableVendors()).containsExactly("GOOGLE");
    }

    @Test
    void 미지원_또는_비활성_벤더를_요청하면_예외가_발생한다() {
        SttProperties properties = new SttProperties();
        SttAdapterFactory factory = new SttAdapterFactory(
            List.of(new TestAdapter("GOOGLE", true)),
            properties
        );

        assertThatThrownBy(() -> factory.getAdapter("NAVER"))
            .isInstanceOf(SttgwException.class)
            .hasMessageContaining("지원하지 않거나 사용 불가능한 STT 벤더");
    }

    private static class TestAdapter implements SttAdapter {

        private final String vendorName;
        private final boolean available;

        private TestAdapter(String vendorName, boolean available) {
            this.vendorName = vendorName;
            this.available = available;
        }

        @Override
        public SttResult recognize(Path audioFilePath, String languageCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SttStreamingSession startStreaming(String languageCode, SttResultCallback callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getVendorName() {
            return vendorName;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }
    }
}
