/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.stream.StreamTable;
import cn.teleinfo.idpointer.sdk.core.stream.StreamVector;

import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicationStateInfo {
    public static final String LAST_TXN_ID = "last_txn_id";
    public static final String LAST_TIMESTAMP = "last_timestamp";

    Map<String, ReplicationServerState> replicationServerStates;
    String ownName;

    public ReplicationStateInfo() {
        replicationServerStates = new ConcurrentHashMap<>();
    }

    public Set<String> keySet() {
        return replicationServerStates.keySet();
    }

    private ReplicationServerState getServerState(String name) {
        return replicationServerStates.get(name);
    }

    public long getLastTxnId(String name) {
        ReplicationServerState serverState = getServerState(name);
        if (serverState == null) return 0;
        else return serverState.lastTxnId;
    }

    public long getLastTimestamp(String name) {
        ReplicationServerState serverState = getServerState(name);
        if (serverState == null) return 0;
        else return serverState.lastTimestamp;
    }

    public void setLastTxnId(String name, long lastTxnId) {
        ReplicationServerState serverState = getServerState(name);
        if (serverState == null) replicationServerStates.put(name, new ReplicationServerState(lastTxnId, -1));
        else serverState.lastTxnId = lastTxnId;
    }

    public void setLastTimestamp(String name, long lastTimestamp) {
        ReplicationServerState serverState = getServerState(name);
        if (serverState == null) replicationServerStates.put(name, new ReplicationServerState(-1, lastTimestamp));
        else serverState.lastTimestamp = lastTimestamp;
    }

    public String getOwnName() {
        return ownName;
    }

    private static class ReplicationServerState {
        volatile long lastTxnId;
        volatile long lastTimestamp;

        ReplicationServerState(long lastTxnId, long lastTimestamp) {
            this.lastTxnId = lastTxnId;
            this.lastTimestamp = lastTimestamp;
        }
    }

    public static ReplicationStateInfo fromStreamTable(StreamTable replicationConfig, String ownName) {
        ReplicationStateInfo result = new ReplicationStateInfo();
        result.ownName = ownName;
        result.replicationServerStates = new ConcurrentHashMap<>();
        for (Enumeration<String> e = replicationConfig.keys(); e.hasMoreElements();) {
            String name = e.nextElement();
            if (name.equals(result.ownName)) continue;
            StreamVector serverStates = (StreamVector) replicationConfig.get(name);
            for (int i = 0; i < serverStates.size(); i++) {
                StreamTable serverState = (StreamTable) serverStates.get(i);
                String serverName = i + ":" + name;
                long lastTxnId = serverState.getLong(LAST_TXN_ID, -1);
                long lastTimestamp = serverState.getLong(LAST_TIMESTAMP, -1);
                result.replicationServerStates.put(serverName, new ReplicationServerState(lastTxnId, lastTimestamp));
            }
        }
        return result;
    }

    public static StreamTable toStreamTable(ReplicationStateInfo replicationStateInfo) {
        StreamTable result = new StreamTable();
        for (Map.Entry<String, ReplicationServerState> entry : replicationStateInfo.replicationServerStates.entrySet()) {
            String name = entry.getKey();
            int colon = name.indexOf(":");
            if (colon < 0) throw new AssertionError("Unexpected replication state name " + name);
            int serverNumber;
            try {
                serverNumber = Integer.parseInt(name.substring(0, colon));
            } catch (NumberFormatException e) {
                throw new AssertionError("Unexpected replication state name " + name);
            }
            String siteName = name.substring(colon + 1);

            StreamVector serverStates = (StreamVector) result.get(siteName);
            if (serverStates == null) {
                serverStates = new StreamVector();
                result.put(siteName, serverStates);
            }

            while (serverStates.size() <= serverNumber) {
                serverStates.add(new StreamTable());
            }
            StreamTable thisServerState = (StreamTable) serverStates.get(serverNumber);
            ReplicationServerState serverState = entry.getValue();
            thisServerState.put(LAST_TXN_ID, serverState.lastTxnId);
            thisServerState.put(LAST_TIMESTAMP, serverState.lastTimestamp);
        }
        return result;
    }

    public void setOwnName(String name) {
        ownName = name;
    }

    public boolean isQueueNameInOwnSite(String queueName) {
        return isQueueNameInSiteNamed(queueName, ownName);
    }

    public static boolean isQueueNameInSiteNamed(String queueName, String ownName) {
        int colon = queueName.indexOf(":");
        if (colon < 0) return false;
        return queueName.substring(colon + 1).equals(ownName);
    }
}
