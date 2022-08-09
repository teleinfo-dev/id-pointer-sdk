/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.sample;


import cn.teleinfo.idpointer.sdk.core.*;

import java.io.*;
import java.util.*;

public class LocalInfoConverter {

    public static Map<String, SiteInfo[]> convertFromJson(String json) {
        List<LocalInfoEntry> list = GsonUtility.getGson().fromJson(json, new com.google.gson.reflect.TypeToken<List<LocalInfoEntry>>() {
        }.getType());
        Map<String, SiteInfo[]> res = new HashMap<>();
        for (LocalInfoEntry entry : list) {
            for (String na : entry.getNas()) {
                SiteInfo[] sites = res.get(na);
                res.put(na, extendSites(sites, entry.getSite()));
            }
        }
        return res;
    }

    private static SiteInfo[] extendSites(SiteInfo[] sites, SiteInfo site) {
        if (sites == null || sites.length == 0) return new SiteInfo[] { site };
        SiteInfo[] newsites = new SiteInfo[sites.length + 1];
        System.arraycopy(sites, 0, newsites, 0, sites.length);
        newsites[sites.length] = site;
        return newsites;
    }

    public static String convertToJson(List<LocalInfoEntry> localInfo) {
        return GsonUtility.getNewGsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(localInfo);
    }

    public static String convertToJson(Map<String, SiteInfo[]> localInfo) {
        Map<SiteInfo, List<String>> map = new LinkedHashMap<>();
        for (Map.Entry<String, SiteInfo[]> entry : localInfo.entrySet()) {
            for (SiteInfo site : entry.getValue()) {
                List<String> nas = map.get(site);
                if (nas == null) nas = new ArrayList<>();
                nas.add(entry.getKey());
                map.put(site, nas);
            }
        }
        List<LocalInfoEntry> list = new ArrayList<>();
        for (Map.Entry<SiteInfo, List<String>> entry : map.entrySet()) {
            list.add(new LocalInfoEntry(entry.getValue(), entry.getKey()));
        }
        return convertToJson(list);
    }

    public static void convertToJson(byte[] bytes, OutputStream out) throws HandleException, IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        try {
            Map<String, SiteInfo[]> map = Encoder.decodeLocalSites(input);
            String json = convertToJson(map);
            out.write(json.getBytes("UTF-8"));
        } finally {
            input.close();
        }
    }

    public static void convertToBin(String json, OutputStream out) throws HandleException, IOException, HandleException {
        List<LocalInfoEntry> list = GsonUtility.getGson().fromJson(json, new com.google.gson.reflect.TypeToken<List<LocalInfoEntry>>() {
        }.getType());
        SiteInfo[] sites = new SiteInfo[list.size()];
        String[][] nas = new String[list.size()][];
        int i = 0;
        for (LocalInfoEntry entry : list) {
            sites[i] = entry.getSite();
            nas[i] = entry.getNas().toArray(new String[0]);
            i++;
        }
        out.write(Encoder.encodeLocalSites(sites, nas));
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
                    System.err.println("local_info binary input will be converted to json");
                    System.err.println("json input will be converted to local_info binary");
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

    public static class LocalInfoEntry {
        private List<String> nas;
        private SiteInfo site;

        public LocalInfoEntry(List<String> nas, SiteInfo site) {
            this.nas = nas;
            this.site = site;
        }

        public List<String> getNas() {
            return nas;
        }

        public void setNas(List<String> nas) {
            this.nas = nas;
        }

        public SiteInfo getSite() {
            return site;
        }

        public void setSite(SiteInfo site) {
            this.site = site;
        }
    }

}
