/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/** Response to VerifyAuthRequest.  Indicates whether or not the
 *  authentication presented in the VerifyAuthRequest message is
 *  valid.
 */

public class VerifyAuthResponse extends AbstractResponse {

    public boolean isValid = false;

    public VerifyAuthResponse(VerifyAuthRequest req, boolean isValid) throws HandleException {
        super(req, RC_SUCCESS);
        this.isValid = isValid;
    }

    public VerifyAuthResponse(boolean isValid) {
        super(OC_VERIFY_CHALLENGE, RC_SUCCESS);
        this.isValid = isValid;
    }
}
