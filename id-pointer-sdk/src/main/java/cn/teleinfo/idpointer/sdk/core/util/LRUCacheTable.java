/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.util;

import java.util.*;

/*
 * Table that never exceeds a maximum number of elements.  Uses a Least
 * Recently Used(LRU) replacement strategy.
 *
 * Adding, removing, getting, and contains key all have an additional
 * overhead of O(lg(n))
 */
public class LRUCacheTable<K, V> extends AbstractMap<K, V> {
    private int maxsize;
    private Map<K, V> map;

    public LRUCacheTable(int maxsize) {
        this.maxsize = maxsize;
        map = Collections.synchronizedMap(new LinkedHashMap<K, V>(maxsize, 1.0F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return super.size() >= LRUCacheTable.this.maxsize;
            }
        });
    }

    @Override
    public int size() {
        return map.size();
    }

    public int getMaxSize() {
        return maxsize;
    }

    public void setMaxSize(int newsize) {
        maxsize = newsize;
    }

    @Override
    public V put(K key, V val) {
        return map.put(key, val);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }
}
