/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.stream.StreamTable;
import cn.teleinfo.idpointer.sdk.core.stream.StreamVector;

import java.io.*;
import java.net.InetAddress;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ConfigCommon {
    /******************************************************************************
     *
     * Create and return a StreamTable full of configuration data suitable for
     * writing to the configuration file (HSG.CONFIG_FILE_NAME).
     *
     */
    public static StreamTable configuration(int serverType, boolean disableUDP, int port, boolean logAccesses, InetAddress listenAddr, int threadCount, int httpPort, String logSavingInterval, // "Never", "Monthly", "Weekly", or "Daily"
                                            boolean caseSensitive, int maxAuthTime, int maxSessionTime, int serverId, boolean isPrimary, String defaultServerAdmin, String replicationAdminStr, String replicationAuthStr, String defaultHomedPrefix,
                                            boolean serverAdminFullAccess) {
        InetAddress[] listenAddrArray = new InetAddress[2];
        listenAddrArray[0] = listenAddr;
        return configuration(serverType, disableUDP, port, logAccesses, listenAddrArray, threadCount, httpPort, logSavingInterval, caseSensitive, maxAuthTime, maxSessionTime, serverId, isPrimary, defaultServerAdmin, replicationAdminStr,
            replicationAuthStr, defaultHomedPrefix, false, serverAdminFullAccess);
    }

    public static StreamTable configuration(int serverType, boolean disableUDP, int port, boolean logAccesses, InetAddress[] listenAddr, int threadCount, int httpPort, String logSavingInterval, // "Never", "Monthly", "Weekly", or "Daily"
        boolean caseSensitive, int maxAuthTime, int maxSessionTime, int serverId, boolean isPrimary, String defaultServerAdmin, String replicationAdminStr, String replicationAuthStr, String defaultHomedPrefix, boolean isDualStack,
        boolean serverAdminFullAccess) {
        StreamTable config = new StreamTable(), tcpConfig = new StreamTable(), httpConfig = new StreamTable(), udpConfig = new StreamTable(), svrConfig = new StreamTable(), logSaveConfig = new StreamTable();

        StreamVector sv = new StreamVector();
        StreamVector saVect = new StreamVector();

        config.put(HSG.NO_UDP, (disableUDP ? "yes" : "no"));

        if (!isDualStack) {

            config.put(HSG.INTERFACES, sv);
            sv.addElement("hdl_udp");
            sv.addElement("hdl_tcp");
            sv.addElement("hdl_http");

            String portStr = String.valueOf(port);
            String accessesToBeLogged = ((logAccesses) ? "yes" : "no");
            String hostAddress = Util.rfcIpRepr(listenAddr[0]);

            config.put(HSG.TCP_CONFIG, tcpConfig);
            tcpConfig.put("bind_port", portStr);
            tcpConfig.put("num_threads", String.valueOf(threadCount));
            tcpConfig.put("log_accesses", accessesToBeLogged);
            tcpConfig.put("bind_address", hostAddress);

            config.put(HSG.HTTP_CONFIG, httpConfig);
            httpConfig.put("bind_port", String.valueOf(httpPort));
            httpConfig.put("num_threads", String.valueOf(threadCount));
            httpConfig.put("log_accesses", accessesToBeLogged);
            httpConfig.put("bind_address", hostAddress);

            config.put(HSG.UDP_CONFIG, udpConfig);
            udpConfig.put("bind_port", portStr);
            udpConfig.put("num_threads", String.valueOf(threadCount));
            udpConfig.put("log_accesses", accessesToBeLogged);
            udpConfig.put("bind_address", hostAddress);
        }

        else {

            StreamTable tcp4Config = new StreamTable();
            StreamTable http4Config = new StreamTable();
            StreamTable udp4Config = new StreamTable();

            config.put(HSG.INTERFACES, sv);
            sv.addElement("hdl_udp");
            sv.addElement("hdl_tcp");
            sv.addElement("hdl_http");
            sv.addElement("hdl_udp4");
            sv.addElement("hdl_tcp4");
            sv.addElement("hdl_http4");

            String portStr = String.valueOf(port);
            String accessesToBeLogged = ((logAccesses) ? "yes" : "no");
            String hostAddress = Util.rfcIpRepr(listenAddr[0]);
            String hostAddress4 = Util.rfcIpRepr(listenAddr[1]);

            config.put(HSG.TCP_CONFIG, tcpConfig);
            tcpConfig.put("bind_port", portStr);
            tcpConfig.put("num_threads", String.valueOf(threadCount));
            tcpConfig.put("log_accesses", accessesToBeLogged);
            tcpConfig.put("bind_address", hostAddress);

            config.put(HSG.HTTP_CONFIG, httpConfig);
            httpConfig.put("bind_port", String.valueOf(httpPort));
            httpConfig.put("num_threads", String.valueOf(threadCount));
            httpConfig.put("log_accesses", accessesToBeLogged);
            httpConfig.put("bind_address", hostAddress);

            config.put(HSG.UDP_CONFIG, udpConfig);
            udpConfig.put("bind_port", portStr);
            udpConfig.put("num_threads", String.valueOf(threadCount));
            udpConfig.put("log_accesses", accessesToBeLogged);
            udpConfig.put("bind_address", hostAddress);

            config.put(HSG.TCP4_CONFIG, tcp4Config);
            tcp4Config.put("bind_port", portStr);
            tcp4Config.put("num_threads", String.valueOf(threadCount));
            tcp4Config.put("log_accesses", accessesToBeLogged);
            tcp4Config.put("bind_address", hostAddress4);

            config.put(HSG.HTTP4_CONFIG, http4Config);
            http4Config.put("bind_port", String.valueOf(httpPort));
            http4Config.put("num_threads", String.valueOf(threadCount));
            http4Config.put("log_accesses", accessesToBeLogged);
            http4Config.put("bind_address", hostAddress4);

            config.put(HSG.UDP4_CONFIG, udp4Config);
            udp4Config.put("bind_port", portStr);
            udp4Config.put("num_threads", String.valueOf(threadCount));
            udp4Config.put("log_accesses", accessesToBeLogged);
            udp4Config.put("bind_address", hostAddress4);

        }
        // Log save-off data
        if (!(logSavingInterval.equals(HSG.NEVER))) {
            config.put(HSG.LOG_SAVE_CONFIG, logSaveConfig);
            logSaveConfig.put(HSG.LOG_SAVE_INTERVAL, logSavingInterval);
            logSaveConfig.put(HSG.LOG_SAVE_DIRECTORY, "logs");
        }

        svrConfig.put("this_server_id", String.valueOf(serverId));
        svrConfig.put("server_admins", saVect);

        if (serverType == HSG.SVR_TYPE_SERVER) { // Settings for SVR_TYPE_SERVER only
            config.put(HSG.SERVER_TYPE, "server");
            config.put(HSG.SERVER_CONFIG, svrConfig);

            svrConfig.put("server_admin_full_access", (serverAdminFullAccess ? "yes" : "no"));
            svrConfig.put("case_sensitive", (caseSensitive ? "yes" : "no"));
            svrConfig.put("max_auth_time", String.valueOf(maxAuthTime));
            svrConfig.put("max_session_time", String.valueOf(maxSessionTime));
            if (isPrimary) {
                //           Primary server entry for backup_admins, this is now considered deprecated and so is not included in the default config
                //        StreamVector backupVect = new StreamVector();
                //        svrConfig.put("backup_admins", backupVect);
                //        backupVect.addElement(defaultServerAdmin);

                StreamVector raVect = new StreamVector();
                svrConfig.put("replication_admins", raVect);
                raVect.addElement(replicationAdminStr);
            } else {
                svrConfig.put("replication_authentication", "privatekey:" + replicationAuthStr);
            }

            saVect.addElement(defaultServerAdmin);
        } else { // Settings for SVR_TYPE_CACHE only
            config.put(HSG.SERVER_TYPE, "cache");
            config.put(HSG.CACHE_CONFIG, svrConfig);
            // Leave saVect empty
        }

        if (isPrimary && defaultHomedPrefix != null) {
            StreamVector autoHomedPrefixesVect = new StreamVector();
            autoHomedPrefixesVect.addElement(defaultHomedPrefix);
            svrConfig.put("auto_homed_prefixes", autoHomedPrefixesVect);
        }

        return config;
    }

    /**
     * Create and return a StreamTable full of contact data suitable for
     * writing to a contact-data file (HSG.SITE_CONTACT_DATA_FILE_NAME).
     */
    public static StreamTable contactDataTable(String orgName, String contactName, String contactPhone, String contactEmail) {
        StreamTable contactData = new StreamTable();

        contactData.put(HSG.ORG_NAME, orgName);

        if (!((contactName == null) || (contactName.equals("")))) contactData.put(HSG.CONTACT_NAME, contactName);

        if (!((contactPhone == null) || (contactPhone.equals("")))) contactData.put(HSG.CONTACT_PHONE, contactPhone);

        contactData.put(HSG.CONTACT_EMAIL, contactEmail);

        return contactData;
    }

    /**
     * Write the replication-site file.
     */

    public static void writeReplicationSiteFile(String statDirName, String statFileName, File replicationSiteFile, SiteInfo replicationSite) throws IOException {
        // If there's an old status file, delete it
        File replStatFile = new File(statDirName, statFileName);
        if (replStatFile.exists()) replStatFile.delete();

        // Create the replication site file

        FileOutputStream replSiteOut = new FileOutputStream(replicationSiteFile);
        replSiteOut.write(Encoder.encodeSiteInfoRecord(replicationSite));
        replSiteOut.close();
    }

    /**
     * Create a "site bundle" of public information to be sent to hdladmin.
     */
    public static void createSiteBundle(String siteBundleDir, String siteBundleName, boolean isPrimary, String replicationAdminStr, File adminPubKeyFile, File replPubKeyFile, String replicationAuthStr, SiteInfo siteInfo,
        File contactDataFile, @SuppressWarnings("unused") boolean isDualStack) throws Exception {
        byte publicKey[] = null;
        // Create a Zip file

        ZipOutputStream bundle = new ZipOutputStream(new FileOutputStream(new File(siteBundleDir, siteBundleName)));

        if (isPrimary) { // Primary Server
            // Write the "replication admin string"
            // (encoded) into a zip entry
            bundle.putNextEntry(new ZipEntry(HSG.REPLICATION_ADMIN_FILE_NAME));
            bundle.write(Util.encodeString(replicationAdminStr + "\n")); // Why "\n" ???

            // Read the Admin Public Key file into publicKey[]
            // and write it into a Zip entry
            publicKey = contentsOf(adminPubKeyFile);
            bundle.putNextEntry(new ZipEntry(HSG.ADMIN_PUB_KEY_FILE_NAME));
            bundle.write(publicKey);
        } else { // Secondary ("Mirror") Server
            // Read the Replication Public Key file into publicKey[]
            // and write it into a Zip entry
            publicKey = contentsOf(replPubKeyFile);
            bundle.putNextEntry(new ZipEntry(HSG.REPLICATION_PUB_KEY_FILE_NAME));
            bundle.write(publicKey);
            // Write the "replication auth string"
            // (encoded) into a zip entry

            bundle.putNextEntry(new ZipEntry(HSG.REPLICATION_ID_FILE_NAME));
            bundle.write(Util.encodeString(replicationAuthStr + "\n")); // Why "\n" ???
        }

        // Write the siteInfo (encoded) into a Zip Entry
        bundle.putNextEntry(new ZipEntry(HSG.SITE_INFO_FILE_NAME));
        bundle.write(Encoder.encodeSiteInfoRecord(siteInfo));

        // Write the contactData (encoded text) into a Zip Entry

        String contents = charContentsOf(contactDataFile);
        bundle.putNextEntry(new ZipEntry(HSG.SITE_CONTACT_DATA_FILE_NAME));
        bundle.write(Util.encodeString(contents));

        bundle.close(); // Close the Zip file
    }

    /**
     * Read contents of file into a byte buffer and return the buffer.
     */
    private static byte[] contentsOf(File f) throws Exception // IOException, FileNotFoundException
    {
        int totalBytesRead = 0, bytesRead = 0;

        byte buffer[] = new byte[(int) f.length()]; // Allocate a buffer the size of the file

        FileInputStream inputStream = new FileInputStream(f); // Open the file

        // Get as much of the file on each read() as the
        // OS will allow, until the whole file is read

        while ((totalBytesRead < buffer.length) && ((bytesRead = inputStream.read(buffer, totalBytesRead, buffer.length - totalBytesRead)) >= 0)) {
            totalBytesRead += bytesRead;
        }
        inputStream.close(); // Close the file

        return buffer;
    }

    /**
     * Read contents of file into a char buffer, convert to a String,
     * and return the String.
     */
    private static String charContentsOf(File f) throws Exception // IOException, FileNotFoundException
    {
        int totalCharsRead = 0, charsRead = 0;

        char buffer[] = new char[(int) f.length()]; // Allocate a buffer the size of the file

        InputStreamReader reader = new InputStreamReader(new FileInputStream(f), "UTF-8"); // Open the file

        // Get as much of the file on each read() as the
        // OS will allow, until the whole file is read

        while ((totalCharsRead < buffer.length) && ((charsRead = reader.read(buffer, totalCharsRead, buffer.length - totalCharsRead)) >= 0)) {
            totalCharsRead += charsRead;
        }

        reader.close(); // Close the file

        return new String(buffer);
    }

    /**
     * Return true if argument is not of legal form "HH:MM:SS" or "H:MM:SS",
     * false otherwise.
     */

    public static final boolean badHHMMSS(String timeString) {
        if ((timeString != null) && (timeString.length() > 0)) {
            String tStr = timeString;

            if (tStr.length() == 7) { // Need leading 0?
                tStr = "0" + tStr;
            }
            if ((tStr.length() == 8) // Only acceptable length
                && (tStr.substring(2, 3).equals(":")) // Colons where needed?
                && (tStr.substring(5, 6).equals(":"))) {
                 try { // (-1 < HH < 24), (-1 < MM < 60), and (-1 < SS < 60)?
                     int h = Integer.parseInt(tStr.substring(0, 2)), m = Integer.parseInt(tStr.substring(3, 5)), s = Integer.parseInt(tStr.substring(6, 8));
                     return ((h < 0) || (h > 23) // h must be in 00..23,
                         || (m < 0) || (m > 59) // m and s in 00..59
                         || (s < 0) || (s > 59));
                 } catch (Exception e) {
                     /* Ignore Exception caused by garbage input */
                 }
            }
        }

        return true;
    }

    /**
     * Determine whether a character is legal in a phone number.
     */

    public static boolean validPhoneNumberChar(char c) {
        return (((c >= '0') && (c <= '9')) || (c == ' ') || (c == '-') || (c == '(') || (c == ')'));
    }

}
