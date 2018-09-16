package org.code13k.thumbly.app;


import org.apache.commons.io.FileUtils;
import org.code13k.thumbly.image.info.CachedImageInfo;
import org.code13k.thumbly.image.processor.CachedImageProcessor;
import org.code13k.thumbly.web.client.CachedWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Status {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(Status.class);

    // Const
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"; // RFC3339

    // Data
    private final Date mAppStartedDate = new Date();

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final Status INSTANCE = new Status();
    }

    public static Status getInstance() {
        return Status.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private Status() {
        mLogger.trace("Status()");
    }

    /**
     * Initialize
     */
    public void init() {
        // Timer
        Timer timer = new Timer("thumbly-status-logging");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    logging();
                } catch (Exception e) {
                    // Nothing
                }
            }
        }, 5000, 1000);
    }

    /**
     * Logging
     */
    public void logging() {
        StringBuffer sb = new StringBuffer();

        // Running time (hour)
        sb.append("RunningTime = " + getAppRunningTimeHour() + "h");

        // Active Thread
        sb.append(", Thread = " + Thread.activeCount());

        sb.append(", ProcessingCount = " + CachedImageProcessor.getInstance().operationCountInQueue());

        // Image ImageInfo Count
        sb.append(", Cached(ImageInfo) = " + CachedImageInfo.getInstance().size());

        // Cached (Origin)
        long sizeOfCacheDirectory = CachedWebClient.getInstance().sizeOfCacheDirectory();
        String displaySize = FileUtils.byteCountToDisplaySize(sizeOfCacheDirectory);
        sb.append(", Cached(Origin) = " + displaySize);

        // Cached (Thumbnail)
        sizeOfCacheDirectory = CachedImageProcessor.getInstance().sizeOfCacheDirectory();
        displaySize = FileUtils.byteCountToDisplaySize(sizeOfCacheDirectory);

        sb.append(", Cached(Thumbnail) = " + displaySize);

        // End
        mLogger.info(sb.toString());
    }

    /**
     * Get application started time
     */
    public Date getAppStartedDate() {
        return mAppStartedDate;
    }

    /**
     * Get application started time string
     */
    public String getAppStartedDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String formattedDate = sdf.format(Status.getInstance().getAppStartedDate());
        return formattedDate;
    }

    /**
     * Get current time string
     */
    public String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String formattedDate = sdf.format(new Date());
        return formattedDate;
    }

    /**
     * Get application running time (hour)
     */
    public int getAppRunningTimeHour() {
        long createdTimestamp = Status.getInstance().getAppStartedDate().getTime();
        long runningTimestamp = System.currentTimeMillis() - createdTimestamp;
        int runningTimeSec = (int) (runningTimestamp / 1000);
        int runningTimeMin = runningTimeSec / 60;
        int runningTimeHour = runningTimeMin / 60;
        return runningTimeHour;
    }
}
