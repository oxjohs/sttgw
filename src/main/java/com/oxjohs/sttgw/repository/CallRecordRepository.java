package com.oxjohs.sttgw.repository;

import com.oxjohs.sttgw.domain.CallRecord;
import com.oxjohs.sttgw.session.CallState;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {

    Optional<CallRecord> findByCallId(String callId);

    Page<CallRecord> findAllByOrderByStartTimeDesc(Pageable pageable);

    List<CallRecord> findByState(CallState state);

    long countByStartTimeBetween(LocalDateTime from, LocalDateTime to);
}
