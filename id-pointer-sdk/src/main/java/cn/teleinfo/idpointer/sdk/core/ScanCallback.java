/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/*********************************************************************
 * Callback for objects that want to be able to scan all of the handles
 * in a HandleStorage instance.
 *********************************************************************/

public interface ScanCallback {
    /*********************************************************************
     * process the specified handle (sent in utf8 format)
     *********************************************************************/
    public void scanHandle(byte handle[]) throws HandleException;
}
