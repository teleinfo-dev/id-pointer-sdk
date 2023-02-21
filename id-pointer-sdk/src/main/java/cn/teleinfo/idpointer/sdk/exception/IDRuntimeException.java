package cn.teleinfo.idpointer.sdk.exception;

import cn.teleinfo.idpointer.sdk.core.AbstractResponse;

public class IDRuntimeException extends RuntimeException{
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
    /**
     * 连接出错,超时,服务器未启动
     */
    public static final int CHANNEL_GET_ERROR = 1000;
    /**
     * 异步消息响应超时
     */
    public static final int PROMISE_GET_ERROR = 1001;

    /**
     * 客户端错误
     */
    public static final int CLIENT_ERROR = 1002;
    /**
     * 响应的handle value不合法
     */
    public static final int RC_INVALID_VALUE = 2001;
    /**
     * 不是合法的响应,例如解析请求响应的不是解析信息
     */
    //public static final int RC_ILLEGAL_RESPONSE = 2002;
    /**
     * 响应的返回响不是成功,RC_SUCCESS不是1,客户端改成3 + 3位RC_CODE
     */
    //public static final int RC_INVALID_RESPONSE_CODE = 3000;

    private final int code;
    private AbstractResponse response;

    public IDRuntimeException(int code) {
        this.code = code;
    }

    public IDRuntimeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public IDRuntimeException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public IDRuntimeException(Throwable cause, int code) {
        super(cause);
        this.code = code;
    }

    public IDRuntimeException(String message, AbstractResponse response) {
        super(message);
        this.code = getResponseCode(response);
        this.response = response;
    }

    public IDRuntimeException(String message, Throwable cause, AbstractResponse response) {
        super(message, cause);
        this.code = getResponseCode(response);
        this.response = response;
    }

    public IDRuntimeException(Throwable cause, AbstractResponse response) {
        super(cause);
        this.code = getResponseCode(response);
        this.response = response;
    }

    private int getResponseCode(AbstractResponse response) {
        return 3000 + response.responseCode;
    }

    public AbstractResponse getResponse() {
        return response;
    }

    public int getCode() {
        return code;
    }
}
