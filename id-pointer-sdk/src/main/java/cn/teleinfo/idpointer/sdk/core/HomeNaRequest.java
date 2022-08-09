/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***************************************************************************
 * Request used to home prefix on a given handle server.
 * When sending this request, clients should be prepared to
 * authenticate as an administrator.
 ***************************************************************************/

public class HomeNaRequest extends AbstractRequest {

    public HomeNaRequest(byte[] na, AuthenticationInfo authInfo) {
        super(na, OC_HOME_NA, authInfo);
        this.isAdminRequest = true;
    }
}
