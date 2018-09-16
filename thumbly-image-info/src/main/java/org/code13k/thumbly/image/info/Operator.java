package org.code13k.thumbly.image.info;

import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.image.info.model.ImageInfo;
import org.code13k.thumbly.image.info.model.Operation;
import org.im4java.core.IMOperation;
import org.im4java.core.IdentifyCmd;
import org.im4java.core.Info;
import org.im4java.process.ArrayListOutputConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Operator {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(Operator.class);

    // Data
    private ConcurrentLinkedQueue<Operation> mQueue = new ConcurrentLinkedQueue<>();

    /**
     * Constructor
     */
    public Operator(int executorCount) {
        run(executorCount);
    }

    /**
     * Operate
     */
    public void operate(String filePath, Consumer<ImageInfo> consumer) {
        Operation operation = new Operation();
        operation.setFilePath(filePath);
        operation.setConsumer(consumer);
        mQueue.offer(operation);
    }

    /**
     * Count
     */
    public int count() {
        return mQueue.size();
    }

    /**
     * Get image info
     */
    private ImageInfo getImageInfo(String filePath) {
        long startTime = System.currentTimeMillis();
        ImageInfo result = new ImageInfo();

        // Log
        mLogger.trace("filePath = " + filePath);

        // Exception
        if (StringUtils.isEmpty(filePath) == true) {
            return null;
        }

        // Width & Height
        try {
            Info info = new Info(filePath, true);
            if (info == null) {
                return null;
            }
            if (info.getImageWidth() == 0 || info.getImageHeight() == 0) {
                return null;
            }
            result.setSize(info.getImageWidth(), info.getImageHeight());
        } catch (Exception e) {
            mLogger.error("Failed to get image size : " + filePath + ", " + result, e);
            return null;
        }

        // Orientation
        result.setOrientation(1);
        try {
            IMOperation operation = new IMOperation();
            operation.ping().format("%[METADATA:Orientation]\n");
            operation.addImage(filePath);
            IdentifyCmd identifyCmd = new IdentifyCmd();
            ArrayListOutputConsumer output = new ArrayListOutputConsumer();
            identifyCmd.setOutputConsumer(output);
            identifyCmd.run(operation);
            if (output.getOutput() != null && StringUtils.isEmpty(output.getOutput().get(0)) == false) {
                result.setOrientation(Integer.valueOf(output.getOutput().get(0)));
            }
        } catch (Exception e) {
            mLogger.error("Failed to get meta data : " + filePath, e);
        }

        // End
        mLogger.debug("processing time #1 : " + Util.processingTime(startTime) + "ms");
        return result;
    }

    /**
     * Run
     */
    private void run(int executorCount) {
        for (int index = 0; index < executorCount; index++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Operation operation = mQueue.poll();
                            if (operation == null) {
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                    // Nothing
                                }
                            } else {
                                ImageInfo imageInfo = getImageInfo(operation.getFilePath());
                                operation.getConsumer().accept(imageInfo);
                            }
                        } catch (Exception e) {
                            mLogger.error("Error occurred ", e);
                        }
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("thumbly-image-info-operator-" + index);
            thread.start();
        }
    }
}
