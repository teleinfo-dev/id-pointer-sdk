/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/******************************************************************************
 * Request used to remove a value from an existing handle.  Holds the handle
 * and the index of the value to be deleted.
 ******************************************************************************/

public class RemoveValueRequest extends AbstractRequest {

    public int indexes[];

    public RemoveValueRequest(byte handle[], int index, AuthenticationInfo authInfo) {
        this(handle, new int[] { index }, authInfo);
    }

    public RemoveValueRequest(byte handle[], int indexes[], AuthenticationInfo authInfo) {
        super(handle, AbstractMessage.OC_REMOVE_VALUE, authInfo);
        this.indexes = indexes;
        this.isAdminRequest = true;
    }

}
