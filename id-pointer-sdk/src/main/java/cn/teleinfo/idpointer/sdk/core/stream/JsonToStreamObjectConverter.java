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

import java.math.BigDecimal;
import java.util.Map.Entry;

public class JsonToStreamObjectConverter {

    public static StreamObject toStreamObject(JsonElement jsonElement) {
        StreamObject result = null;
        if (jsonElement.isJsonObject()) {
            result = toStreamTable((JsonObject) jsonElement);
        } else {
            result = toStreamVector((JsonArray) jsonElement);
        }
        return result;
    }

    private static StreamTable toStreamTable(JsonObject jsonObject) {
        StreamTable streamTable = new StreamTable();
        for (Entry<String, JsonElement> properties : jsonObject.entrySet()) {
            String key = properties.getKey();
            JsonElement value = properties.getValue();
            if (value.isJsonObject() || value.isJsonArray()) {
                StreamObject childObject = toStreamObject(value);
                streamTable.put(key, childObject);
            } else if (value.isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = value.getAsJsonPrimitive();
                if (jsonPrimitive.isString()) {
                    String stringValue = value.getAsJsonPrimitive().getAsString();
                    streamTable.put(key, stringValue);
                } else if (jsonPrimitive.isBoolean()) {
                    boolean booleanValue = jsonPrimitive.getAsBoolean();
                    streamTable.put(key, booleanValue);
                } else if (jsonPrimitive.isNumber()) {
                    BigDecimal number = jsonPrimitive.getAsBigDecimal();
                    String numberAsString = number.toPlainString();
                    streamTable.put(key, numberAsString);
                } else if (jsonPrimitive.isJsonNull()) {
                    streamTable.put(key, "null");
                }
            }
        }
        return streamTable;
    }

    private static StreamVector toStreamVector(JsonArray jsonArray) {
        StreamVector streamVector = new StreamVector();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement item = jsonArray.get(i);
            if (item.isJsonObject() || item.isJsonArray()) {
                StreamObject childObject = toStreamObject(item);
                streamVector.add(childObject);
            } else if (item.isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = item.getAsJsonPrimitive();
                if (jsonPrimitive.isString()) {
                    String stringValue = item.getAsJsonPrimitive().getAsString();
                    streamVector.add(stringValue);
                } else if (jsonPrimitive.isBoolean()) {
                    boolean booleanValue = jsonPrimitive.getAsBoolean();
                    streamVector.add(String.valueOf(booleanValue));
                } else if (jsonPrimitive.isNumber()) {
                    BigDecimal number = jsonPrimitive.getAsBigDecimal();
                    String numberAsString = number.toPlainString();
                    streamVector.add(numberAsString);
                } else if (jsonPrimitive.isJsonNull()) {
                    streamVector.add("null");
                }
            }
        }
        return streamVector;
    }
}
