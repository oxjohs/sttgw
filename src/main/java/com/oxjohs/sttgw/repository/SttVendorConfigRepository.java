package com.oxjohs.sttgw.repository;

import com.oxjohs.sttgw.domain.SttVendorConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SttVendorConfigRepository extends JpaRepository<SttVendorConfig, Long> {

    Optional<SttVendorConfig> findFirstByOrderByIdAsc();

    Optional<SttVendorConfig> findByVendorNameAndActiveTrue(String vendorName);
}
