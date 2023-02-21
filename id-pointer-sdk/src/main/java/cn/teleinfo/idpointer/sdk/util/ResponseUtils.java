package cn.teleinfo.idpointer.sdk.util;

import cn.teleinfo.idpointer.sdk.core.AbstractMessage;
import cn.teleinfo.idpointer.sdk.core.AbstractResponse;
import cn.teleinfo.idpointer.sdk.exception.IDException;

public abstract class ResponseUtils {

    public static void checkResponse(AbstractResponse response) throws IDException {
        if (response.responseCode != AbstractMessage.RC_SUCCESS && response.responseCode != AbstractMessage.RC_AUTHENTICATION_NEEDED) {
            throw new IDException("IDException.RC_INVALID_RESPONSE_CODE", response);
        }
    }
}
