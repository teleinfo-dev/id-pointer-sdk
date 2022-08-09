/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.util.LRUCacheTable;

/*************************************************************
 * Class used to cache values in a local database file.
 *************************************************************/

public class MemCache implements Cache {
    private final LRUCacheTable<String, byte[]> db;
    // by default don't let TTLs last longer than 60 minutes.  should provide
    // decent performance, while still detecting updates in things like
    // site_infos.
    private long maxTTL = 60 * 60;

    public MemCache(int maxHandles, long maxTTL) {
        this.maxTTL = maxTTL;
        db = new LRUCacheTable<>(maxHandles);
    }

    @Deprecated
    public MemCache(int maxHandles, long maxTTL, @SuppressWarnings("unused") boolean trackHits) {
        this(maxHandles, maxTTL);
    }

    public MemCache(long maxTTL) {
        this(100, maxTTL);
    }

    public MemCache() {
        this(100, 60 * 60);
    }

    @Override
    public boolean isCachedNotFound(byte[][] values) {
        // use something that looks like a Handle Value with an index of -1
        return values == cachedNotFoundResult; //physical equality
        // return values.length==1 && values[0].length >= Encoder.INT_SIZE && Encoder.readInt(values[0],0) < 0;
    }

    private static final byte[][] cachedNotFoundResult = new byte[1][Encoder.INT_SIZE];
    static {
        Encoder.writeInt(cachedNotFoundResult[0], 0, -1);
    }

    // a NOT_FOUND is cached as -1 timeRetrieved ttl
    private boolean isCachedNotFoundClumps(byte[] clumps) {
        return Encoder.readInt(clumps, 0) < 0;
    }

    private int notFoundTimeRetrieved(byte[] notfound) {
        return Encoder.readInt(notfound, Encoder.INT_SIZE);
    }

    private int notFoundTTL(byte[] notfound) {
        return Encoder.readInt(notfound, 2 * Encoder.INT_SIZE);
    }

    /** Returns any non-expired handle values that are in the caches
     *  storage.  A null return value indicates that the requested values
     *  aren't in the cache.  Returning the an array of values (including
     *  an array of zero length) indicates that the returned values are
     *  the only values from the requested set (ie the handle doesn't have
     *  any more values from the requested set).
     *
     *  ***** Speed is important in this method *****
     */
    @Override
    public byte[][] getCachedValues(byte handle[], byte reqTypes[][], int reqIndexes[]) throws Exception {
        byte clumps[];
        try {
            String key = Util.decodeString(handle);
            clumps = db.get(key);
        } catch (Exception e) {
            System.err.println("cache error: " + e);
            e.printStackTrace(System.err);
            return null;
        }
        if (clumps == null) {
            return null;
        }

        int now = (int) (System.currentTimeMillis() / 1000);

        if (isCachedNotFoundClumps(clumps)) {
            if (now - notFoundTimeRetrieved(clumps) > Math.min(maxTTL, notFoundTTL(clumps))) {
                // value is stale
                return null;
            }
            return cachedNotFoundResult;
        }

        int idx = 0;

        boolean allValues = (reqIndexes == null || reqIndexes.length <= 0) && (reqTypes == null || reqTypes.length <= 0);

        // records should have this layout:
        //  typeArrayLen typeArray indexArrayLen indexArray numClumps ( length time_retrieved clump )+
        byte types[][] = new byte[Encoder.readInt(clumps, idx)][];
        idx += Encoder.INT_SIZE;
        idx += Encoder.readByteArrayArray(types, clumps, idx);

        int indexes[] = Encoder.readIntArray(clumps, idx);
        idx += Encoder.INT_SIZE + Encoder.INT_SIZE * indexes.length;

        // int numClumps = Encoder.readInt(clumps, idx);
        idx += Encoder.INT_SIZE;

        // if we don't have the requested types, return null
        if (!(types.length == 0 && indexes.length == 0)) {
            // the cache DB only has specific values (not necessarily all values) for the handle

            if (allValues) {
                return null; // they were asking for all values, which we don't have
            }

            if (reqIndexes != null && reqIndexes.length > 0) {
                // the user is requesting specific indexes, see if we have them...
                for (int i = 0; i < reqIndexes.length; i++) {
                    if (!Util.isInArray(indexes, reqIndexes[i])) {
                        return null; // one of the requested indexes wasn't cached
                    }
                }
            }
            if (reqTypes != null && reqTypes.length > 0) {
                // the user is requesting specific types, see if we have them...
                for (int i = 0; i < reqTypes.length; i++) {
                    if (!Util.isParentTypeInArray(types, reqTypes[i]) && !Util.isInArray(types, reqTypes[i])) {
                        return null; // one of the requested types wasn't cached
                    }
                }
            }
        }

        // at this point, we know that we have the requested values cached so
        // we just need to filter them out, check for timeouts and return them

        int clumpLen;
        byte clumpType[];
        int clumpIndex;
        int startIdx = idx;
        int numMatches = 0;
        boolean gotClumps = false;

        // count the number of matching records
        while (idx < clumps.length) {
            clumpLen = Encoder.readInt(clumps, idx);
            idx += Encoder.INT_SIZE;
            idx += Encoder.INT_SIZE; // skip the time-retrieved field

            clumpType = Encoder.getHandleValueType(clumps, idx);
            clumpIndex = Encoder.getHandleValueIndex(clumps, idx);

            if (allValues || Util.isParentTypeInArray(reqTypes, clumpType) || Util.isInArray(reqIndexes, clumpIndex)) numMatches++;
            gotClumps = true;
            idx += clumpLen;
        }

        // if we didn't find any of the requested records, return null
        // not empty set - because the empty set would never time-out
        if (!gotClumps || numMatches == 0) {
            return null;
        }

        // put the matching records into an array
        byte retValues[][] = new byte[numMatches][];
        int clumpNum = 0;
        idx = startIdx;
        int valueDate;
        HandleValue testValue = null;
        while (idx < clumps.length) {
            clumpLen = Encoder.readInt(clumps, idx);
            idx += Encoder.INT_SIZE;
            valueDate = Encoder.readInt(clumps, idx);
            idx += Encoder.INT_SIZE;

            clumpType = Encoder.getHandleValueType(clumps, idx);
            clumpIndex = Encoder.getHandleValueIndex(clumps, idx);

            if (allValues || Util.isParentTypeInArray(reqTypes, clumpType) || Util.isInArray(reqIndexes, clumpIndex)) {
                // check to see if the value is timed out... if so, return nothing.

                retValues[clumpNum] = new byte[clumpLen];
                if (testValue == null) testValue = new HandleValue();
                Encoder.decodeHandleValue(clumps, idx, testValue);

                if (testValue.isExpired(now, valueDate)) {
                    // value is stale, need to re-retrieve all values for this query
                    return null;
                } else if ((now - valueDate) > maxTTL) {
                    // not explicitly expired, but exceeds our max TTL
                    return null;
                }
                System.arraycopy(clumps, idx, retValues[clumpNum], 0, retValues[clumpNum].length);
                clumpNum++;
            }

            idx += clumpLen;
        }

        if (clumpNum != retValues.length) {
            // we missed something along the way - failsafe!!
            System.err.println("Unknown cache error!!!");
            Thread.dumpStack();
            return null;
        }
        return retValues;
    }

    @Override
    public void setCachedNotFound(byte handle[], int ttl) throws Exception {
        String key = Util.decodeString(handle);
        int now = (int) (System.currentTimeMillis() / 1000);
        byte dataBuf[] = new byte[3 * Encoder.INT_SIZE];
        Encoder.writeInt(dataBuf, 0, -1);
        Encoder.writeInt(dataBuf, Encoder.INT_SIZE, now);
        Encoder.writeInt(dataBuf, 2 * Encoder.INT_SIZE, ttl);
        db.put(key, dataBuf);
    }

    @Override
    public void removeHandle(byte[] handle) throws Exception {
        db.remove(Util.decodeString(handle));
    }

    /** Store the given handle values after a query for the handle.  The
     *  query was performed with the given type-list and index-list.
     *
     * ***** Speed is less important in this method *****
     */
    @Override
    public void setCachedValues(byte handle[], HandleValue newValues[], byte newTypeList[][], int newIndexList[]) throws Exception {
        if (newValues != null && (newTypeList != null && newTypeList.length > 0)) {
            // Caching a restricted query where there are types.
            // Make sure that all the indices we actually got are in the index list.
            // That way we can find them later by index.
            if (newIndexList == null) newIndexList = new int[0];
            int[] expIndexList = new int[newIndexList.length + newValues.length];
            System.arraycopy(newIndexList, 0, expIndexList, 0, newIndexList.length);
            for (int i = 0; i < newValues.length; i++) {
                expIndexList[newIndexList.length + i] = newValues[i].index;
            }
            java.util.Arrays.sort(expIndexList);
            int uniq = 0;
            for (int i = 0; i < expIndexList.length; i++) {
                if (i == 0 || expIndexList[i] != expIndexList[i - 1]) {
                    uniq++;
                }
            }
            newIndexList = new int[uniq];
            int count = 0;
            for (int i = 0; count < uniq; i++) {
                if (i == 0 || expIndexList[i] != expIndexList[i - 1]) {
                    newIndexList[count++] = expIndexList[i];
                }
            }
        }

        byte types[][] = null;
        int indexes[] = null;
        int valueDates[] = null;
        HandleValue values[] = null;

        byte clumps[] = null;

        String key = Util.decodeString(handle);

        try {
            clumps = db.get(key);
        } catch (Exception e) {
            System.err.println("MemCache error: " + e);
            e.printStackTrace(System.err);
            return;
        }

        int idx = 0;

        if (clumps != null && !isCachedNotFoundClumps(clumps)) {
            types = new byte[Encoder.readInt(clumps, idx)][];
            idx += Encoder.INT_SIZE;
            idx += Encoder.readByteArrayArray(types, clumps, idx);

            indexes = Encoder.readIntArray(clumps, idx);
            idx += Encoder.INT_SIZE + Encoder.INT_SIZE * indexes.length;

            values = new HandleValue[Encoder.readInt(clumps, idx)];
            idx += Encoder.INT_SIZE;

            valueDates = new int[values.length];

            int i = 0;
            while (idx < clumps.length) {
                int clumpLen = Encoder.readInt(clumps, idx);
                idx += Encoder.INT_SIZE;
                valueDates[i] = Encoder.readInt(clumps, idx);
                idx += Encoder.INT_SIZE;

                values[i] = new HandleValue();
                Encoder.decodeHandleValue(clumps, idx, values[i]);
                i++;
                idx += clumpLen;
            }
        }

        int now = (int) (System.currentTimeMillis() / 1000);

        // at this point the values that will go into the cache are the union of the
        // values in 'values' and 'newValues'

        if ((newTypeList == null || newTypeList.length <= 0) && (newIndexList == null || newIndexList.length <= 0)) {
            // replace all old values with new values...
            types = null;
            indexes = null;
            values = null;
        } else if ((types != null && types.length <= 0) && (indexes != null && indexes.length <= 0)) {
            // there was already a query for all values, we'll only update the new
            // values that were just retrieved
            for (int i = 0; values != null && newValues != null && i < newValues.length; i++) {
                int thisIndex = newValues[i].index;
                for (int j = 0; j < values.length; j++) {
                    if (values[j] != null && values[j].index == thisIndex) {
                        values[j] = null;
                    }
                }
            }
            // remove currently cached value for which the new resolution requested its type
            if (values != null && newTypeList != null && newTypeList.length > 0) {
                for (int j = 0; j < values.length; j++) {
                    if (values[j] != null && Util.isParentTypeInArray(newTypeList, values[j].type)) {
                        values[j] = null;
                    }
                }
            }
            newTypeList = null;
            newIndexList = null;

        } else {
            // There were already some values in the cache, and we got some more.
            // merge the new fresh values with the old ones.

            // remove currently cached value for which the new resolution requested its type
            if (values != null && newTypeList != null && newTypeList.length > 0) {
                for (int j = 0; j < values.length; j++) {
                    if (values[j] != null && Util.isParentTypeInArray(newTypeList, values[j].type)) {
                        values[j] = null;
                    }
                }
            }

            if (newTypeList != null && newTypeList.length > 0) {
                byte typeListCopy[][] = new byte[newTypeList.length][];
                System.arraycopy(newTypeList, 0, typeListCopy, 0, newTypeList.length);
                newTypeList = typeListCopy;

                // remove duplicates in intersections of the old type query list and the new one
                for (int i = 0; types != null && i < types.length; i++) {
                    if (types[i] == null) continue;
                    for (int j = 0; newTypeList != null && j < newTypeList.length; j++) {
                        if (newTypeList[j] != null && Util.equalsCI(types[i], newTypeList[j])) {
                            newTypeList[j] = null;
                        }
                    }
                }
            }

            if (newIndexList != null && newIndexList.length > 0) {
                int indexListCopy[] = new int[newIndexList.length];
                System.arraycopy(newIndexList, 0, indexListCopy, 0, newIndexList.length);
                newIndexList = indexListCopy;

                // remove duplicates in intersections of the old index query list and the new one
                for (int i = 0; indexes != null && i < indexes.length; i++) {
                    if (indexes[i] < 0) continue;
                    for (int j = 0; newIndexList != null && j < newIndexList.length; j++) {
                        if (newIndexList[j] >= 0 && indexes[i] == newIndexList[j]) {
                            newIndexList[j] = -1;
                        }
                    }
                }
            }

            // remove values for which we have newer values from the old list of values
            for (int i = 0; values != null && newValues != null && i < newValues.length; i++) {
                int thisIndex = newValues[i].index;
                for (int j = 0; j < values.length; j++) {
                    if (values[j] != null && values[j].index == thisIndex) {
                        // get rid of old values
                        values[j] = null;
                    }
                }
            }
        }

        // copy the new values, index-queries, and type-queries into a new clump to
        // put back in the database.
        int dataLen = 0;
        int typeCount = 0;
        int indexCount = 0;
        int valueCount = 0;
        dataLen += Encoder.INT_SIZE; // type-list length
        dataLen += Encoder.INT_SIZE; // index-list length
        dataLen += Encoder.INT_SIZE; // value-list length

        // typeArrayLen typeArray indexArrayLen indexArray numClumps ( length time_retrieved clump )+
        for (int i = 0; types != null && i < types.length; i++) {
            if (types[i] != null) {
                dataLen += Encoder.INT_SIZE + types[i].length;
                typeCount++;
            }
        }
        for (int i = 0; newTypeList != null && i < newTypeList.length; i++) {
            if (newTypeList[i] != null) {
                dataLen += Encoder.INT_SIZE + newTypeList[i].length;
                typeCount++;
            }
        }

        for (int i = 0; indexes != null && i < indexes.length; i++) {
            if (indexes[i] >= 0) {
                dataLen += Encoder.INT_SIZE;
                indexCount++;
            }
        }
        for (int i = 0; newIndexList != null && i < newIndexList.length; i++) {
            if (newIndexList[i] >= 0) {
                dataLen += Encoder.INT_SIZE;
                indexCount++;
            }
        }
        for (int i = 0; values != null && i < values.length; i++) {
            if (values[i] != null) {
                dataLen += Encoder.INT_SIZE; // the time-retrieved field
                dataLen += Encoder.INT_SIZE; // the value-length field
                dataLen += Encoder.calcStorageSize(values[i]);
                valueCount++;
            }
        }
        for (int i = 0; newValues != null && i < newValues.length; i++) {
            if (newValues[i] != null) {
                dataLen += Encoder.INT_SIZE; // the time-retrieved field
                dataLen += Encoder.INT_SIZE; // the value-length field
                dataLen += Encoder.calcStorageSize(newValues[i]);
                valueCount++;
            }
        }

        byte dataBuf[] = new byte[dataLen];
        int loc = 0;
        loc += Encoder.writeInt(dataBuf, loc, typeCount);
        for (int i = 0; types != null && i < types.length; i++) {
            if (types[i] != null) {
                loc += Encoder.writeByteArray(dataBuf, loc, types[i]);
            }
        }
        for (int i = 0; newTypeList != null && i < newTypeList.length; i++) {
            if (newTypeList[i] != null) {
                loc += Encoder.writeByteArray(dataBuf, loc, newTypeList[i]);
            }
        }

        loc += Encoder.writeInt(dataBuf, loc, indexCount);
        for (int i = 0; indexes != null && i < indexes.length; i++) {
            if (indexes[i] >= 0) {
                loc += Encoder.writeInt(dataBuf, loc, indexes[i]);
            }
        }
        for (int i = 0; newIndexList != null && i < newIndexList.length; i++) {
            if (newIndexList[i] >= 0) {
                loc += Encoder.writeInt(dataBuf, loc, newIndexList[i]);
            }
        }

        loc += Encoder.writeInt(dataBuf, loc, valueCount);
        for (int i = 0; values != null && valueDates != null && i < values.length; i++) {
            if (values[i] != null) {
                int lenLoc = loc;
                loc += Encoder.INT_SIZE; // placeholder for the value-length field
                loc += Encoder.writeInt(dataBuf, loc, valueDates[i]);
                loc += Encoder.encodeHandleValue(dataBuf, loc, values[i]);
                Encoder.writeInt(dataBuf, lenLoc, loc - lenLoc - 2 * Encoder.INT_SIZE); // the value-length field
            }
        }

        for (int i = 0; newValues != null && i < newValues.length; i++) {
            if (newValues[i] != null) {
                int lenLoc = loc;
                loc += Encoder.INT_SIZE; // placeholder for the value-length field
                loc += Encoder.writeInt(dataBuf, loc, now);
                loc += Encoder.encodeHandleValue(dataBuf, loc, newValues[i]);
                Encoder.writeInt(dataBuf, lenLoc, loc - lenLoc - 2 * Encoder.INT_SIZE); // the value-length field
            }
        }

        db.put(key, dataBuf);
    }

    /** Remove all values from the cache */
    @Override
    public void clear() throws Exception {
        db.clear();
    }

    /** Set the maximum size for the cache by the number of handles. */
    @Override
    public void setMaximumHandles(int maxHandles) {
        db.setMaxSize(maxHandles);
    }

    /** Set the maximum size for the cache by the number of bytes
     *  used for storage.
     */
    @Override
    public void setMaximumSize(int maxSize) {
        // hmmm this is hard to do for a hashtable, lets just use a simple policy
        setMaximumHandles(maxSize / 1024);
    }

    @Override
    public void close() {
    }

}
