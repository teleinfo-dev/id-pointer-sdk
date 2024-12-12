/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/******************************************************************************
 * Request used to add a value to an existing handle.  Holds the handle
 * and the value to be added.
 ******************************************************************************/

public class AddValueIdRequest extends AbstractIdRequest {

    public HandleValue values[];

    public AddValueIdRequest(byte handle[], HandleValue value, AuthenticationInfo authInfo) {
        this(handle, new HandleValue[] { value }, authInfo);
    }

    public AddValueIdRequest(byte handle[], HandleValue values[], AuthenticationInfo authInfo) {
        super(handle, AbstractMessage.OC_ADD_VALUE, authInfo);
        this.values = values;
        this.isAdminRequest = true;
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
