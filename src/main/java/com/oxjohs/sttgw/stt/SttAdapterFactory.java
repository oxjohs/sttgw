package com.oxjohs.sttgw.stt;

import com.oxjohs.sttgw.common.SttgwException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SttAdapterFactory {

    private final List<SttAdapter> adapters;
    private final SttProperties sttProperties;

    public SttAdapter getDefaultAdapter() {
        return getAdapter(sttProperties.getDefaultVendor());
    }

    public SttAdapter getAdapter(String vendorName) {
        return adapters.stream()
            .filter(SttAdapter::isAvailable)
            .filter(adapter -> adapter.getVendorName().equalsIgnoreCase(vendorName))
            .findFirst()
            .orElseThrow(() -> new SttgwException("지원하지 않거나 사용 불가능한 STT 벤더입니다: " + vendorName));
    }

    public List<String> getAvailableVendors() {
        return adapters.stream()
            .filter(SttAdapter::isAvailable)
            .map(SttAdapter::getVendorName)
            .sorted(String::compareToIgnoreCase)
            .toList();
    }
}
