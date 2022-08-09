/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class AdminRecord {
    public byte adminId[];

    public int adminIdIndex;

    public static final int ADD_HANDLE = 0, DELETE_HANDLE = 1, ADD_DERIVED_PREFIX = 2, DELETE_DERIVED_PREFIX = 3, MODIFY_VALUE = 4, REMOVE_VALUE = 5, ADD_VALUE = 6, MODIFY_ADMIN = 7, REMOVE_ADMIN = 8, ADD_ADMIN = 9, READ_VALUE = 10,
        LIST_HANDLES = 11;

    @Deprecated
    public static final int ADD_NAMING_AUTH = 2, DELETE_NAMING_AUTH = 3;

    // Named Admin-permissions booleans
    // for use in constructor invocations

    public static final boolean PRM_ADD_HANDLE = true, PRM_NO_ADD_HANDLE = false,

        PRM_DELETE_HANDLE = true, PRM_NO_DELETE_HANDLE = false,

        PRM_ADD_NA = true, PRM_NO_ADD_NA = false,

        PRM_DELETE_NA = true, PRM_NO_DELETE_NA = false,

        PRM_READ_VALUE = true, PRM_NO_READ_VALUE = false,

        PRM_MODIFY_VALUE = true, PRM_NO_MODIFY_VALUE = false,

        PRM_REMOVE_VALUE = true, PRM_NO_REMOVE_VALUE = false,

        PRM_ADD_VALUE = true, PRM_NO_ADD_VALUE = false,

        PRM_MODIFY_ADMIN = true, PRM_NO_MODIFY_ADMIN = false,

        PRM_REMOVE_ADMIN = true, PRM_NO_REMOVE_ADMIN = false,

        PRM_ADD_ADMIN = true, PRM_NO_ADD_ADMIN = false,

        PRM_LIST_HANDLES = true, PRM_NO_LIST_HANDLES = false;

    public boolean perms[] = new boolean[12];

    /**
     * Version 7.3 and earlier had two extra zero bytes at the end of the representation
     */
    public boolean legacyByteLength = false;

    /******************************************************************************
     *
     * Null constructor
     *
     */

    public AdminRecord() {
    }

    /******************************************************************************
     *
     * Constructor
     *
     */

    public AdminRecord(byte adminId[], int adminIdIndex,

        boolean addHandle, boolean deleteHandle,

        boolean addNA, boolean deleteNA,

        boolean modifyValue, boolean removeValue, boolean addValue,

        boolean modifyAdmin, boolean removeAdmin, boolean addAdmin,

        boolean readValue,

        boolean listHandles) {
        this.adminId = adminId;
        this.adminIdIndex = adminIdIndex;

        perms[ADD_HANDLE] = addHandle;
        perms[DELETE_HANDLE] = deleteHandle;

        perms[ADD_DERIVED_PREFIX] = addNA;
        perms[DELETE_DERIVED_PREFIX] = deleteNA;

        perms[LIST_HANDLES] = listHandles;

        perms[READ_VALUE] = readValue;
        perms[MODIFY_VALUE] = modifyValue;
        perms[REMOVE_VALUE] = removeValue;
        perms[ADD_VALUE] = addValue;

        perms[MODIFY_ADMIN] = modifyAdmin;
        perms[REMOVE_ADMIN] = removeAdmin;
        perms[ADD_ADMIN] = addAdmin;
    }

    /******************************************************************************
     *
     * String representation of object (Admin's handle, index, and permissions)
     *
     */

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(20);

        sb.append("handle=");
        sb.append(Util.decodeString(adminId));

        sb.append("; index=");
        sb.append(adminIdIndex);

        sb.append("; [");

        int len = sb.length();

        if (perms[ADD_HANDLE]) sb.append("create hdl,");
        if (perms[DELETE_HANDLE]) sb.append("delete hdl,");
        if (perms[ADD_DERIVED_PREFIX]) sb.append("create derived prefix,");
        if (perms[DELETE_DERIVED_PREFIX]) sb.append("delete derived prefix,");
        if (perms[READ_VALUE]) sb.append("read val,");
        if (perms[MODIFY_VALUE]) sb.append("modify val,");
        if (perms[REMOVE_VALUE]) sb.append("del val,");
        if (perms[ADD_VALUE]) sb.append("add val,");
        if (perms[MODIFY_ADMIN]) sb.append("modify admin,");
        if (perms[REMOVE_ADMIN]) sb.append("del admin,");
        if (perms[ADD_ADMIN]) sb.append("add admin,");
        if (perms[LIST_HANDLES]) sb.append("list,");

        if (len != sb.length()) sb.deleteCharAt(sb.length() - 1);

        sb.append("]");

        return sb.toString();
    }

}
