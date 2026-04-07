package com.oxjohs.sttgw.repository;

import com.oxjohs.sttgw.domain.SttTranscript;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SttTranscriptRepository extends JpaRepository<SttTranscript, Long> {

    List<SttTranscript> findByCallIdOrderByTimestampAsc(String callId);

    List<SttTranscript> findByCallIdAndChannel(String callId, String channel);

    List<SttTranscript> findByCallIdAndIsFinalTrue(String callId);
}
