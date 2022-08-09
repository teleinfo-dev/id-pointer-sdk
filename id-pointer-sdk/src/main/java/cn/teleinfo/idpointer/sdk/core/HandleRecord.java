/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HandleRecord {
    private String handle;
    private List<HandleValue> values;

    public HandleRecord() {
    }

    public HandleRecord(String handle, HandleValue[] valuesArray) {
        this.handle = handle;
        if (valuesArray != null) {
            values = new ArrayList<>();
            values.addAll(Arrays.asList(valuesArray));
        } else {
            values = null;
        }
    }

    public HandleRecord(String handle, List<HandleValue> values) {
        this.handle = handle;
        this.values = values;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public byte[] getHandleBytes() {
        return Util.encodeString(handle);
    }

    public List<HandleValue> getValues() {
        return values;
    }

    public void setValues(List<HandleValue> values) {
        this.values = values;
    }

    public void setValues(HandleValue[] valuesArray) {
        if (valuesArray != null) {
            values = new ArrayList<>();
            values.addAll(Arrays.asList(valuesArray));
        } else {
            values = null;
        }
    }

    public HandleValue[] getValuesAsArray() {
        if (values == null) return null;
        HandleValue[] result = values.toArray(new HandleValue[values.size()]);
        return result;
    }

    public HandleValue getValueAtIndex(int index) {
        if (values == null) return null;
        for (HandleValue value : values) {
            if (value.getIndex() == index) return value;
        }
        return null;
    }

    public List<HandleValue> getValuesOfType(String type) {
        List<HandleValue> result = new ArrayList<>();
        if (values == null) return result;
        for (HandleValue value : values) {
            byte[] typeBytes = value.getType();
            String valueType = Util.decodeString(typeBytes);
            if (type.equals(valueType)) {
                result.add(value);
            }
        }
        return result;
    }

}
