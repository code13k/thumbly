package org.code13k.thumbly.image.processor;


import org.apache.commons.io.FilenameUtils;
import org.code13k.thumbly.image.processor.model.Command;
import org.code13k.thumbly.image.processor.model.Operation;
import org.code13k.thumbly.image.processor.model.Size;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
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
    public void operate(String filePath, Command command, Consumer<String> consumer) {
        Operation operation = new Operation();
        operation.setFilePath(filePath);
        operation.setCommand(command);
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
     * Execute operation
     */
    private String executeOperation(Operation operation) {
        try {
            // Init
            IMOperation imOperation = new IMOperation();

            // Origin File
            String originFilePath = operation.getFilePath();
            imOperation.addImage(originFilePath + "[0]");

            // Resize
            Size resize = operation.getCommand().getSize();
            imOperation.resize(resize.width, resize.height, "^");

            // Gravity
            if (operation.getCommand().getType() == Command.Type.THUMB_TOP) {
                imOperation.gravity("North");
            } else if (operation.getCommand().getType() == Command.Type.THUMB_RIGHT) {
                imOperation.gravity("East");
            } else if (operation.getCommand().getType() == Command.Type.THUMB_BOTTOM) {
                imOperation.gravity("South");
            } else if (operation.getCommand().getType() == Command.Type.THUMB_LEFT) {
                imOperation.gravity("West");
            } else {
                imOperation.gravity("Center");
            }

            // Crop
            imOperation.crop(resize.width, resize.height, 0);

            // Orientation
            imOperation.autoOrient();

            // GIF
            imOperation.p_repage();

            // Quality
            if (operation.getCommand().getQuality() < 100) {
                imOperation.quality((double) operation.getCommand().getQuality());
            }

            // Output File Path
            String extension;
            if (operation.getCommand().getFormat() == Command.Format.ORIGIN) {
                extension = FilenameUtils.getExtension(originFilePath);
            } else {
                extension = operation.getCommand().getFormat().name().toLowerCase();
            }
            String outputFilePath = FileStore.getInstance().generateTempFilePath() + "." + extension;
            imOperation.addImage(outputFilePath);

            // Run
            ConvertCmd convert = new ConvertCmd();
            convert.run(imOperation);
            mLogger.debug("operate() operation # " + operation.toString());

            // Finished
            boolean isFileExists = Files.exists(Paths.get(outputFilePath));
            if (isFileExists == true) {
                return outputFilePath;
            } else {
                mLogger.error("Failed to operate # " + operation);
                return null;
            }
        } catch (Exception e) {
            mLogger.error("Error occurred", e);
        }
        return null;
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
                                String outputFilePath = executeOperation(operation);
                                operation.getConsumer().accept(outputFilePath);
                            }
                        } catch (Exception e) {
                            mLogger.error("Error occurred ", e);
                        }
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("thumbly-image-processor-operator-" + index);
            thread.start();
        }
    }
}
