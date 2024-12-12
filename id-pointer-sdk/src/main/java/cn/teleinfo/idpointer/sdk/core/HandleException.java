/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class HandleException extends Exception {
    public static final int INVALID_VALUE = 0; // thrown by resolver and server
    public static final int INTERNAL_ERROR = 1; // thrown by resolver and server
    public static final int SERVICE_NOT_FOUND = 2; // thrown by resolver
    public static final int NO_ACCEPTABLE_INTERFACES = 3; // thrown by resolver
    public static final int UNKNOWN_PROTOCOL = 4; // thrown by resolver, bad data in SiteInfo
    public static final int HANDLE_ALREADY_EXISTS = 5; // thrown by storage
    public static final int MESSAGE_FORMAT_ERROR = 6; // thrown by resolver, and server http only
    public static final int CANNOT_CONNECT_TO_SERVER = 7; // thrown by resolver
    public static final int UNABLE_TO_AUTHENTICATE = 8; // thrown by resolver
    public static final int HANDLE_DOES_NOT_EXIST = 9; // thrown by storage, thrown by resolver when get that message from server
    public static final int SECURITY_ALERT = 10; // thrown by resolver
    public static final int CONFIGURATION_ERROR = 11; // thrown by server
    public static final int REPLICATION_ERROR = 12; // thrown by server
    public static final int MISSING_OR_INVALID_SIGNATURE = 13; // thrown by resolver
    public static final int MISSING_CRYPTO_PROVIDER = 14; // thrown by resolver and (once) server
    public static final int SERVER_ERROR = 15; // thrown by resolver when error message from server; thrown by server rarely
    public static final int UNKNOWN_ALGORITHM_ID = 16; // thrown by resolver
    public static final int GOT_EXPIRED_MESSAGE = 17; // thrown by resolver
    public static final int STORAGE_RDONLY = 18; // thrown by storage
    public static final int UNABLE_TO_SIGN_REQUEST = 19; // thrown by resolver
    public static final int INVALID_SESSION_EXCHANGE_PRIVKEY = 20; // unused
    public static final int NEED_RSAKEY_FOR_SESSIONEXCHANGE = 21; // unused
    public static final int NEED_PUBLICKEY_FOR_SESSIONIDENTITY = 22; // unused
    public static final int SESSION_TIMEOUT = 23; // unused
    public static final int INCOMPLETE_SESSIONSETUP = 24; // thrown by resolver
    public static final int SERVER_CANNOT_PROCESS_SESSION = 25; // thrown by resolver when server response unexpected
    public static final int ENCRYPTION_ERROR = 26; // thrown by resolver
    public static final int OTHER_CONNECTION_ESTABLISHED = 27; // only used internally
    public static final int DUPLICATE_SESSION_COUNTER = 28; // used by server, becomes error response; also resolver
    public static final int SERVICE_REFERRAL_ERROR = 29; // used by resolver

    public static final String OTHER_CONNECTION_ESTABLISHED_STRING = "This request has been fully processed by another thread.";

    public static final String getCodeStr(int c) {
        switch (c) {
        case INVALID_VALUE:
            return "INVALID_VALUE";
        case INTERNAL_ERROR:
            return "INTERNAL_ERROR";
        case SERVICE_NOT_FOUND:
            return "SERVICE_NOT_FOUND";
        case NO_ACCEPTABLE_INTERFACES:
            return "NO_ACCEPTABLE_INTERFACES";
        case UNKNOWN_PROTOCOL:
            return "UNKNOWN_PROTOCOL";
        case HANDLE_ALREADY_EXISTS:
            return "HANDLE_ALREADY_EXISTS";
        case MESSAGE_FORMAT_ERROR:
            return "MESSAGE_FORMAT_ERROR";
        case CANNOT_CONNECT_TO_SERVER:
            return "CANNOT_CONNECT_TO_SERVER";
        case UNABLE_TO_AUTHENTICATE:
            return "UNABLE_TO_AUTHENTICATE";
        case HANDLE_DOES_NOT_EXIST:
            return "HANDLE_DOES_NOT_EXIST";
        case SECURITY_ALERT:
            return "SECURITY_ALERT";
        case CONFIGURATION_ERROR:
            return "CONFIGURATION_ERROR";
        case REPLICATION_ERROR:
            return "REPLICATION_ERROR";
        case UNKNOWN_ALGORITHM_ID:
            return "UNKNOWN_ALGORITHM_ID";
        case MISSING_OR_INVALID_SIGNATURE:
            return "MISSING_OR_INVALID_SIGNATURE";
        case MISSING_CRYPTO_PROVIDER:
            return "MISSING_CRYPTO_PROVIDER";
        case SERVER_ERROR:
            return "SERVER_ERROR";
        case GOT_EXPIRED_MESSAGE:
            return "GOT_EXPIRED_MESSAGE";
        case STORAGE_RDONLY:
            return "STORAGE_RDONLY";
        case UNABLE_TO_SIGN_REQUEST:
            return "UNABLE_TO_SIGN_REQUEST";
        case INVALID_SESSION_EXCHANGE_PRIVKEY:
            return "INVALID_SESSION_EXCHANGE_PRIVKEY";
        case NEED_RSAKEY_FOR_SESSIONEXCHANGE:
            return "NEED_RSAKEY_FOR_SESSIONEXCHANGE";
        case NEED_PUBLICKEY_FOR_SESSIONIDENTITY:
            return "NEED_PUBLICKEY_FOR_SESSIONIDENTITY";
        case SESSION_TIMEOUT:
            return "SESSION_TIMEOUT";
        case INCOMPLETE_SESSIONSETUP:
            return "INCOMPLETE_SESSIONSETUP";
        case SERVER_CANNOT_PROCESS_SESSION:
            return "SERVER_CANNOT_PROCESS_SESSION";
        case ENCRYPTION_ERROR:
            return "ENCRYPTION_ERROR";
        case OTHER_CONNECTION_ESTABLISHED:
            return "OTHER_CONNECTION_ESTABLISHED";
        case DUPLICATE_SESSION_COUNTER:
            return "DUPLICATE_SESSION_COUNTER";
        case SERVICE_REFERRAL_ERROR:
            return "SERVICE_REFERRAL_ERROR";
        default:
            return "UNKNOWN_ERROR(" + c + ")";
        }
    }

    private final int code;

    public HandleException(int code) {
        super();
        this.code = code;
    }

    public HandleException(int code, String message) {
        super(message);
        this.code = code;
    }

    public HandleException(int code, Throwable throwable) {
        super(throwable);
        this.code = code;
    }

    public HandleException(int code, String message, Throwable throwable) {
        super(message, throwable);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        String msg = getMessage();
        if (msg == null) msg = "";
        return "HandleException (" + getCodeStr(code) + ") " + msg;
    }

    private static final int responseCodeFromExceptionCodeThrownByResolver(int c) {
        switch (c) {
        case SERVICE_NOT_FOUND:
            return AbstractMessage.RC_HANDLE_NOT_FOUND;
        // could be RC_AUTHENTICATION_NEEDED, but that is only for challenge responses;
        // not applicable here
        case UNABLE_TO_AUTHENTICATE:
            return AbstractMessage.RC_AUTHEN_ERROR;
        case HANDLE_DOES_NOT_EXIST:
            return AbstractMessage.RC_HANDLE_NOT_FOUND;
        default:
            return AbstractMessage.RC_ERROR;
        }
    }

    public ErrorIdResponse toErrorResponse(AbstractIdRequest req) {
        try {
            if (req != null) return new ErrorIdResponse(req, responseCodeFromExceptionCodeThrownByResolver(this.code), Util.encodeString(this.toString()));
        } catch (HandleException e) {
        }
        return new ErrorIdResponse(AbstractMessage.OC_RESERVED, responseCodeFromExceptionCodeThrownByResolver(this.code), Util.encodeString(this.toString()));
    }

    public static ErrorIdResponse toErrorResponse(AbstractIdRequest req, Exception e) {
        if (e instanceof HandleException) return ((HandleException) e).toErrorResponse(req);
        try {
            if (req != null) return new ErrorIdResponse(req, AbstractMessage.RC_ERROR, Util.encodeString(e.toString()));
        } catch (HandleException he) {
        }
        return new ErrorIdResponse(AbstractMessage.OC_RESERVED, AbstractMessage.RC_ERROR, Util.encodeString(e.toString()));
    }

    public static HandleException ofResponse(AbstractIdResponse response) {
        switch (response.responseCode) {
        case AbstractMessage.RC_SUCCESS:
        case AbstractMessage.RC_AUTHENTICATION_NEEDED:
        case AbstractMessage.RC_SERVICE_REFERRAL:
        case AbstractMessage.RC_PREFIX_REFERRAL:
            return new HandleException(HandleException.INTERNAL_ERROR, "Unexpected response: " + response.toString());
        case AbstractMessage.RC_PROTOCOL_ERROR:
            return new HandleException(HandleException.MESSAGE_FORMAT_ERROR, response.toString());
        case AbstractMessage.RC_HANDLE_NOT_FOUND:
            return new HandleException(HandleException.HANDLE_DOES_NOT_EXIST, response.toString());
        case AbstractMessage.RC_HANDLE_ALREADY_EXISTS:
            return new HandleException(HandleException.HANDLE_ALREADY_EXISTS, response.toString());
        case AbstractMessage.RC_INVALID_HANDLE:
        case AbstractMessage.RC_VALUES_NOT_FOUND:
        case AbstractMessage.RC_VALUE_ALREADY_EXISTS:
        case AbstractMessage.RC_INVALID_VALUE:
            return new HandleException(HandleException.INVALID_VALUE, response.toString());
        case AbstractMessage.RC_INVALID_ADMIN:
        case AbstractMessage.RC_INSUFFICIENT_PERMISSIONS:
        case AbstractMessage.RC_AUTHENTICATION_FAILED:
        case AbstractMessage.RC_INVALID_CREDENTIAL:
        case AbstractMessage.RC_AUTHEN_TIMEOUT:
        case AbstractMessage.RC_AUTHEN_ERROR:
            return new HandleException(HandleException.UNABLE_TO_AUTHENTICATE, response.toString());
        case AbstractMessage.RC_SESSION_TIMEOUT:
            return new HandleException(HandleException.SESSION_TIMEOUT, response.toString());
        case AbstractMessage.RC_SESSION_FAILED:
        case AbstractMessage.RC_INVALID_SESSION_KEY:
        case AbstractMessage.RC_NEED_RSAKEY_FOR_SESSIONEXCHANGE:
        case AbstractMessage.RC_INVALID_SESSIONSETUP_REQUEST:
            return new HandleException(HandleException.SERVER_CANNOT_PROCESS_SESSION, response.toString());
        case AbstractMessage.RC_SESSION_MESSAGE_REJECTED:
            return new HandleException(HandleException.DUPLICATE_SESSION_COUNTER, response.toString());
        default:
            return new HandleException(HandleException.SERVER_ERROR, response.toString());
        }
    }

}
