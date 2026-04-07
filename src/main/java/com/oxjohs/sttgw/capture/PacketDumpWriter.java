package com.oxjohs.sttgw.capture;

import com.oxjohs.sttgw.common.FileUtils;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.DataLinkType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PacketDumpWriter {

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CaptureProperties captureProperties;
    private final Clock systemClock;

    private final ExecutorService dumpExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "pcap-dump");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, PcapDumper> dumperCache = new ConcurrentHashMap<>();
    private final Map<String, PcapHandle> handleCache = new ConcurrentHashMap<>();

    public void writeAsync(Packet packet) {
        if (packet == null) {
            return;
        }

        try {
            dumpExecutor.submit(() -> dump(packet));
        } catch (RuntimeException exception) {
            log.warn("패킷 덤프 작업 제출 실패: message={}", exception.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        dumpExecutor.shutdown();
        try {
            if (!dumpExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                dumpExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            dumpExecutor.shutdownNow();
        }

        dumperCache.values().forEach(PcapDumper::close);
        handleCache.values().forEach(PcapHandle::close);
        dumperCache.clear();
        handleCache.clear();
    }

    private void dump(Packet packet) {
        LocalDateTime now = LocalDateTime.now(systemClock);
        try {
            PcapDumper dumper = getOrCreateDumper(now);
            dumper.dump(packet);
        } catch (IOException | PcapNativeException | NotOpenException exception) {
            log.warn("패킷 덤프 쓰기 실패: message={}", exception.getMessage());
        }
    }

    private synchronized PcapDumper getOrCreateDumper(LocalDateTime dateTime)
        throws IOException, PcapNativeException, NotOpenException {
        String dateKey = dateTime.toLocalDate().toString();
        PcapDumper existing = dumperCache.get(dateKey);
        if (existing != null && existing.isOpen()) {
            return existing;
        }

        Path directory = FileUtils.resolveDatedDirectory(captureProperties.getDumpPath(), dateTime);
        Files.createDirectories(directory);

        String fileName = "sttgw-" + dateTime.format(FILE_DATE_FORMATTER) + ".pcap";
        Path dumpFile = directory.resolve(fileName);

        PcapHandle handle = Pcaps.openDead(DataLinkType.EN10MB, captureProperties.getSnapLen());
        PcapDumper dumper = handle.dumpOpen(dumpFile.toString());

        dumperCache.put(dateKey, dumper);
        handleCache.put(dateKey, handle);

        return dumper;
    }
}
