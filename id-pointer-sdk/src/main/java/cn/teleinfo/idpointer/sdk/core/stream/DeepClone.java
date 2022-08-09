/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
         http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.stream;

/** Interface implemented by objects that can clone themselves as well as
 * all of the objects contained within them (as long as they also implement
 * the DeepClone interface).
 */
public interface DeepClone {

    public Object deepClone();

}
