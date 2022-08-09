/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.core.stream.StreamTable;

import java.util.Enumeration;

/***********************************************************************
 * HandleStorage is an interface to an object that is capable
 * of storing handles and their values.
 ***********************************************************************/

public interface HandleStorage {

    /*********************************************************************
     * Initializes the handle storage object with the given configuration
     * table.
     *********************************************************************/
    public void init(StreamTable configTable) throws Exception;

    /*********************************************************************
     * Returns true if this server is responsible for the given prefix.
     *********************************************************************/
    public boolean haveNA(byte authHandle[]) throws HandleException;

    /*********************************************************************
     * Sets a flag indicating whether or not this server is responsible
     * for the given prefix.
     *********************************************************************/
    public void setHaveNA(byte authHandle[], boolean flag) throws HandleException;

    /*********************************************************************
     * Creates the specified handle in the "database" with the specified
     * initial values
     *********************************************************************/
    public void createHandle(byte handle[], HandleValue values[]) throws HandleException;

    /*********************************************************************
     * Delete the specified handle in the database.  Returns true if
     * the given handle was in the database.
     *********************************************************************/
    public boolean deleteHandle(byte handle[]) throws HandleException;

    /*********************************************************************
     * Return the pre-packaged values of the given handle that are either
     * in the indexList or the typeList.  This method should return any
     * values of type ALIAS or REDIRECT, even if they were not requested.
     * Return null to indicate handle not found; byte[0][] to indicate
     * values not found.
     *********************************************************************/
    public byte[][] getRawHandleValues(byte handle[], int indexList[], byte typeList[][]) throws HandleException;

    /*********************************************************************
     * Replace the handle value that has the same index as the given handle
     * value with the given handle value.
     *********************************************************************/
    public void updateValue(byte handle[], HandleValue value[]) throws HandleException;

    /*********************************************************************
     * Scan the database, calling a method in the specified callback for
     * every handle in the database.
     *********************************************************************/
    public void scanHandles(ScanCallback callback) throws HandleException;

    /*********************************************************************
     * Scan the homed prefix database, calling a method in the
     * specified callback for every prefix in the database.
     *********************************************************************/
    public void scanNAs(ScanCallback callback) throws HandleException;

    /*********************************************************************
     * Scan the database for handles with the given prefix
     * and return an Enumeration of byte arrays with each byte array
     * being a handle.  <i>naHdl</i> is the prefix handle
     * for the prefix that you want to list the handles for.
     *********************************************************************/
    public Enumeration<byte[]> getHandlesForNA(byte naHdl[]) throws HandleException;

    /*********************************************************************
     * Remove all of the records from the database.
     ********************************************************************/
    public void deleteAllRecords() throws HandleException;

    /*********************************************************************
     * Checkpoint (ie backup, and reset transaction logs) the database.
     * If not supported, or if the operation failed this should throw an
     * exception.  This may just be an asynchronous call that *starts* the
     * checkpoint process, so the method may return if the checkpoint was
     * started, not necessarily if it was successful.
     ********************************************************************/
    public void checkpointDatabase() throws HandleException;

    /*********************************************************************
     * Save pending data and close any open files.
     *********************************************************************/
    public void shutdown();

    /**
     * Returns true if {@link #scanHandlesFrom(byte[], boolean, ScanCallback)} and
     * {@link #scanNAsFrom(byte[], boolean, ScanCallback)} are implemented;
     * otherwise false.
     */
    default boolean supportsDumpResumption() {
        return false;
    }

    /*********************************************************************
     * Scan the database, calling a method in the specified callback for
     * every handle in the database.
     *********************************************************************/
    @SuppressWarnings("unused")
    default void scanHandlesFrom(byte[] startingPoint, boolean inclusive, ScanCallback callback) throws HandleException {
        throw new UnsupportedOperationException();
    }

    /*********************************************************************
     * Scan the homed prefix database, calling a method in the
     * specified callback for every prefix in the database.
     *********************************************************************/
    @SuppressWarnings("unused")
    default void scanNAsFrom(byte[] startingPoint, boolean inclusive, ScanCallback callback) throws HandleException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if the given handle exists in the database.
     */
    default boolean exists(byte[] handle) throws HandleException {
        return getRawHandleValues(handle, null, null) != null;
    }

    /**
     * Creates or updates the handle to have the exact given handle values.
     */
    default void createOrUpdateRecord(byte[] handle, HandleValue[] values) throws HandleException {
        if (exists(handle)) {
            updateValue(handle, values);
        } else {
            createHandle(handle, values);
        }
    }
}
