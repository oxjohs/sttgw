package com.oxjohs.sttgw.dashboard;

import com.oxjohs.sttgw.api.dto.CallInfoResponse;
import com.oxjohs.sttgw.domain.SttVendorConfig;
import com.oxjohs.sttgw.repository.SttVendorConfigRepository;
import com.oxjohs.sttgw.session.CallSessionManager;
import com.oxjohs.sttgw.stt.SttAdapterFactory;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CallSessionManager callSessionManager;
    private final SttAdapterFactory sttAdapterFactory;
    private final SttVendorConfigRepository sttVendorConfigRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        List<CallInfoResponse> activeCalls = callSessionManager.getActiveSessions().stream()
            .map(CallInfoResponse::from)
            .toList();

        model.addAttribute("activeCalls", activeCalls);
        model.addAttribute("activeCount", callSessionManager.getActiveSessionCount());
        return "dashboard";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        SttVendorConfig currentConfig = sttVendorConfigRepository.findFirstByOrderByIdAsc()
            .orElse(SttVendorConfig.defaultConfig());

        LinkedHashSet<String> vendorSet = new LinkedHashSet<>(sttAdapterFactory.getAvailableVendors());
        if (currentConfig.getVendorName() != null && !currentConfig.getVendorName().isBlank()) {
            vendorSet.add(currentConfig.getVendorName());
        }

        model.addAttribute("currentConfig", currentConfig);
        model.addAttribute("vendors", new ArrayList<>(vendorSet));
        return "settings";
    }
}
