package cn.teleinfo.idpointer.sdk.security.gm.jni;

import org.gmssl.GmSSLJNI;

import java.io.File;
import java.io.IOException;

public class TempFileManager {

    public static TempFileManager instance;

    public static final String NATIVE_FOLDER_PATH_PREFIX = "keys";

    private File tempFileDir;

    private TempFileManager(String folderPathPrefix) throws IOException {
        this.tempFileDir = createTempDirectory(folderPathPrefix);
    }

    public static TempFileManager getInstance() {
        if (instance == null) {
            synchronized (TempFileManager.class) {
                if (instance == null) {
                    try {
                        instance = new TempFileManager(NATIVE_FOLDER_PATH_PREFIX);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return instance;
    }

    public File getTempFileDir() {
        return tempFileDir;
    }

    private File createTempDirectory(String prefix) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File generatedDir = new File(tempDir, prefix + System.nanoTime());

        if (!generatedDir.mkdir()) {
            throw new IOException("Failed to create temp directory " + generatedDir.getName());
        }

        generatedDir.deleteOnExit();

        return generatedDir;
    }

    public static interface FileHandler {
        void handle(File tempFile);
    }

    public void useTempFile(FileHandler fileHandler) {
        File tempKeyFile = new File(getInstance().getTempFileDir(),"key"+System.nanoTime()+".pem");
        try{
            fileHandler.handle(tempKeyFile);
        }finally {
            tempKeyFile.delete();
        }
    }


}
