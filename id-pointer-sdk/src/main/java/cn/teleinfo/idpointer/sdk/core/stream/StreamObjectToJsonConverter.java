/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
         http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Enumeration;

@SuppressWarnings("rawtypes")
public class StreamObjectToJsonConverter {

    public static JsonElement toJson(StreamObject streamObject) {
        JsonElement result = null;
        if (streamObject.isStreamTable()) {
            result = toJson((StreamTable) streamObject);
        } else if (streamObject.isStreamVector()) {
            result = toJson((StreamVector) streamObject);
        }
        return result;
    }

    private static JsonObject toJson(StreamTable streamTable) {
        JsonObject jsonObject = new JsonObject();
        for (Enumeration e = streamTable.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object value = streamTable.get(key);
            if (value instanceof StreamObject) {
                StreamObject childObject = (StreamObject) value;
                JsonElement childElement = toJson(childObject);
                jsonObject.add((String) key, childElement);
            } else if (value instanceof String) {
                jsonObject.addProperty((String) key, (String) value);
            }
        }
        return jsonObject;
    }

    private static JsonArray toJson(StreamVector streamVector) {
        JsonArray jsonArray = new JsonArray();
        for (Enumeration e = streamVector.elements(); e.hasMoreElements();) {
            Object item = e.nextElement();
            if (item instanceof StreamObject) {
                StreamObject childObject = (StreamObject) item;
                JsonElement childElement = toJson(childObject);
                jsonArray.add(childElement);
            } else if (item instanceof String) {
                jsonArray.add(new JsonPrimitive((String) item));
            }
        }
        return jsonArray;
    }
}
