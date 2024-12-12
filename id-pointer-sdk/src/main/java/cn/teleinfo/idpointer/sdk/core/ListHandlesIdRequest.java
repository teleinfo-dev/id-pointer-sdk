/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/***************************************************************************
 * Request used to retrieve a list of handles from a given prefix
 * from a server.  When sending this request, clients should be prepared to
 * authenticate as an administrator with list-handles permission in the
 * prefix handle.  Clients should also send a ListHandlesRequest
 * to every server in a site in order to get all of the handles for a
 * particular prefix.
 *
 * The corresponding response - ListHandlesResponse - is usually sent
 * using continuation messages, so clients should probably provide a
 * callback to the HandleResolver object when sending messages of this
 * type.
 *
 * For ListHandlesRequests the 'handle' member will contain the handle
 * for the prefix that we want the handles for.
 ***************************************************************************/

public class ListHandlesIdRequest extends AbstractIdRequest {

    public ListHandlesIdRequest(byte naHandle[], AuthenticationInfo authInfo) {
        super(naHandle, OC_LIST_HANDLES, authInfo);
        this.requiresConnection = true;
    }

}
