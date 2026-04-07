package com.oxjohs.sttgw.api;

import com.oxjohs.sttgw.api.dto.ApiResponse;
import com.oxjohs.sttgw.api.dto.CallInfoResponse;
import com.oxjohs.sttgw.api.dto.CallRecordResponse;
import com.oxjohs.sttgw.api.dto.SttTranscriptResponse;
import com.oxjohs.sttgw.repository.CallRecordRepository;
import com.oxjohs.sttgw.repository.SttTranscriptRepository;
import com.oxjohs.sttgw.session.CallSessionManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallSessionManager callSessionManager;
    private final CallRecordRepository callRecordRepository;
    private final SttTranscriptRepository sttTranscriptRepository;

    @GetMapping("/active")
    public ApiResponse<List<CallInfoResponse>> getActiveCalls() {
        List<CallInfoResponse> responses = callSessionManager.getActiveSessions().stream()
            .map(CallInfoResponse::from)
            .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/history")
    public ApiResponse<Page<CallRecordResponse>> getCallHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<CallRecordResponse> responses = callRecordRepository
            .findAllByOrderByStartTimeDesc(PageRequest.of(page, size))
            .map(CallRecordResponse::from);
        return ApiResponse.success(responses);
    }

    @GetMapping("/{callId}/transcript")
    public ApiResponse<List<SttTranscriptResponse>> getTranscript(@PathVariable String callId) {
        List<SttTranscriptResponse> responses = sttTranscriptRepository.findByCallIdOrderByTimestampAsc(callId)
            .stream()
            .map(SttTranscriptResponse::from)
            .toList();
        return ApiResponse.success(responses);
    }
}
