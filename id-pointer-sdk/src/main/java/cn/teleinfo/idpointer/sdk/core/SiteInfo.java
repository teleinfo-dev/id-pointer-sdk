/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
 All rights reserved.

 The HANDLE.NET software is made available subject to the
 Handle.Net Public License Agreement, which may be obtained at
 http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
 \**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class SiteInfo {
    public static final String DOMAIN = "domain";
    public static final String PATH = "path";

    public static final byte HASH_TYPE_BY_PREFIX = 0;
    public static final byte HASH_TYPE_BY_SUFFIX = 1;
    public static final byte HASH_TYPE_BY_ALL = 2;

    public static final short PRIMARY_SITE = 0x80;
    public static final short MULTI_PRIMARY = 0x40;

    public int dataFormatVersion = Common.SITE_RECORD_FORMAT_VERSION;
    public int serialNumber;

    public byte majorProtocolVersion = Common.MAJOR_VERSION;
    public byte minorProtocolVersion = Common.MINOR_VERSION;

    public boolean isPrimary;
    // This flag in the SiteInfo reflects a handle service with multiple primary
    // servers, and not necessarily shared administration by diverse organizations
    public boolean multiPrimary;
    public boolean isRoot = false;

    public long responseTime; // Last response time, for setting site selection
    // preference

    public byte hashOption = HASH_TYPE_BY_ALL;
    public byte[] hashFilter;

    public ServerInfo[] servers;

    public Attribute[] attributes;

    /**
     * Default constructor: object uninitialized except as above.
     */
    public SiteInfo() {
    }

    /**
     * Constructor used by configuration routines.
     */
    public SiteInfo(int siteVersion, boolean isPrimary, boolean isMultiPrimary, byte hashingOption, String siteDescription, InetAddress listenAddr, int port, int httpPort, File pubKeyFile, boolean disableUDP) throws IOException {
        serialNumber = siteVersion;
        this.isPrimary = isPrimary;
        multiPrimary = isMultiPrimary;
        hashOption = hashingOption;

        if (siteDescription != null)
            attributes = new Attribute[]{new Attribute(Util.encodeString(HSG.DESCRIPTION), Util.encodeString(siteDescription))};

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        // Support only 1 server per site

        servers = new ServerInfo[]{new ServerInfo()}; // Array of 1
        servers[0].serverId = 1; // First and only

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        byte[] addr1 = listenAddr.getAddress(); // IP Address
        byte[] addr2 = new byte[Common.IP_ADDRESS_LENGTH];

        for (int i = 0; i < Common.IP_ADDRESS_LENGTH; i++) {
            addr2[i] = (byte) 0;
        }
        System.arraycopy(addr1, 0, addr2, addr2.length - addr1.length, addr1.length);

        servers[0].ipAddress = addr2;

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        byte[] pkbuf = new byte[(int) pubKeyFile.length()]; // Public key

        FileInputStream pubKeyIn = new FileInputStream(pubKeyFile);

        int n = 0;
        int r;

        while ((n < pkbuf.length) && ((r = pubKeyIn.read(pkbuf, n, pkbuf.length - n)) >= 0)) {
            n += r;
        }
        pubKeyIn.close();

        servers[0].publicKey = pkbuf;
        // Interfaces
        if (disableUDP) {
            servers[0].interfaces = new Interface[]{new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_TCP, port), new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_HTTP, httpPort)};
        } else {
            servers[0].interfaces = new Interface[]{new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_TCP, port), new Interface(Interface.ST_QUERY, Interface.SP_HDL_UDP, port),
                    new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_HTTP, httpPort)};
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -
    }

    public SiteInfo(int siteVersion, boolean isPrimary, boolean isMultiPrimary, byte hashingOption, String siteDescription, InetAddress listenAddr, InetAddress altAddr, int port, int httpPort, File pubKeyFile, boolean disableUDP)
            throws IOException {
        this(siteVersion, isPrimary, isMultiPrimary, hashingOption, siteDescription, listenAddr, port, httpPort, pubKeyFile, disableUDP);

        Attribute desc = attributes[0];
        attributes = new Attribute[2];
        attributes[0] = desc;

        Attribute altAddrAttribute = new Attribute();
        altAddrAttribute.name = Util.encodeString("alt_addr.1");
        altAddrAttribute.value = Util.encodeString(Util.rfcIpRepr(altAddr));

        attributes[1] = altAddrAttribute;

    }

    public SiteInfo(int siteVersion, boolean isPrimary, boolean isMultiPrimary, byte hashingOption, String siteDescription, InetAddress listenAddr, int port, int httpPort, byte[] pubKeyBytes, boolean disableUDP) {
        serialNumber = siteVersion;
        this.isPrimary = isPrimary;
        multiPrimary = isMultiPrimary;
        hashOption = hashingOption;

        if (siteDescription != null)
            attributes = new Attribute[]{new Attribute(Util.encodeString(HSG.DESCRIPTION), Util.encodeString(siteDescription))};

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        // Support only 1 server per site

        servers = new ServerInfo[]{new ServerInfo()}; // Array of 1
        servers[0].serverId = 1; // First and only

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        byte[] addr1 = listenAddr.getAddress(); // IP Address
        byte[] addr2 = new byte[Common.IP_ADDRESS_LENGTH];

        for (int i = 0; i < Common.IP_ADDRESS_LENGTH; i++) {
            addr2[i] = (byte) 0;
        }
        System.arraycopy(addr1, 0, addr2, addr2.length - addr1.length, addr1.length);

        servers[0].ipAddress = addr2;

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        // PublicKey publicKey = KeyConverter.fromX509Pem(pubKeyPem);
        // byte pkbuf[] = Util.getBytesFromPublicKey(publicKey);
        servers[0].publicKey = pubKeyBytes;
        // Interfaces
        if (disableUDP) {
            servers[0].interfaces = new Interface[]{new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_TCP, port), new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_HTTP, httpPort)};
        } else {
            servers[0].interfaces = new Interface[]{new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_TCP, port), new Interface(Interface.ST_QUERY, Interface.SP_HDL_UDP, port),
                    new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_HTTP, httpPort)};
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -
    }


    public SiteInfo(int siteVersion, boolean isPrimary, boolean isMultiPrimary, byte hashingOption, String siteDescription, InetAddress listenAddr, int tcpPort, Integer udpPort, Integer httpPort, byte[] pubKeyBytes) {
        serialNumber = siteVersion;
        this.isPrimary = isPrimary;
        multiPrimary = isMultiPrimary;
        hashOption = hashingOption;

        if (siteDescription != null)
            attributes = new Attribute[]{new Attribute(Util.encodeString(HSG.DESCRIPTION), Util.encodeString(siteDescription))};

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        // Support only 1 server per site

        servers = new ServerInfo[]{new ServerInfo()}; // Array of 1
        servers[0].serverId = 1; // First and only

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        byte[] addr1 = listenAddr.getAddress(); // IP Address
        byte[] addr2 = new byte[Common.IP_ADDRESS_LENGTH];

        for (int i = 0; i < Common.IP_ADDRESS_LENGTH; i++) {
            addr2[i] = (byte) 0;
        }
        System.arraycopy(addr1, 0, addr2, addr2.length - addr1.length, addr1.length);

        servers[0].ipAddress = addr2;

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        // PublicKey publicKey = KeyConverter.fromX509Pem(pubKeyPem);
        // byte pkbuf[] = Util.getBytesFromPublicKey(publicKey);
        servers[0].publicKey = pubKeyBytes;

        List<Interface> interfaceList = new ArrayList<>(3);
        interfaceList.add(new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_TCP, tcpPort));

        if (udpPort != null) {
            interfaceList.add(new Interface(Interface.ST_QUERY, Interface.SP_HDL_UDP, udpPort));
        }

        if (httpPort != null) {
            interfaceList.add(new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_HTTP, httpPort));
        }


        Interface[] interfaces = new Interface[interfaceList.size()];
        interfaceList.toArray(interfaces);

        // Interfaces
        servers[0].interfaces = interfaces;

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -
    }

    public SiteInfo(int siteVersion, boolean isPrimary, boolean isMultiPrimary, byte hashingOption, String siteDescription, InetAddress listenAddr, int port, byte[] pubKeyBytes, boolean disableUDP) {
        serialNumber = siteVersion;
        this.isPrimary = isPrimary;
        multiPrimary = isMultiPrimary;
        hashOption = hashingOption;

        if (siteDescription != null)
            attributes = new Attribute[]{new Attribute(Util.encodeString(HSG.DESCRIPTION), Util.encodeString(siteDescription))};

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        // Support only 1 server per site

        servers = new ServerInfo[]{new ServerInfo()}; // Array of 1
        servers[0].serverId = 1; // First and only

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        byte[] addr1 = listenAddr.getAddress(); // IP Address
        byte[] addr2 = new byte[Common.IP_ADDRESS_LENGTH];

        for (int i = 0; i < Common.IP_ADDRESS_LENGTH; i++) {
            addr2[i] = (byte) 0;
        }
        System.arraycopy(addr1, 0, addr2, addr2.length - addr1.length, addr1.length);

        servers[0].ipAddress = addr2;

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -

        // PublicKey publicKey = KeyConverter.fromX509Pem(pubKeyPem);
        // byte pkbuf[] = Util.getBytesFromPublicKey(publicKey);
        servers[0].publicKey = pubKeyBytes;
        // Interfaces
        if (disableUDP) {
            servers[0].interfaces = new Interface[]{new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_TCP, port)};
        } else {
            servers[0].interfaces = new Interface[]{new Interface(Interface.ST_ADMIN_AND_QUERY, Interface.SP_HDL_TCP, port), new Interface(Interface.ST_QUERY, Interface.SP_HDL_UDP, port)};
        }

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // - - -
    }

    public SiteInfo(SiteInfo site) {
        this.serialNumber = site.serialNumber;
        this.isPrimary = site.isPrimary;
        this.multiPrimary = site.multiPrimary;
        this.hashOption = site.hashOption;
        this.dataFormatVersion = site.dataFormatVersion;
        this.isRoot = site.isRoot;
        this.responseTime = site.responseTime;
        this.majorProtocolVersion = site.majorProtocolVersion;
        this.minorProtocolVersion = site.minorProtocolVersion;
        if (site.hashFilter == null) {
            this.hashFilter = new byte[0];
        } else {
            this.hashFilter = new byte[site.hashFilter.length];
            System.arraycopy(site.hashFilter, 0, this.hashFilter, 0, site.hashFilter.length);
        }

        if (site.attributes != null) {
            this.attributes = new Attribute[site.attributes.length];
            for (int i = 0; i < site.attributes.length; i++) {
                this.attributes[i] = new Attribute(site.attributes[i].name, site.attributes[i].value);
            }
        }
        if (site.servers != null) {
            this.servers = new ServerInfo[site.servers.length];
            for (int i = 0; i < site.servers.length; i++) {
                this.servers[i] = site.servers[i].cloneServerInfo();
            }
        }
    }

    /**
     * Get the value of the specified attribute for this site (if any).
     */
    public byte[] getAttribute(byte[] attribute) {
        if (attributes != null) {
            for (Attribute existingAttribute : attributes) {
                if (Util.equals(attribute, existingAttribute.name)) {
                    return existingAttribute.value;
                }
            }
        }
        return null;
    }

    /**
     * Return the positive integer generated by hashing the part of this handle
     * indicated by hashOption.
     */
    public static int getHandleHash(byte[] handle, int hashOption) throws HandleException {
        byte[] hashPart;

        switch (hashOption) {
            case HASH_TYPE_BY_PREFIX:
                hashPart = Util.upperCaseInPlace(Util.getPrefixPart(handle));
                break;

            case HASH_TYPE_BY_SUFFIX:
                hashPart = Util.upperCaseInPlace(Util.getSuffixPart(handle));
                break;

            case HASH_TYPE_BY_ALL:
                hashPart = Util.upperCase(handle);
                break;

            default:
                throw new HandleException(HandleException.INVALID_VALUE, "Unknown hash method: " + hashOption);
        }

        // Get the 32 bits of the hashvalue
        // modulo the number of (primary) buckets
        byte[] digest = Util.doMD5Digest(hashPart);
        return Math.abs(Encoder.readInt(digest, digest.length - 4));
    }

    /**
     * Shortcut to determineServerNum(handle, hashOption, numServers) using
     * this.hashOption
     */

    public final int determineServerNum(byte[] handle) throws HandleException {
        return determineServerNum(handle, hashOption, servers.length);
    }

    /**
     * Return the index of the server that this handle hashes to
     */
    public static int determineServerNum(byte[] handle, int hashOption, int numServers) throws HandleException {
        return getHandleHash(handle, hashOption) % numServers;
    }

    /**
     * Return the ServerInfo that this handle hashes to
     */
    public ServerInfo determineServer(byte[] handle) throws HandleException {
        return servers[determineServerNum(handle, hashOption, servers.length)];
    }

    /**
     * Return a string of labeled members of this object.
     */
    @Override
    public String toString() {
        // Change servers[] into comma-and-space-separated string

        StringBuilder servList = new StringBuilder();
        if (servers != null) {
            //servList = servList + servers[0];
            servList.append(servers[0]);
            for (int i = 1; i < servers.length; i++) {
                //servList = servList+", " + servers[i];
                servList.append(", ").append(servers[i]);
            }
        }

        return "version: " + majorProtocolVersion + '.' + minorProtocolVersion + "; serial:" + serialNumber + "; primary:" + (isPrimary ? "y; " : "n; ") + "servers=[" + servList + "]";
    }

    public HashMap<Integer, ServerInfo> getId2ServerMap() {
        HashMap<Integer, ServerInfo> id2Server = new HashMap<>();

        for (ServerInfo server : servers) {
            int id = server.serverId;

            id2Server.put(id, server);
        }

        return id2Server;
    }

    /**
     * Hash the given arguments as handles and display the results.
     */
    public static void main(String[] argv) throws Exception {
        for (String hdl : argv) {
            System.out.println("Handle: " + hdl);
            int hashResult;
            hashResult = getHandleHash(Util.encodeString(hdl), HASH_TYPE_BY_PREFIX);
            System.out.println("  hash by prefix: " + hashResult + "; #servers: 2=" + (hashResult % 2) + ", 3=" + (hashResult % 3) + ", 4=" + (hashResult % 4));
            hashResult = getHandleHash(Util.encodeString(hdl), HASH_TYPE_BY_SUFFIX);
            System.out.println("  hash by suffix: " + hashResult + "; #servers: 2=" + (hashResult % 2) + ", 3=" + (hashResult % 3) + ", 4=" + (hashResult % 4));
            hashResult = getHandleHash(Util.encodeString(hdl), HASH_TYPE_BY_ALL);
            System.out.println("  hash by all: " + hashResult + "; #servers: 2=" + (hashResult % 2) + ", 3=" + (hashResult % 3) + ", 4=" + (hashResult % 4));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(emptyToNull(attributes));
        result = prime * result + dataFormatVersion;
        result = prime * result + Arrays.hashCode(emptyToNull(hashFilter));
        result = prime * result + hashOption;
        result = prime * result + (isPrimary ? 1231 : 1237);
        result = prime * result + majorProtocolVersion;
        result = prime * result + minorProtocolVersion;
        result = prime * result + (multiPrimary ? 1231 : 1237);
        result = prime * result + serialNumber;
        result = prime * result + Arrays.hashCode(emptyToNull(servers));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SiteInfo other = (SiteInfo) obj;
        if (!Arrays.equals(emptyToNull(attributes), emptyToNull(other.attributes))) return false;
        if (dataFormatVersion != other.dataFormatVersion) return false;
        if (!Arrays.equals(emptyToNull(hashFilter), emptyToNull(other.hashFilter))) return false;
        if (hashOption != other.hashOption) return false;
        if (isPrimary != other.isPrimary) return false;
        if (majorProtocolVersion != other.majorProtocolVersion) return false;
        if (minorProtocolVersion != other.minorProtocolVersion) return false;
        if (multiPrimary != other.multiPrimary) return false;
        if (serialNumber != other.serialNumber) return false;

        return Arrays.equals(emptyToNull(servers), emptyToNull(other.servers));
    }

    private static byte[] emptyToNull(byte[] array) {
        if (array == null || array.length == 0) return null;
        return array;
    }

    private static <T> T[] emptyToNull(T[] array) {
        if (array == null || array.length == 0) return null;
        return array;
    }

    public String getAttributeForServer(String attribute, int which) {
        byte[] domainBytes = null;
        if (which >= 0) domainBytes = getAttribute(Util.encodeString(attribute + "." + which));
        if (domainBytes == null) domainBytes = getAttribute(Util.encodeString(attribute));
        if (domainBytes == null) return null;
        return Util.decodeString(domainBytes);
    }

    public String getDomainForServer(int which) {
        return getAttributeForServer(DOMAIN, which);
    }

    public boolean hasZeroAddressServersAndDomain() {
        for (int i = 0; i < servers.length; i++) {
            ServerInfo server = servers[i];
            if (server.hasAllZerosAddress()) {
                if (getDomainForServer(i) != null) return true;
            }
        }
        return false;
    }
}
