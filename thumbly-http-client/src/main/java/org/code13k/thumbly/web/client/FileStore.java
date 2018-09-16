package org.code13k.thumbly.web.client;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

public class FileStore {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(FileStore.class);

    // Const
    private static final String DEFAULT_TEMP_DIRECTORY = ".temp";

    // Data
    private String mTempDirectory;
    private String mCacheDirectory;

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final FileStore INSTANCE = new FileStore();
    }

    public static FileStore getInstance() {
        return FileStore.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private FileStore() {
        mLogger.trace("FileStore()");
    }

    /**
     * Initialize
     */
    synchronized public void init(String cacheDirectory) {
        if (mCacheDirectory == null) {
            try {
                mTempDirectory = DEFAULT_TEMP_DIRECTORY;
                if (Files.exists(Paths.get(mTempDirectory)) == false) {
                    Files.createDirectories(Paths.get(mTempDirectory));
                }
                mCacheDirectory = cacheDirectory;
                if (Files.exists(Paths.get(mCacheDirectory)) == false) {
                    Files.createDirectories(Paths.get(mCacheDirectory));
                }
            } catch (Exception e) {
                mLogger.error("FileStore() error occurred", e);
            }
        } else {
            mLogger.debug("Duplicated initializing");
        }
    }

    /**
     * Get temp directory
     */
    public String getTempDirectory(){
        return mTempDirectory;
    }

    /**
     * Get cache directory
     */
    public String getCacheDirectory(){
        return mCacheDirectory;
    }

    /**
     * Save content to temp file
     */
    public String saveToTempFile(byte[] bytes) {
        try {
            // Check directory
            if (Files.exists(Paths.get(mTempDirectory)) == false) {
                Files.createDirectories(Paths.get(mTempDirectory));
            }

            // Generate file name
            UUID uuid = UUID.randomUUID();
            String fileName = DigestUtils.md5Hex(uuid.toString());
            String filePath = mTempDirectory + "/" + fileName;

            // Save
            boolean result = save(filePath, bytes);
            if (result == true) {
                return filePath;
            }
            return null;
        } catch (Exception e) {
            mLogger.error("saveToTempFile() error occurred", e);
            return null;
        }
    }

    /**
     * Move (temp) file to cache file
     */
    public String moveToCacheFile(String filePath, String key) {
        try {
            // Check directory
            if (Files.exists(Paths.get(mCacheDirectory)) == false) {
                Files.createDirectories(Paths.get(mCacheDirectory));
            }

            // Generate file path
            String dstFilePath = mCacheDirectory + "/" + key;

            // Save
            boolean result = move(filePath, dstFilePath);
            if (result == true) {
                return dstFilePath;
            }
            return null;
        } catch (Exception e) {
            mLogger.error("moveToCacheFile() error occurred", e);
            return null;
        }
    }

    /**
     * Delete cache file
     */
    public void deleteCacheFile(String key) {
        try {
            // Generate file path
            String filePath = mCacheDirectory + "/" + key;

            // Delete
            Files.deleteIfExists(Paths.get(filePath));
        } catch (Exception e) {
            mLogger.error("deleteCacheFile() error occurred", e);
        }
    }

    /**
     * Get cache file if exists
     */
    public String getCacheFile(String key) {
        try {
            // Generate file path
            String filePath = mCacheDirectory + "/" + key;

            // Result
            boolean result = Files.exists(Paths.get(filePath));
            if (result == true) {
                return filePath;
            }
            return null;
        } catch (Exception e) {
            mLogger.error("deleteCacheFile() error occurred", e);
            return null;
        }
    }


    /**
     * saveFile()
     */
    private boolean save(String filePath, byte[] bytes) {
        try {
            OpenOption[] options = new OpenOption[]{WRITE, CREATE_NEW};
            Path resultPath = Files.write(Paths.get(filePath), bytes, options);
            return resultPath != null;
        } catch (Exception e) {
            String errorMessage = "save() error occurred # " + filePath + " : " + e;
            mLogger.error(errorMessage);
        }
        return false;
    }

    /**
     * move()
     */
    private boolean move(String srcFilePath, String dstFilePath) {
        try {
            Path srcPath = Paths.get(srcFilePath);
            Path dstPath = Paths.get(dstFilePath);
            Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (Exception e) {
            String errorMessage = "move() error occurred # " + srcFilePath + ", " + dstFilePath + ", " + e;
            mLogger.error(errorMessage);
        }
        return false;
    }
}
