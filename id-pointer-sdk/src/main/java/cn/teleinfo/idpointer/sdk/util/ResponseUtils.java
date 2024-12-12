package cn.teleinfo.idpointer.sdk.util;

import cn.teleinfo.idpointer.sdk.client.v3.IdResponse;
import cn.teleinfo.idpointer.sdk.core.AbstractMessage;
import cn.teleinfo.idpointer.sdk.exception.IDException;

public abstract class ResponseUtils {

    public static void checkResponseCode(IdResponse response) throws IDException {
        if (response.getResponseCode() != AbstractMessage.RC_SUCCESS && response.getResponseCode() != AbstractMessage.RC_AUTHENTICATION_NEEDED) {
            throw new IDException(response.getResponseCode(), "Response code is not success", response);
        }
    }

}
