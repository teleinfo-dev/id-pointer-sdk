/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class HSG {
    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // INTEGERS
    public static final int SVR_TYPE_SERVER      =        0,
        SVR_TYPE_CACHE       =        1,
        BACKLOG              =        50,
        THREAD_COUNT         =       15,
        MAX_AUTH_TIME        =    60000,            // 1 minute
        MAX_SESSION_TIME     = 86400000,            // 24 hours
        KEY_STRENGTH         =     2048,
        DEFAULT_TCP_UDP_PORT =     2641,
        DEFAULT_HTTP_PORT    =     8000,
        EMAIL_PORT           =       25,
        LOWEST_PORT          =        1,
        HIGHEST_PORT         =    32000,
        IP_VERSION_6         =        6,
        IP_VERSION_4         =        4,
        IP_EITHER_VERSION    =        0;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // BOOLEANS
    public static final boolean CASE_SENSITIVE   = true,
        CASE_INSENSITIVE = false,

        APPEND           = true,          // File-open args
        NO_APPEND        = false,
        TRUNCATE         = NO_APPEND;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // STRINGS
    // - - - - - - - - - - - - - - - - - - -
    // File/directory names
    public static final String ACCESS_LOG_FILE_NAME_BASE      = "access.log",
        ADMIN_PRIV_KEY_FILE_NAME       = "admpriv.bin",
        ADMIN_PUB_KEY_FILE_NAME        = "admpub.bin",
        CONFIG_FILE_NAME               = "config.dct",
        DEFAULT_CONFIG_SUBDIR_NAME     = "/hs/svr_1",
        ERROR_LOG_FILE_NAME_BASE       = "error.log",
        EXTRA_LOG_FILE_NAME_BASE       = "extra.log",
        PRIV_KEY_FILE_NAME             = "privkey.bin",
        PUB_KEY_FILE_NAME              = "pubkey.bin",
        REPLICATION_ADMIN_FILE_NAME    = "repl_admin",
        REPLICATION_ID_FILE_NAME       = "replid.txt",
        REPLICATION_PRIV_KEY_FILE_NAME = "replpriv.bin",
        REPLICATION_PUB_KEY_FILE_NAME  = "replpub.bin",
        RSA_PRIV_KEY_FILE_NAME         = "rsapriv.bin",
        RSA_PUB_KEY_FILE_NAME          = "rsapub.bin",
        SITE_BUNDLE_ZIPFILE_NAME       = "sitebndl.zip",
        SITE_CONTACT_DATA_FILE_NAME    = "contactdata.dct",
        SITE_INFO_FILE_NAME            = "siteinfo.bin",
        SITE_INFO_JSON_FILE_NAME       = "siteinfo.json",
        TXN_STAT_FILE_NAME             = "txnstat.dct",
        WINDOWS_CONFIG_DIR_NAME        = "C:\\hs\\svr_1\\",

        // - - - - - - - - - - - - - - - - - - -
        // Keys and defined values for config.dct
        NO_UDP                         = "no_udp_resolution",
        INTERFACES                     = "interfaces",
        TCP_CONFIG                     = "hdl_tcp_config",
        HTTP_CONFIG                    = "hdl_http_config",
        UDP_CONFIG                     = "hdl_udp_config",
        TCP4_CONFIG                    = "hdl_tcp4_config",
        HTTP4_CONFIG                   = "hdl_http4_config",
        UDP4_CONFIG                    = "hdl_udp4_config",
        DNS_UDP_CONFIG                 = "dns_udp_config",
        DNS_TCP_CONFIG                 = "dns_tcp_config",
        LOG_SAVE_CONFIG                = "log_save_config",
        SERVER_TYPE                    = "server_type",
        SERVER_CONFIG                  = "server_config",
        CACHE_CONFIG                   = "cache_config",
        LOG_ACCESSES                   = "log_accesses",
        DNS_CONFIG                     = "dns_config",

        // Log-related stuff
        LOG_CONFIG                     = "log_save_config",
        LOG_SAVE_INTERVAL              = "log_save_interval",
        LOG_SAVE_WEEKDAY               = "log_save_weekday",
        LOG_SAVE_TIME                  = "log_save_time",
        LOG_SAVE_DIRECTORY             = "log_save_directory",
        LOG_REDIRECT_STDERR            = "log_redirect_stderr",
        MONTHLY                        = "Monthly",
        WEEKLY                         = "Weekly",
        DAILY                          = "Daily",
        NEVER                          = "Never",

        // - - - - - - - - - - - - - - - - - - -
        // Name strings for "Attributes"
        // SiteInfo Attribute
        DESCRIPTION                    = "desc",

        // SiteContactData Attributes
        ORG_NAME                       = "org_name",
        CONTACT_NAME                   = "contact_name",
        CONTACT_PHONE                  = "contact_phone",
        CONTACT_EMAIL                  = "contact_email",

        // - - - - - - - - - - - - - - - - - - -
        // Other strings
        HS_JAVA_VERSION                = "1.6",
        DEFAULT_REPLICATION_GROUP      = "300:0.NA/YOUR_PREFIX",
        DEFAULT_REPLICATION_ID         = "300:0.NA/YOUR_PREFIX",
        DEFAULT_SERVER_ADMIN           = "300:0.NA/YOUR_PREFIX",
        DEFAULT_HOMED_PREFIX           = "0.NA/YOUR_PREFIX",

        KEY_ALGORITHM                  = "RSA",

        YES                            = "yes",
        NO                             = "no",
        NOT_APPL                       = "N/A", // Not applicable

        SUNDAY                         = "Sunday",
        MONDAY                         = "Monday",
        TUESDAY                        = "Tuesday",
        WEDNESDAY                      = "Wednesday",
        THURSDAY                       = "Thursday",
        FRIDAY                         = "Friday",
        SATURDAY                       = "Saturday";
}
