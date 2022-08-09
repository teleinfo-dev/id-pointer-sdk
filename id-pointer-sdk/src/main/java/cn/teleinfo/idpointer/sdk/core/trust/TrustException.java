/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;


import cn.teleinfo.idpointer.sdk.core.HandleException;

public class TrustException extends HandleException {

    public TrustException(String message) {
        super(HandleException.SECURITY_ALERT, message);
    }

    public TrustException(String message, Throwable cause) {
        super(HandleException.SECURITY_ALERT, message, cause);
    }

}
