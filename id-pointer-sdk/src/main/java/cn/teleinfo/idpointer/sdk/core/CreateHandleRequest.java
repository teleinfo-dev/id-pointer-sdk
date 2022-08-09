/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/******************************************************************************
 * Request used to create a new handle.  Holds the handle and the
 * initial values.
 ******************************************************************************/

public class CreateHandleRequest extends AbstractRequest {
    public HandleValue[] values;

    public CreateHandleRequest(byte[] handle, HandleValue[] values, AuthenticationInfo authInfo) {
        super(handle, AbstractMessage.OC_CREATE_HANDLE, authInfo);
        this.values = values;
        this.isAdminRequest = true;
    }

    public CreateHandleRequest(byte[] prefix, HandleValue[] values, AuthenticationInfo authInfo, boolean mintNewSuffix) {
        super(prefix, AbstractMessage.OC_CREATE_HANDLE, authInfo);
        this.values = values;
        this.isAdminRequest = true;
        this.mintNewSuffix = mintNewSuffix;
    }

    @Override
    public boolean shouldEncrypt() {
        if (!hasEqualOrGreaterVersion(2, 8)) return false;
        if (values == null) return false;
        for (HandleValue value : values) {
            if (!value.publicRead) return true;
        }
        return false;
    }
}
