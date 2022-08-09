package cn.teleinfo.idpointer.sdk.core;

public class LoginIDSystemRequest extends AbstractRequest {
	public int requestedIndexes = -1;
	public final boolean isAdminRequest = false;

	public LoginIDSystemRequest(byte[] identifier, int index, AuthenticationInfo authInfo) {
		super(identifier, AbstractMessage.OC_LOGIN_ID_SYSTEM, authInfo);
		this.requestedIndexes = index;
		this.authInfo = authInfo;
	}

	public String toString() {
		return super.toString() + " index:" + requestedIndexes;
	}
}
