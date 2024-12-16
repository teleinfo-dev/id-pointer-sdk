/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/** Request used to resolve a handle.  Holds the handle and parameters
 *  used in resolution.
 */

public class VerifyAuthIdRequest extends AbstractIdRequest {

    public int handleIndex;
    public byte nonce[];
    // public byte origDigestAlg; // unused.  VerifyAuthRequest is now encoded using an old format which allows variable length "origRequestDigest".
    public byte origRequestDigest[];
    public byte signedResponse[];

    public VerifyAuthIdRequest(byte handle[], byte nonce[], byte origRequestDigest[], @SuppressWarnings("unused") byte origDigestAlg, byte signedResponse[], int handleIndex, AuthenticationInfo authInfo) {
        super(handle, OC_VERIFY_CHALLENGE, authInfo);
        this.handleIndex = handleIndex;
        this.nonce = nonce;
        // this.origDigestAlg = origDigestAlg;
        this.origRequestDigest = origRequestDigest;
        this.signedResponse = signedResponse;
        this.authInfo = authInfo;
        this.certify = true;
        this.returnRequestDigest = true;
    }

}
