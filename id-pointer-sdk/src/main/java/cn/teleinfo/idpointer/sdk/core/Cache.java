/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/*************************************************************
 *
 *************************************************************/

public interface Cache {

    /** Returns any non-expired handle values that are in the caches
     *  storage.  A null return value indicates that the requested values
     *  aren't in the cache.  Returning the an array of values (including
     *  an array of zero length) indicates that the returned values are
     *  the only values from the requested set (ie the handle doesn't have
     *  any more values from the requested set).
     *
     *  Returns a sentinel value if HANDLE_NOT_FOUND has been cached; return
     *  value should be checked against isCachedNotFound before using.
     *
     *  ***** Speed is important in this method *****
     */
    public byte[][] getCachedValues(byte handle[], byte reqTypes[][], int reqIndexes[]) throws Exception;

    /** Returns true if this return value of getCachedValues indicates a
     *  cached value of HANDLE_NOT_FOUND
     */
    public boolean isCachedNotFound(byte[][] values);

    /** Store the given handle values after a query for the handle.  The
     *  query was performed with the given type-list and index-list.
     *
     * ***** Speed is less important in this method *****
     *
     */
    public void setCachedValues(byte handle[], HandleValue newValues[], byte newTypeList[][], int newIndexList[]) throws Exception;

    /** Returns true if this handle should have a cached
     *  HANDLE_NOT_FOUND.  Pass time-to-live.
     */
    public void setCachedNotFound(byte[] handle, int ttl) throws Exception;

    /** Set the maximum size for the cache by the number of handles.
     */
    public void setMaximumHandles(int maxHandles);

    /** Set the maximum size for the cache by the number of bytes
     *  used for storage.
     */
    public void setMaximumSize(int maxSize);

    /** Remove one handle from the cache */
    public void removeHandle(byte[] handle) throws Exception;

    /** Remove all values from the cache */
    public void clear() throws Exception;

    public void close() throws Exception;

}
