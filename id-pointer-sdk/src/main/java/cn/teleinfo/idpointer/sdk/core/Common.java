/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
 All rights reserved.

 The HANDLE.NET software is made available subject to the
 Handle.Net Public License Agreement, which may be obtained at
 http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
 \**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***********************************************************************
 * This class holds all of the standard identifiers for the handle
 * library.
 ***********************************************************************/
public abstract class Common {
    public static final byte MAJOR_VERSION = 2;
    public static final byte MINOR_VERSION = 11;

    // If an AbstractRequest doesn't explicitly set a protocol version when contacting an interface directly (without knowing its SiteInfo), this is used.
    public static final byte COMPATIBILITY_MAJOR_VERSION = 2;
    public static final byte COMPATIBILITY_MINOR_VERSION = 10;
    public static final byte TELEINFO_MINOR_VERSION = 9;

    public static final byte EMPTY_BYTE_ARRAY[] = new byte[0];
    public static final String TEXT_ENCODING = "UTF8";

    public static final byte ST_NONE = 0;
    public static final byte ST_ADMIN = 1;
    public static final byte ST_RESOLUTION = 2;
    public static final byte ST_RESOLUTION_AND_ADMIN = 3;

    public static final byte BLANK_HANDLE[] = Util.encodeString("/");
    public static final byte GLOBAL_NA_PREFIX[] = Util.encodeString("0.");
    public static final byte GLOBAL_NA[] = Util.encodeString("0/");
    public static final byte NA_HANDLE_PREFIX_NOSLASH[] = Util.encodeString("0.NA");
    public static final byte NA_HANDLE_PREFIX[] = Util.encodeString("0.NA/");
    public static final byte TRUST_ROOT_HANDLE[] = Util.encodeString("0.0/0.0");
    public static final byte ROOT_HANDLE[] = Util.encodeString("0.NA/0.NA");
    public static final byte SPECIAL_DERIVED_MARKER[] = Util.encodeString("0.NA/0.NA/");

    public static final byte SITE_INFO_TYPE[] = Util.encodeString("HS_SITE");
    public static final String STR_SITE_INFO_TYPE = "HS_SITE";
    public static final byte SITE_INFO_6_TYPE[] = Util.encodeString("HS_SITE.6");
    public static final byte LEGACY_DERIVED_PREFIX_SITE_TYPE[] = Util.encodeString("HS_NA_DELEGATE");
    public static final byte DERIVED_PREFIX_SITE_TYPE[] = Util.encodeString("HS_SITE.PREFIX");
    public static final byte SERVICE_HANDLE_TYPE[] = Util.encodeString("HS_SERV");
    public static final byte DERIVED_PREFIX_SERVICE_HANDLE_TYPE[] = Util.encodeString("HS_SERV.PREFIX");
    public static final byte NAMESPACE_INFO_TYPE[] = Util.encodeString("HS_NAMESPACE");
    @Deprecated
    public static final byte MD5_SECRET_KEY_TYPE[] = Util.encodeString("HS_SECKEY");
    public static final byte SECRET_KEY_TYPE[] = Util.encodeString("HS_SECKEY");
    public static final byte PUBLIC_KEY_TYPE[] = Util.encodeString("HS_PUBKEY");
    public static final byte ADMIN_TYPE[] = Util.encodeString("HS_ADMIN");
    public static final byte ADMIN_GROUP_TYPE[] = Util.encodeString("HS_VLIST");
    public static final byte HS_SIGNATURE_TYPE[] = Util.encodeString("HS_SIGNATURE");
    public static final byte HS_CERT_TYPE[] = Util.encodeString("HS_CERT");

    public static final String STR_HS_SIGNATURE_TYPE = "HS_SIGNATURE";
    public static final String STR_HS_CERT_TYPE = "HS_CERT";

    public static final byte HASH_ALG_MD5[] = Util.encodeString("MD5");
    public static final byte HASH_ALG_SHA1[] = Util.encodeString("SHA1");
    public static final byte HASH_ALG_SHA1_ALTERNATE[] = Util.encodeString("SHA-1");
    public static final byte HASH_ALG_SHA256[] = Util.encodeString("SHA-256");
    public static final byte HASH_ALG_SHA256_ALTERNATE[] = Util.encodeString("SHA256");

    public static final byte HASH_ALG_SM3[] = Util.encodeString("SM3");

    public static final byte HASH_ALG_HMAC_SHA1[] = Util.encodeString("HMAC-SHA1");
    public static final byte HASH_ALG_HMAC_SHA256[] = Util.encodeString("HMAC-SHA256");

    public static final byte HASH_ALG_PBKDF2_HMAC_SHA1[] = Util.encodeString("PBKDF2-HMAC-SHA1");
    public static final byte HASH_ALG_PBKDF2_HMAC_SHA1_ALTERNATE[] = Util.encodeString("PBKDF2WithHmacSHA1");

    public static final byte SITE_INFO_TYPES[][] = {SITE_INFO_TYPE, SITE_INFO_6_TYPE};
    public static final byte DERIVED_PREFIX_SITE_INFO_TYPES[][] = {DERIVED_PREFIX_SITE_TYPE, LEGACY_DERIVED_PREFIX_SITE_TYPE};
    public static final byte SITE_INFO_INCL_PREFIX_TYPES[][] = {SITE_INFO_TYPE, SITE_INFO_6_TYPE, DERIVED_PREFIX_SITE_TYPE, LEGACY_DERIVED_PREFIX_SITE_TYPE};
    public static final byte SITE_INFO_AND_SERVICE_HANDLE_TYPES[][] = {SITE_INFO_TYPE, SITE_INFO_6_TYPE, SERVICE_HANDLE_TYPE};
    public static final byte SITE_INFO_AND_SERVICE_HANDLE_INCL_PREFIX_TYPES[][] = {SITE_INFO_TYPE, SITE_INFO_6_TYPE, SERVICE_HANDLE_TYPE, DERIVED_PREFIX_SITE_TYPE, LEGACY_DERIVED_PREFIX_SITE_TYPE, DERIVED_PREFIX_SERVICE_HANDLE_TYPE};
    public static final byte SITE_INFO_AND_SERVICE_HANDLE_AND_NAMESPACE_TYPES[][] = {SITE_INFO_TYPE, SITE_INFO_6_TYPE, SERVICE_HANDLE_TYPE, NAMESPACE_INFO_TYPE};
    public static final byte DERIVED_PREFIX_SITE_AND_SERVICE_HANDLE_TYPES[][] = {DERIVED_PREFIX_SITE_TYPE, LEGACY_DERIVED_PREFIX_SITE_TYPE, DERIVED_PREFIX_SERVICE_HANDLE_TYPE};
    public static byte[][] HS_SIGNATURE_TYPE_LIST = new byte[][]{Common.HS_SIGNATURE_TYPE};
    public static final byte SERVICE_HANDLE_TYPES[][] = {SERVICE_HANDLE_TYPE, DERIVED_PREFIX_SERVICE_HANDLE_TYPE};
    public static final byte LOCATION_TYPES[][] = {SITE_INFO_TYPE, SERVICE_HANDLE_TYPE, DERIVED_PREFIX_SERVICE_HANDLE_TYPE, SITE_INFO_6_TYPE, DERIVED_PREFIX_SITE_TYPE, LEGACY_DERIVED_PREFIX_SITE_TYPE, NAMESPACE_INFO_TYPE};
    @Deprecated
    public static final byte MD5_SECRET_KEY_TYPES[][] = {SECRET_KEY_TYPE};
    public static final byte SECRET_KEY_TYPES[][] = {SECRET_KEY_TYPE};
    public static final byte PUBLIC_KEY_TYPES[][] = {PUBLIC_KEY_TYPE};
    public static final byte ADMIN_TYPES[][] = {ADMIN_TYPE};
    public static final byte ADMIN_GROUP_TYPES[][] = {ADMIN_GROUP_TYPE};
    public static final byte LOCATION_AND_ADMIN_TYPES[][] = {SITE_INFO_TYPE, SITE_INFO_6_TYPE, DERIVED_PREFIX_SITE_TYPE, LEGACY_DERIVED_PREFIX_SITE_TYPE, SERVICE_HANDLE_TYPE, DERIVED_PREFIX_SERVICE_HANDLE_TYPE, NAMESPACE_INFO_TYPE,
            ADMIN_TYPE, ADMIN_GROUP_TYPE, PUBLIC_KEY_TYPE, SECRET_KEY_TYPE};

    public static final int ADMIN_INDEXES[] = {};

    public static final byte STD_TYPE_URL[] = Util.encodeString("URL");
    public static final byte STD_TYPE_EMAIL[] = Util.encodeString("EMAIL");
    public static final byte STD_TYPE_HSALIAS[] = Util.encodeString("HS_ALIAS");
    public static final byte STD_TYPE_HSSITE[] = Util.encodeString("HS_SITE");
    public static final byte STD_TYPE_HSSITE6[] = Util.encodeString("HS_SITE.6");
    public static final byte STD_TYPE_HSADMIN[] = Util.encodeString("HS_ADMIN");
    public static final byte STD_TYPE_HSSERV[] = Util.encodeString("HS_SERV");
    //  public static final byte STD_TYPE_HOSTNAME[] = Util.encodeString("INET_HOST");
    //  public static final byte STD_TYPE_URN[] = Util.encodeString("URN");

    public static final byte STD_TYPE_HSSECKEY[] = Util.encodeString("HS_SECKEY");
    public static final byte STD_TYPE_HSPUBKEY[] = Util.encodeString("HS_PUBKEY");
    public static final byte STD_TYPE_HSVALLIST[] = Util.encodeString("HS_VLIST");

    public static final byte STD_TYPES[][] = {STD_TYPE_URL, STD_TYPE_EMAIL, STD_TYPE_HSADMIN, STD_TYPE_HSALIAS, STD_TYPE_HSSITE, STD_TYPE_HSSITE6, STD_TYPE_HSSERV, STD_TYPE_HSSECKEY, STD_TYPE_HSPUBKEY, STD_TYPE_HSVALLIST,
            //                                             STD_TYPE_HOSTNAME,
            //                                             STD_TYPE_URN,
    };

    // codes identifying hash types (used in request digest encoding)
    public static final byte HASH_CODE_MD5_OLD_FORMAT = (byte) 0;
    public static final byte HASH_CODE_MD5 = (byte) 1;
    public static final byte HASH_CODE_SHA1 = (byte) 2;
    public static final byte HASH_CODE_SHA256 = (byte) 3;
    public static final byte HASH_CODE_HMAC_SHA1 = (byte) 0x12;
    public static final byte HASH_CODE_HMAC_SHA256 = (byte) 0x13;
    public static final byte HASH_CODE_PBKDF2_HMAC_SHA1 = (byte) 0x22;

    // credential type identifier indicating a message signed by a client for session fashion request submit
    public static final byte[] CREDENTIAL_TYPE_MAC = Util.encodeString("HS_MAC");

    // credential type identifier indicating a message signed by a server, or client for session setup
    public static final byte[] CREDENTIAL_TYPE_SIGNED = Util.encodeString("HS_SIGNED");

    // credential type identifier indicating a message signed by a server (old style, deprecated)
    public static final byte[] CREDENTIAL_TYPE_OLDSIGNED = Util.encodeString("HS_DSAPUBKEY");

    // codes identifying file-encryption schemes
    @Deprecated
    public static final int ENCRYPT_DES_ECB_PKCS5 = 0; // DES with ECB and PKCS5 padding
    public static final int ENCRYPT_NONE = 1; // no encryption
    public static final int ENCRYPT_DES_CBC_PKCS5 = 2; // DES with CBC and PKCS5 padding
    public static final int ENCRYPT_PBKDF2_DESEDE_CBC_PKCS5 = 3; // DESede with CBC and PKCS5 padding and PBKDF2 to derive encryption key
    public static final int ENCRYPT_PBKDF2_AES_CBC_PKCS5 = 4; // AES with CBC and PKCS5 padding and PBKDF2 to derive encryption key
    public static final int MAX_ENCRYPT = 9; // All file-encryption schemes must be smaller, and all public-key-encoding strings must be longer

    // identifier for the DSA private key encoding
    public static final byte KEY_ENCODING_DSA_PRIVATE[] = Util.encodeString("DSA_PRIV_KEY");

    // identifier for the DSA public key encoding
    public static final byte KEY_ENCODING_DSA_PUBLIC[] = Util.encodeString("DSA_PUB_KEY");

    // identifier for the DH private key encoding
    public static final byte KEY_ENCODING_DH_PRIVATE[] = Util.encodeString("DH_PRIV_KEY");

    // identifier for the DH public key encoding
    public static final byte KEY_ENCODING_DH_PUBLIC[] = Util.encodeString("DH_PUB_KEY");

    // identifier for the RSA private key and private crt key encoding
    public static final byte KEY_ENCODING_RSA_PRIVATE[] = Util.encodeString("RSA_PRIV_KEY");
    public static final byte KEY_ENCODING_RSACRT_PRIVATE[] = Util.encodeString("RSA_PRIVCRT_KEY");

    // identifier for the RSA public key encoding
    public static final byte KEY_ENCODING_RSA_PUBLIC[] = Util.encodeString("RSA_PUB_KEY");

    // identifier for
    // SM2_PUB_KEY
    // SM2_PRIV_KEY
    // SM9_PRIV_KEY
    // SM9_PUB_KEY
    public static final byte KEY_ENCODING_SM2_PUB_KEY[] = Util.encodeString("SM2_PUB_KEY");

    public static final byte KEY_ENCODING_SM2_PRIV_KEY[] = Util.encodeString("SM2_PRIV_KEY");

    public static final byte KEY_ENCODING_SM9_PUB_KEY[] = Util.encodeString("SM9_PUB_KEY");

    public static final byte KEY_ENCODING_SM9_PRIV_KEY[] = Util.encodeString("SM9_PRIV_KEY");





    // format version number for site records
    public static final int SITE_RECORD_FORMAT_VERSION = 1;

    // size (in bytes) of the "nonce" sent to clients for authentication
    public static final int CHALLENGE_NONCE_SIZE = 16;

    // size (in bytes) of an MD5 digest
    public static final int MD5_DIGEST_SIZE = 16;

    // size (in bytes) of an SHA1 digest
    public static final int SHA1_DIGEST_SIZE = 20;

    // size (in bytes) of an SHA256 digest
    public static final int SHA256_DIGEST_SIZE = 32;

    // static size of message header (in bytes)
    public static final int MESSAGE_HEADER_SIZE = 24;

    // static size of message envelope (in bytes)
    public static final int MESSAGE_ENVELOPE_SIZE = 20;

    // maximum allowable size (in bytes) of a message
    public static final int MAX_MESSAGE_LENGTH = 262144;

    // public static final int MAX_MESSAGE_DATA_SIZE = MAX_MESSAGE_LENGTH - MESSAGE_ENVELOPE_SIZE;
    public static final int MAX_MESSAGE_DATA_SIZE = MAX_MESSAGE_LENGTH - MESSAGE_ENVELOPE_SIZE;

    // maximum size of udp packets.  packets in multi-packet
    // messages must be as large as possible equal to or below
    // this limit.
    public static final int MAX_UDP_PACKET_SIZE = 512;

    // the maximum size of the data portion of a UDP packet
    // (ie the non-envelope portion)
    public static final int MAX_UDP_DATA_SIZE = MAX_UDP_PACKET_SIZE - MESSAGE_ENVELOPE_SIZE;

    // the maximum number of handle values which are allowed in a message
    public static final int MAX_HANDLE_VALUES = 2048;

    // the maximum length of a handle
    public static final int MAX_HANDLE_LENGTH = 2048;

    // limit all arrays in messages to one million elements max
    public static final int MAX_ARRAY_SIZE = 1048576;

    // IP address length
    public static final int IP_ADDRESS_LENGTH = 16;

    public static final String HDL_MIME_TYPE = "application/x-hdl-message";
    public static final String XML_MIME_TYPE = "text/xml";

    //for session setup request, exchange key type
    public static final int KEY_EXCHANGE_NONE = 0; // no session
    public static final int KEY_EXCHANGE_CIPHER_CLIENT = 1; // Use client pub key
    public static final int KEY_EXCHANGE_CIPHER_SERVER = 2; // Use server pub key
    public static final int KEY_EXCHANGE_CIPHER_HDL = 3; // Use key from hdl
    public static final int KEY_EXCHANGE_DH = 4; // diffie hellman

    // size (in bytes) of the session key sent between client and server
    public static final int SESSION_KEY_SIZE = 512;

    //default server session time out 24 hours (in seconds)
    public static final int DEFAULT_SESSION_TIMEOUT = 24 * 60 * 60;

    public static final String READ_ONLY_DB_STORAGE_KEY = "read_only";

}
