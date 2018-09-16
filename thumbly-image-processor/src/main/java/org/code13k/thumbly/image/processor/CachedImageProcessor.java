package org.code13k.thumbly.image.processor;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.image.info.CachedImageInfo;
import org.code13k.thumbly.image.info.model.ImageInfo;
import org.code13k.thumbly.image.processor.model.Command;
import org.code13k.thumbly.image.processor.model.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CachedImageProcessor {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(CachedImageProcessor.class);

    // Data
    private Operator mOperator = null;
    private Map<String, List<Consumer<String>>> mRequestMap = null;

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final CachedImageProcessor INSTANCE = new CachedImageProcessor();
    }

    public static CachedImageProcessor getInstance() {
        return CachedImageProcessor.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private CachedImageProcessor() {
        mLogger.trace("CachedImageProcessor");
    }

    /**
     * Initialize
     */
    synchronized public void init(String cacheDirectory, long maxSizeOfCacheDirectory, int operatorCount) {
        if (mOperator == null) {
            mOperator = new Operator(operatorCount);
            mRequestMap = new HashMap<>();
            FileStore.getInstance().init(cacheDirectory);
            StoreManager.getInstance().init(maxSizeOfCacheDirectory);
        } else {
            mLogger.debug("Duplicated initializing");
        }
    }

    /**
     * Size of cache directory
     */
    public long sizeOfCacheDirectory() {
        String cacheDirectory = FileStore.getInstance().getCacheDirectory();
        long sizeOfCacheDirectory = Util.sizeOfCacheDirectory(cacheDirectory);
        return sizeOfCacheDirectory;
    }

    /**
     * Process
     */
    public void process(String originFilePath, Command command, Consumer<String> consumer) {
        CachedImageInfo.getInstance().get(originFilePath, new Consumer<ImageInfo>() {
            @Override
            public void accept(ImageInfo originImageImageInfo) {
                // Calculate output size
                Size outputSize = calculateOutputSize(originImageImageInfo, command);
                if (outputSize == null) {
                    mLogger.error("Failed to process (outputSize is null)");
                    consumer.accept(null);
                    return;
                }

                // Process
                Command newCommand = new Command();
                newCommand.fromCommand(command);
                newCommand.setSize(outputSize);
                mLogger.trace("command = " + command);
                mLogger.trace("newCommand = " + newCommand);
                processInternal(originFilePath, newCommand, consumer);
            }
        });
    }

    /**
     * Process (Internal)
     */
    private void processInternal(String originFilePath, Command command, Consumer<String> consumer) {
        final String key = generateKey(originFilePath, command);
        String thumbFilePath = FileStore.getInstance().getCacheFile(key);
        mLogger.trace("key = " + key);

        /**
         * Cache HIT!!
         */
        if (StringUtils.isEmpty(thumbFilePath) == false) {
            mLogger.debug("Cache HIT!!");
            consumer.accept(thumbFilePath);
            return;
        }

        /**
         * Cache Not HIT!!
         */
        // Check if duplicate request is already running
        synchronized (mRequestMap) {
            List<Consumer<String>> consumerList;
            if (mRequestMap.containsKey(key)) {
                consumerList = mRequestMap.get(key);
                consumerList.add(consumer);
                return;
            } else {
                consumerList = new ArrayList<>();
                consumerList.add(consumer);
                mRequestMap.put(key, consumerList);
            }
        }

        // Operate
        mOperator.operate(originFilePath, command, new Consumer<String>() {
            @Override
            public void accept(String tempThumbFilePath) {
                mLogger.debug("key = " + key);
                mLogger.debug("tempThumbFilePath = " + tempThumbFilePath);
                String cachedThumbFilePath = FileStore.getInstance().moveToCacheFile(tempThumbFilePath, key);

                // End
                List<Consumer<String>> consumerList = null;
                synchronized (mRequestMap) {
                    if (mRequestMap.containsKey(key)) {
                        consumerList = new ArrayList<>(mRequestMap.get(key));
                        mRequestMap.remove(key);
                    } else {
                        mLogger.error("Your algorithm is wrong. It's very critical.");
                    }
                }
                if (consumerList != null) {
                    mLogger.debug("Duplicate request : " + consumerList.size());
                    consumerList.forEach(consumerItem -> {
                        consumerItem.accept(cachedThumbFilePath);
                    });
                }
            }
        });
    }

    /**
     * Get cached file
     */
    public String getCachedFile(String originFilePath, Command command) {
        // Get cached image info
        ImageInfo cachedImageInfo = CachedImageInfo.getInstance().getCached(originFilePath);
        if (cachedImageInfo != null) {
            // Calculate output size
            Size outputSize = calculateOutputSize(cachedImageInfo, command);
            if (outputSize != null) {
                // Process
                Command newCommand = new Command();
                newCommand.fromCommand(command);
                newCommand.setSize(outputSize);
                // Get thumbnail
                final String key = generateKey(originFilePath, newCommand);
                String thumbFilePath = FileStore.getInstance().getCacheFile(key);
                // End
                if (StringUtils.isEmpty(thumbFilePath) == false) {
                    return thumbFilePath;
                }
            }
        }
        return null;
    }

    /**
     * Operation count in queue
     */
    public int operationCountInQueue() {
        return mOperator.count();
    }

    /**
     * Generate key
     */
    private String generateKey(String filePath, Command command) {
        String name = DigestUtils.md5Hex(Util.MD5FromFile(filePath) + command.toString());
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        String extension = "";
        if (command.getFormat() == Command.Format.ORIGIN) {
            extension = FilenameUtils.getExtension(filePath);
        } else {
            extension = command.getFormat().name().toLowerCase();
        }
        if (StringUtils.isEmpty(extension) == false) {
            sb.append(".");
            sb.append(extension);
        }
        return sb.toString();
    }

    /**
     * Calculate output size
     */
    private static Size calculateOutputSize(ImageInfo imageInfo, Command command) {
        // Exception
        if (command == null) {
            return null;
        }
        if (command.getType() == Command.Type.ORIGIN) {
            return null;
        }

        // Init
        int originOrientation = imageInfo.getOrientation();
        Size originSize = new Size(imageInfo.getSizeWidth(), imageInfo.getSizeHeight());
        Size requestSize = command.getSize();

        // Result
        Size resultSize = new Size();

        // 회전
        if (originOrientation == 5 || originOrientation == 6 || originOrientation == 7 || originOrientation == 8) {
            int temp = requestSize.width;
            requestSize.width = requestSize.height;
            requestSize.height = temp;
        }

        // 가로크기 세로크기 모두 없을 경우 (가로, 세로크기가 0인 경우), 원본 크기를 사용한다.
        if (requestSize.width == 0 && requestSize.height == 0) {
            resultSize.width = originSize.width;
            resultSize.height = originSize.height;
        }

        // 가로크기만 있을 경우 (세로 크기가 0인 경우), 원본보다 크면 원본 크기를 사용한다.
        else if (requestSize.height == 0) {
            if (requestSize.width >= originSize.width) {
                resultSize.width = originSize.width;
                resultSize.height = originSize.height;
            } else {
                float ratio = (float) requestSize.width / (float) originSize.width;
                resultSize.height = (int) (originSize.height * ratio);
                resultSize.width = requestSize.width;
            }
        }

        // 세로크기만 있을 경우 (가로 크기가 0인 경우, 원본보다 크면 원본 크기를 사용한다.
        else if (requestSize.width == 0) {
            if (requestSize.height >= originSize.height) {
                resultSize.width = originSize.width;
                resultSize.height = originSize.height;
            } else {
                float ratio = (float) requestSize.height / (float) originSize.height;
                resultSize.height = requestSize.height;
                resultSize.width = (int) (originSize.width * ratio);
            }
        }

        // 가로와 세로크기를 같이 줬을 경우, 원본보다 가로와 세로 모두 크면 원본 크기를 사용한다.
        else if (requestSize.width >= originSize.width && requestSize.height >= originSize.height) {
            resultSize.width = originSize.width;
            resultSize.height = originSize.height;
        }

        // 가로와 세로크기를 같이 줬을 경우, 가로크기만 원본보다 클 경우
        else if (requestSize.width > originSize.width && requestSize.height < originSize.height) {
            float ratio = (float) requestSize.height / (float) originSize.height;
            resultSize.width = (int) (originSize.width * ratio);
            resultSize.height = (int) (originSize.height * ratio);
        }

        // 가로와 세로크기를 같이 줬을 경우, 세로크기만 원본보다 클 경우
        else if (requestSize.width < originSize.width && requestSize.height > originSize.height) {
            float ratio = (float) requestSize.width / (float) originSize.width;
            resultSize.width = (int) (originSize.width * ratio);
            resultSize.height = (int) (originSize.height * ratio);
        }

        // 가로와 세로크기를 같이 줬을 경우, 원본보다 가로와 세로 모두 작을경우
        else {
            if (command.getType() == Command.Type.THUMB
                    || command.getType() == Command.Type.THUMB_TOP
                    || command.getType() == Command.Type.THUMB_RIGHT
                    || command.getType() == Command.Type.THUMB_BOTTOM
                    || command.getType() == Command.Type.THUMB_LEFT) {
                resultSize.width = requestSize.width;
                resultSize.height = requestSize.height;
            } else if (command.getType() == Command.Type.RESIZE) {
                float ratio = 1.0f;
                if (originSize.width >= originSize.height) {
                    ratio = (float) requestSize.width / (float) originSize.width;
                } else {
                    ratio = (float) requestSize.height / (float) originSize.height;
                }
                resultSize.width = (int) (originSize.width * ratio);
                resultSize.height = (int) (originSize.height * ratio);
            }
        }

        // END
        return resultSize;
    }

    /**
     * Check if Command object is valid
     */
    public static boolean isValid(Command command) {
        if (command == null) {
            mLogger.error("Parameter command is null");
            return false;
        }
        if (command.getType() == null) {
            mLogger.error("Type is null");
            return false;
        }
        if (command.getSize() == null) {
            mLogger.error("Size is null");
            return false;
        }
        if (command.getFormat() == null) {
            mLogger.error("Format is null");
            return false;
        }
        if (command.getType() != Command.Type.ORIGIN) {
            if (command.getSize().width == 0 && command.getSize().height == 0) {
                return false;
            }
        }
        return true;
    }


}
