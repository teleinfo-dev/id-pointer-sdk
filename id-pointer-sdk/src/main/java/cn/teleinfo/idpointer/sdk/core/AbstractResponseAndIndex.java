/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class AbstractResponseAndIndex {
    final AbstractIdResponse response;
    final int index;

    public AbstractResponseAndIndex(int index, AbstractIdResponse response) {
        this.response = response;
        this.index = index;
    }

    public AbstractIdResponse getResponse() {
        return response;
    }

    public int getIndex() {
        return index;
    }
}
