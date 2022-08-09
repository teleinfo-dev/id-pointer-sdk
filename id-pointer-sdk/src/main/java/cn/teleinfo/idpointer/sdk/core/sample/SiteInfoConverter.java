/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.sample;

import cn.teleinfo.idpointer.sdk.core.GsonUtility;
import cn.teleinfo.idpointer.sdk.core.SiteInfo;
import cn.teleinfo.idpointer.sdk.core.*;

import java.io.*;

public class SiteInfoConverter {

    public static String convertToJson(SiteInfo site) {
        return GsonUtility.getNewGsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(site);
    }

    public static SiteInfo convertToSiteInfo(String input) {
        return GsonUtility.getGson().fromJson(input, SiteInfo.class);
    }

    public static void convertToJson(byte[] input, OutputStream out) throws HandleException, IOException {
        SiteInfo site = new SiteInfo();
        Encoder.decodeSiteInfoRecord(input, 0, site);
        String json = convertToJson(site);
        out.write(json.getBytes("UTF-8"));
    }

    public static void convertToBin(String input, OutputStream out) throws IOException {
        SiteInfo site = convertToSiteInfo(input);
        out.write(Encoder.encodeSiteInfoRecord(site));
    }

    public static void main(String[] args) {
        String outputFilename = null;
        String inputFilename = null;
        boolean sawDash = false;
        boolean expectingOutput = false;
        boolean sawEndOfOptions = false;
        for (String arg : args) {
            if (!sawEndOfOptions && arg.length() >= 2 && arg.startsWith("-")) {
                if (arg.equals("--")) sawEndOfOptions = true;
                else if (arg.equals("-o") || arg.equals("-output") || arg.equals("--output")) {
                    if (outputFilename != null) {
                        System.err.println("Too many output files specified");
                        System.exit(1);
                        return;
                    }
                    expectingOutput = true;
                } else if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
                    System.err.println("arguments: [input-filename] [-o output-filename]");
                    System.err.println("siteinfo.bin input will be converted to json");
                    System.err.println("json input will be converted to siteinfo.bin");
                    System.exit(0);
                    return;
                } else {
                    System.err.println("Unknown option " + arg);
                    System.exit(1);
                    return;
                }
            } else {
                if (expectingOutput) {
                    if (!arg.equals("-")) outputFilename = arg;
                } else {
                    if (sawDash || inputFilename != null) {
                        System.err.println("Too many input files specified");
                        System.exit(1);
                        return;
                    } else {
                        if (arg.equals("-")) sawDash = true;
                        else inputFilename = arg;
                    }
                }
            }
        }

        InputStream in = System.in;
        try {
            if (inputFilename != null) in = new FileInputStream(new File(inputFilename));
        } catch (FileNotFoundException e) {
            System.err.println("File " + inputFilename + " not found");
            System.exit(1);
            return;
        }
        OutputStream out;
        try {
            if (outputFilename != null) out = new FileOutputStream(new File(outputFilename));
            else out = System.out;
        } catch (FileNotFoundException e) {
            System.err.println("File " + outputFilename + " not writeable");
            System.exit(1);
            if (in != null) try { in.close(); } catch (Exception ignored) { }
            return;
        }

        byte[] buf = new byte[4096];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int r;
        try {
            while ((r = in.read(buf)) > 0) {
                bout.write(buf, 0, r);
            }
        } catch (IOException e) {
            System.err.println("IOException reading input");
            System.exit(1);
            return;
        } finally {
            try { in.close(); } catch (IOException e) { }
        }
        byte[] inputBytes = bout.toByteArray();

        try {
            if (Util.looksLikeBinary(inputBytes)) {
                convertToJson(inputBytes, out);
            } else {
                convertToBin(new String(inputBytes, "UTF-8"), out);
            }
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            // e.printStackTrace();
            System.exit(1);
            return;
        } finally {
            try { out.close(); } catch (IOException e) { }
        }
    }
}
