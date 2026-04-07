package com.oxjohs.sttgw.api;

import com.oxjohs.sttgw.api.dto.ApiResponse;
import com.oxjohs.sttgw.api.dto.SttResultResponse;
import com.oxjohs.sttgw.stt.SttAdapter;
import com.oxjohs.sttgw.stt.SttAdapterFactory;
import com.oxjohs.sttgw.stt.SttResult;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/stt")
@RequiredArgsConstructor
public class SttController {

    private final SttAdapterFactory sttAdapterFactory;

    @PostMapping("/recognize")
    public ApiResponse<SttResultResponse> recognize(
        @RequestParam String filePath,
        @RequestParam(defaultValue = "ko-KR") String lang,
        @RequestParam(defaultValue = "GOOGLE") String vendor
    ) {
        SttAdapter adapter = sttAdapterFactory.getAdapter(vendor);
        SttResult result = adapter.recognize(Path.of(filePath), lang);
        return ApiResponse.success(SttResultResponse.from(result));
    }

    @GetMapping("/vendors")
    public ApiResponse<List<String>> getAvailableVendors() {
        return ApiResponse.success(sttAdapterFactory.getAvailableVendors());
    }
}
