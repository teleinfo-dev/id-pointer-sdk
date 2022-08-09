/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***********************************************************************
 * Object used to represent an answer to a challenge to authenticate.
 ***********************************************************************/
public class ChallengeAnswerRequest extends AbstractRequest {
    public byte authType[];
    public byte userIdHandle[];
    public int userIdIndex;
    public byte signedResponse[];
    public AbstractRequest originalRequest; // only used by client

    public ChallengeAnswerRequest(byte authType[], byte userIdHandle[], int userIdIndex, byte signedResponse[], AuthenticationInfo authInfo) {
        super(Common.BLANK_HANDLE, AbstractMessage.OC_RESPONSE_TO_CHALLENGE, authInfo);
        this.authType = authType;
        this.userIdHandle = userIdHandle;
        this.userIdIndex = userIdIndex;
        this.signedResponse = signedResponse;
    }

    public ChallengeAnswerRequest(AbstractRequest req, ChallengeResponse challenge, AuthenticationInfo authInfo) throws HandleException {
        this(authInfo.getAuthType(), authInfo.getUserIdHandle(), authInfo.getUserIdIndex(), authInfo.authenticate(challenge, req), authInfo);
        takeValuesFrom(req);
        sessionId = challenge.sessionId;
        sessionInfo = req.sessionInfo;
    }

    @Override
    public String toString() {
        return super.toString() + ' ' + Util.decodeString(authType) + ' ' + userIdIndex + ':' + Util.decodeString(userIdHandle);
    }
}
