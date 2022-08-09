package cn.teleinfo.idpointer.sdk.core.stream.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class StreamUtil {

    /** Read bytes from the given InputStream until an EOF is reached. */
    public static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte buf[] = new byte[4096];
        int r;
        while((r=in.read(buf))>=0) {
            bout.write(buf, 0, r);
        }
        return bout.toByteArray();
    }

    /** Read characters from the given Reader until an EOF is reached. */
    public static String readFully(Reader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int r;
        while((r=in.read(buf))>=0) {
            sb.append(buf, 0, r);
        }
        return sb.toString();
    }

    /** Read bytes from the given File until an EOF is reached. */
    @Deprecated
    public static byte[] readFully(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /** Read bytes from the given File until an EOF is reached. */
    @Deprecated
    public static String readFullyAsString(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Read bytes from the given File until an EOF is reached. */
    @Deprecated
    public static byte[] readFully(String file) throws IOException {
        return Files.readAllBytes(Paths.get(file));
    }

    /** Read bytes from the given File until an EOF is reached. */
    @Deprecated
    public static String readFullyAsString(String file) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(file));
        return new String(bytes, StandardCharsets.UTF_8);
    }

}