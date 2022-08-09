/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public interface RootInfoListener {

    /** This is called when it is determined that the root info
      for a particular configuration is out of date.  This lets
      an application update it's root information in whatever way
      is appropriate.  If no RootInfoListeners are defined for a
      configuration, then the ~/.handle/root_info file is updated
      with the results of a certified query for the 0.na/0.na handle. */
    public void rootInfoOutOfDate(Configuration configuration);

}
