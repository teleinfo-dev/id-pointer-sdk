/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/**
 * @deprecated can just use HandleStorage with default methods
 */
@Deprecated
public interface HandleStorage2 extends HandleStorage {
    @Override
    default boolean supportsDumpResumption() {
        return true;
    }

    /*********************************************************************
     * Scan the database, calling a method in the specified callback for
     * every handle in the database.
     *********************************************************************/
    @Override
    public void scanHandlesFrom(byte[] startingPoint, boolean inclusive, ScanCallback callback) throws HandleException;

    /*********************************************************************
     * Scan the homed prefix database, calling a method in the
     * specified callback for every prefix in the database.
     *********************************************************************/
    @Override
    public void scanNAsFrom(byte[] startingPoint, boolean inclusive, ScanCallback callback) throws HandleException;
}
