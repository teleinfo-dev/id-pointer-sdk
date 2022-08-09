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

public class GenericResponse extends AbstractResponse {

    public GenericResponse() {
        super();
    }

    public GenericResponse(int opCode, int responseCode) {
        super(opCode, responseCode);
    }

    public GenericResponse(AbstractRequest req, int responseCode) throws HandleException {
        super(req, responseCode);
    }

}
