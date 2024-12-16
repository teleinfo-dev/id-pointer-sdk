/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/** Generic response without any fields or parameters
 */

public class GenericIdResponse extends AbstractIdResponse {

    public GenericIdResponse() {
        super();
    }

    public GenericIdResponse(int opCode, int responseCode) {
        super(opCode, responseCode);
    }

    public GenericIdResponse(AbstractIdRequest req, int responseCode) throws HandleException {
        super(req, responseCode);
    }

}
