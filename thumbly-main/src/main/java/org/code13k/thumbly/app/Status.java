package org.code13k.thumbly.app;


import org.apache.commons.io.FileUtils;
import org.code13k.thumbly.business.ClusteredSecretUrl;
import org.code13k.thumbly.image.info.CachedImageInfo;
import org.code13k.thumbly.image.processor.CachedImageProcessor;
import org.code13k.thumbly.web.client.CachedWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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
        Timer timer = new Timer("thumbly-status");
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

        // SecretUrl
        sb.append(", SecretUrls=" + ClusteredSecretUrl.getInstance().size());

        // End
        mLogger.info(sb.toString());
    }

    /**
     * Get all values
     */
    public Map<String, Object> values() {
        HashMap<String, Object> result = new HashMap<>();

        // Common
        HashMap<Long, String> threadInfo = getThreadInfo();
        result.put("threadInfo", threadInfo);
        result.put("threadCount", threadInfo.size());
        result.put("startedDate", getAppStartedDateString());
        result.put("currentDate", getCurrentDateString());
        result.put("runningTimeHour", getAppRunningTimeHour());
        result.put("cpuUsage", getCpuUsage());
        result.put("vmMemoryUsage", getVmMemoryUsage());

        return result;
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

    /**
     * Get CPU usage
     */
    public double getCpuUsage() {
        // CPU Usage
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuUsage = operatingSystemMXBean.getSystemLoadAverage();
        return new Double(new DecimalFormat("#.##").format(cpuUsage));
    }

    /**
     * Get memory usage of VM
     */
    public HashMap<String, String> getVmMemoryUsage() {
        // VM Memory Usage
        Runtime runtime = Runtime.getRuntime();
        NumberFormat format = NumberFormat.getInstance();
        long vmMemoryMax = runtime.maxMemory();
        long vmMemoryAllocated = runtime.totalMemory();
        long vmMemoryFree = runtime.freeMemory();
        String vmMemoryMaxString = format.format(vmMemoryMax / 1024 / 1024) + "M";
        String vmMemoryAllocatedString = format.format(vmMemoryAllocated / 1024 / 1024) + "M";
        String vmMemoryFreeString = format.format(vmMemoryFree / 1024 / 1024) + "M";
        String vmMemoryTotalFreeString = format.format((vmMemoryFree + (vmMemoryMax - vmMemoryAllocated)) / 1024 / 1024) + "M";

        // Result
        HashMap<String, String> result = new HashMap<>();
        result.put("max", vmMemoryMaxString);
        result.put("allocated", vmMemoryAllocatedString);
        result.put("free", vmMemoryFreeString);
        result.put("totalFree", vmMemoryTotalFreeString);
        return result;
    }

    /**
     * Get thread info
     */
    public HashMap<Long, String> getThreadInfo() {
        NumberFormat format = NumberFormat.getInstance();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadMXBean.getAllThreadIds();
        HashMap<Long, String> threadResult = new HashMap<>();
        for (long threadId : threadIds) {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId);
            String threadInfoValue = threadInfo.getThreadName() + ", " + format.format(threadMXBean.getThreadCpuTime(threadId) / 1000000000) + "sec";
            threadResult.put(threadId, threadInfoValue);
        }
        return threadResult;
    }
}
