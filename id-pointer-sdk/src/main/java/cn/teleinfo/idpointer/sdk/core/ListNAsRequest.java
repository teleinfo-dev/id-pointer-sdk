/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***************************************************************************
 * Request used to retrieve a list of prefixes homed on a
 * server.  When sending this request, clients should be prepared to
 * authenticate as an administrator.
 *
 * The corresponding response - ListNAsResponse - is usually sent
 * using continuation messages, so clients should probably provide a
 * callback to the HandleResolver object when sending messages of this
 * type.
 *
 * For ListNAsRequest the 'handle' member will be blank.
 ***************************************************************************/

public class ListNAsRequest extends AbstractRequest {

    public ListNAsRequest(byte naHandle[], AuthenticationInfo authInfo) {
        super(naHandle, OC_LIST_HOMED_NAS, authInfo);
        this.requiresConnection = true;
    }

}
