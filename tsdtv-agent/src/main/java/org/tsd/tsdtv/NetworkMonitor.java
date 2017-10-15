package org.tsd.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Singleton
public class NetworkMonitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(NetworkMonitor.class);

    private static final int MAX_DURATION_MILLIS
            = new Long(TimeUnit.MINUTES.toMillis(1)).intValue();

    private static final long TEST_PERIOD_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private static final String TARGET_URI = "http://posttestserver.com/post.php";
    private static final int UPLOAD_FILE_SIZE = 1_000_000;

    private boolean shutdown = false;
    private SpeedTestReport report = null;
    private SpeedTestError error = null;

    @Inject
    public NetworkMonitor() {

    }

    @Override
    public void run() {
        while (!shutdown) {
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
                @Override
                public void onCompletion(SpeedTestReport speedTestReport) {
                    publishReport(speedTestReport);
                }

                @Override
                public void onProgress(float v, SpeedTestReport speedTestReport) {
                    log.debug("Progress {}: bitrate = {}/s",
                            speedTestReport.getProgressPercent(),
                            getHumanReadableBytes(speedTestReport.getTransferRateBit()));
                }

                @Override
                public void onError(SpeedTestError speedTestError, String s) {
                    publishError(speedTestError, s);
                }
            });
            speedTestSocket.startFixedUpload(TARGET_URI,
                    UPLOAD_FILE_SIZE,
                    MAX_DURATION_MILLIS);
            try {
                Thread.sleep(TEST_PERIOD_MILLIS);
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
                shutdown();
            }
        }
    }

    public SpeedTestReport getReport() {
        return report;
    }

    public SpeedTestError getError() {
        return error;
    }

    private void publishReport(SpeedTestReport report) {
        log.info("Speed test finished: bitrate = {}/s", getHumanReadableBytes(report.getTransferRateBit()));
        this.report = report;
        this.error = null;
    }

    private void publishError(SpeedTestError error, String message) {
        log.error("Speed test ERROR ({}): {}", error, message);
        this.report = null;
        this.error = error;
    }

    private static String getHumanReadableBytes(BigDecimal bitrate) {
        return FileUtils.byteCountToDisplaySize(bitrate.longValue());
    }

    public void shutdown() {
        this.shutdown = true;
    }

}