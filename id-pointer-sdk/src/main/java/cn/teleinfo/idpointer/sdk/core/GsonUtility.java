/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
 All rights reserved.

 The HANDLE.NET software is made available subject to the
 Handle.Net Public License Agreement, which may be obtained at
 http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
 \**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.core.stream.util.FastDateFormat;
import cn.teleinfo.idpointer.sdk.security.gm.SM2Factory;
import cn.teleinfo.idpointer.sdk.security.gm.SM2PublicKey;
import com.google.gson.*;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.interfaces.*;
import java.security.spec.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GsonUtility {
    /**
     * Register Handle.net type adapters on a given GsonBuilder, to enable serialization and deserialization of various Handle.net types.
     *
     * @param gsonBuilder a GsonBuilder
     * @return the passed-in GsonBuilder.
     */
    public static GsonBuilder setup(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(HandleValue.class, new HandleValueGsonTypeAdapter());
        gsonBuilder.registerTypeAdapter(Transaction.class, new TransactionGsonTypeAdapter());
        gsonBuilder.registerTypeAdapter(ValueReference.class, new ValueReferenceGsonTypeAdapter());
        gsonBuilder.registerTypeAdapter(AdminRecord.class, new AdminRecordGsonTypeAdapter());
        gsonBuilder.registerTypeAdapter(SiteInfo.class, new SiteInfoGsonTypeAdapter());
        gsonBuilder.registerTypeAdapter(ServerInfo.class, new ServerInfoGsonTypeAdapter());
        gsonBuilder.registerTypeAdapter(Interface.class, new InterfaceGsonTypeAdapter());
        gsonBuilder.registerTypeHierarchyAdapter(AbstractIdResponse.class, new ResponseGsonTypeHierarchyAdapter());
        gsonBuilder.registerTypeHierarchyAdapter(PublicKey.class, new PublicKeyTypeHierarchyAdapter());
        gsonBuilder.registerTypeHierarchyAdapter(PrivateKey.class, new PrivateKeyTypeHierarchyAdapter());
        return gsonBuilder;
    }

    /**
     * Returns a GsonBuilder which can serialize and deserialize various Handle.net types.
     *
     * @return a GsonBuilder which can serialize and deserialize various Handle.net types.
     */
    public static GsonBuilder getNewGsonBuilder() {
        return setup(new GsonBuilder());
    }

    /**
     * Returns a Gson instance which can serialize and deserialize various Handle.net types.  This Gson instance has HTML escaping disabled.
     *
     * @return a Gson instance which can serialize and deserialize various Handle.net types.
     */
    public static Gson getGson() {
        return GsonHolder.gson;
    }

    /**
     * Returns a Gson instance which can serialize and deserialize various Handle.net types.  This Gson instance has HTML escaping disabled and pretty-printing enabled.
     *
     * @return a Gson instance which can serialize and deserialize various Handle.net types.
     */
    public static Gson getPrettyGson() {
        return PrettyGsonHolder.prettyGson;
    }

    private static class GsonHolder {
        static Gson gson;

        static {
            gson = GsonUtility.setup(new GsonBuilder().disableHtmlEscaping()).create();
        }
    }

    private static class PrettyGsonHolder {
        static Gson prettyGson;

        static {
            prettyGson = GsonUtility.setup(new GsonBuilder().disableHtmlEscaping().setPrettyPrinting()).create();
        }
    }

    private static String lowerCaseFirst(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    static JsonObject lowerCaseIfNeeded(JsonObject json) {
        if (json.has("ServerList") || json.has("Address") || json.has("Port")) {
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                obj.add(lowerCaseFirst(entry.getKey()), entry.getValue());
            }
            return obj;
        } else return json;
    }

    static String secondsTimestampAsString(int seconds) {
        return FastDateFormat.formatUtc(FastDateFormat.FormatSpec.ISO8601_NO_MS, 1000L * seconds);
    }

    static int secondsTimestampFromString(String s) throws ParseException {
        return (int) (FastDateFormat.parseUtc(s) / 1000);
    }

    static JsonElement serializeData(HandleValue value, JsonSerializationContext context) {
        try {
            if (value.hasType(Common.ADMIN_TYPE)) {
                AdminRecord adminRecord = new AdminRecord();
                Encoder.decodeAdminRecord(value.getData(), 0, adminRecord);
                return dataOfType("admin", context.serialize(adminRecord));
            } else if (value.hasType(Common.ADMIN_GROUP_TYPE)) {
                ValueReference[] refs = Encoder.decodeValueReferenceList(value.getData(), 0);
                return dataOfType("vlist", context.serialize(refs));
            } else if (value.hasType(Common.SITE_INFO_TYPE) || value.hasType(Common.DERIVED_PREFIX_SITE_TYPE) || value.hasType(Common.LEGACY_DERIVED_PREFIX_SITE_TYPE)) {
                SiteInfo site = new SiteInfo();
                Encoder.decodeSiteInfoRecord(value.getData(), 0, site);
                return dataOfType("site", context.serialize(site));
            } else if (value.hasType(Common.PUBLIC_KEY_TYPE)) {
                PublicKey key = Util.getPublicKeyFromBytes(value.getData());
                return dataOfType("key", context.serialize(key));
            }
        } catch (Exception e) {
            // fall-through
        }
        if (!Util.looksLikeBinary(value.getData())) {
            return serializeString(Util.decodeString(value.getData()));
        } else return serializeBinary(value.getData());
    }

    private static JsonElement dataOfType(String type, JsonElement value) {
        JsonObject obj = new JsonObject();
        obj.addProperty("format", type);
        obj.add("value", value);
        return obj;
    }

    static JsonElement serializeBinary(byte[] bytes) {
        JsonObject obj = new JsonObject();
        obj.addProperty("format", "base64");
        obj.addProperty("value", Base64.encodeBase64String(bytes));
        return obj;
    }

    static JsonElement serializeString(String s) {
        JsonObject obj = new JsonObject();
        obj.addProperty("format", "string");
        obj.addProperty("value", s);
        return obj;
    }

    static byte[] deserializeData(JsonElement json, JsonDeserializationContext context) throws DecoderException, ParseException {
        if (json.isJsonPrimitive()) {
            return Util.encodeString(json.getAsString());
        } else {
            JsonObject obj = json.getAsJsonObject();
            String format = obj.get("format").getAsString();
            JsonElement value = obj.get("value");
            if (format.equalsIgnoreCase("string")) {
                return Util.encodeString(value.getAsString());
            } else if (format.equalsIgnoreCase("base64")) {
                return Base64.decodeBase64(value.getAsString());
            } else if (format.equalsIgnoreCase("hex")) {
                return Hex.decodeHex(value.getAsString().toCharArray());
            } else if (format.equalsIgnoreCase("admin")) {
                AdminRecord adminRecord = context.deserialize(value, AdminRecord.class);
                return Encoder.encodeAdminRecord(adminRecord);
            } else if (format.equalsIgnoreCase("vlist")) {
                ValueReference[] refs = context.deserialize(value, ValueReference[].class);
                return Encoder.encodeValueReferenceList(refs);
            } else if (format.equalsIgnoreCase("site")) {
                SiteInfo site = context.deserialize(value, SiteInfo.class);
                return Encoder.encodeSiteInfoRecord(site);
            } else if (format.equalsIgnoreCase("key")) {
                PublicKey key = context.deserialize(value, PublicKey.class);
                try {
                    return Util.getBytesFromPublicKey(key);
                } catch (HandleException e) {
                    ParseException pe = new ParseException("Unable to deserialize public key", 0);
                    pe.initCause(e);
                    throw pe;
                }
            } else throw new ParseException("Unexpected type " + format, 0);
        }
    }

    /**
     * Serialize a response, adding in the handle value from the given request.
     *
     * @param req  a request
     * @param resp a response
     * @return The response, serialized as a JSON tree, with the "handle" value from the request if not already in the response.
     */
    public static JsonElement serializeResponseToRequest(AbstractIdRequest req, AbstractIdResponse resp) {
        JsonObject json = getGson().toJsonTree(resp).getAsJsonObject();
        if (json.has("handle")) return json;
        if (req != null && req.handle != null && req.handle.length > 0 && !Util.equals(Common.BLANK_HANDLE, req.handle))
            json.addProperty("handle", Util.decodeString(req.handle));
        return json;
    }

    public static class HandleValueGsonTypeAdapter implements JsonSerializer<HandleValue>, JsonDeserializer<HandleValue> {
        @Override
        public JsonElement serialize(HandleValue value, Type type, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("index", Integer.valueOf(value.index));
            json.addProperty("type", Util.decodeString(value.type));
            json.add("data", serializeData(value, context));
            if (unusualPermissions(value)) json.add("permissions", serializePermissions(value));
            json.add("ttl", serializeTtl(value));
            if (value.timestamp != 0) {
                json.addProperty("timestamp", secondsTimestampAsString(value.timestamp));
            }
            if (value.references != null && value.references.length > 0) {
                json.add("references", context.serialize(value.references));
            }
            return json;
        }

        @Override
        public HandleValue deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject obj = json.getAsJsonObject();
                HandleValue value = new HandleValue();
                value.index = obj.get("index").getAsInt();
                value.type = Util.encodeString(obj.get("type").getAsString());
                value.data = deserializeData(obj.get("data"), context);
                deserializePermissions(value, obj);
                deserializeTtl(value, obj);
                if (obj.has("timestamp"))
                    value.timestamp = secondsTimestampFromString(obj.get("timestamp").getAsString());
                JsonElement refs = obj.get("references");
                if (refs != null && !refs.isJsonNull()) {
                    value.references = context.deserialize(refs, ValueReference[].class);
                    ensureNoTrailingComma(value.references);
                }
                return value;
            } catch (JsonParseException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }

        private static boolean unusualPermissions(HandleValue value) {
            return !value.adminRead || !value.adminWrite || !value.publicRead || value.publicWrite;
        }

        private static JsonElement serializeTtl(HandleValue value) {
            if (value.ttlType == HandleValue.TTL_TYPE_RELATIVE) {
                return new JsonPrimitive(Integer.valueOf(value.ttl));
            } else {
                return new JsonPrimitive(secondsTimestampAsString(value.ttl));
            }
        }

        private static void deserializeTtl(HandleValue value, JsonObject obj) throws ParseException {
            JsonElement ttl = obj.get("ttl");
            if (ttl != null) {
                JsonPrimitive ttlPrim = ttl.getAsJsonPrimitive();
                if (ttlPrim.isString()) {
                    value.ttlType = HandleValue.TTL_TYPE_ABSOLUTE;
                    value.ttl = secondsTimestampFromString(ttlPrim.getAsString());
                } else if (ttlPrim.isNumber()) {
                    value.ttlType = HandleValue.TTL_TYPE_RELATIVE;
                    value.ttl = ttlPrim.getAsInt();
                } else throw new NumberFormatException("bad ttl");
            }
        }

        private static String bit(boolean b) {
            return b ? "1" : "0";
        }

        private static JsonElement serializePermissions(HandleValue value) {
            return new JsonPrimitive(bit(value.adminRead) + bit(value.adminWrite) + bit(value.publicRead) + bit(value.publicWrite));
        }

        private static void deserializePermissions(HandleValue value, JsonObject obj) {
            if (obj.has("permissions")) {
                String perms = obj.get("permissions").getAsString();
                if ("*".equals(perms)) {
                    value.publicRead = value.publicWrite = value.adminRead = value.adminWrite = true;
                } else {
                    if (perms.length() > 1) value.publicWrite = '1' == perms.charAt(perms.length() - 1);
                    if (perms.length() > 2) value.publicRead = '1' == perms.charAt(perms.length() - 2);
                    if (perms.length() > 3) value.adminWrite = '1' == perms.charAt(perms.length() - 3);
                    if (perms.length() > 4) value.adminRead = '1' == perms.charAt(perms.length() - 4);
                }
            }
        }

    }

    public static class TransactionGsonTypeAdapter implements JsonSerializer<Transaction>, JsonDeserializer<Transaction> {
        @Override
        public JsonElement serialize(Transaction txn, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("txnId", txn.txnId);
            json.addProperty("handle", Util.decodeString(txn.handle));
            json.addProperty("action", Transaction.actionToString(txn.action));
            json.addProperty("date", FastDateFormat.getUtcFormat().format(txn.date));
            return json;
        }

        @Override
        public Transaction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject obj = json.getAsJsonObject();
                int txnId = obj.get("txnId").getAsInt();
                String handleString = obj.get("handle").getAsString();
                byte[] handle = Util.encodeString(handleString);
                byte action = Transaction.stringToAction(obj.get("action").getAsString());
                long date = FastDateFormat.parseUtc(obj.get("date").getAsString());
                return new Transaction(txnId, handle, action, date);
            } catch (JsonParseException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }

    public static class ValueReferenceGsonTypeAdapter implements JsonSerializer<ValueReference>, JsonDeserializer<ValueReference> {
        @Override
        public JsonElement serialize(ValueReference valueRef, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("handle", Util.decodeString(valueRef.handle));
            json.addProperty("index", Integer.valueOf(valueRef.index));
            return json;
        }

        @Override
        public ValueReference deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject obj = json.getAsJsonObject();
                String handle = obj.get("handle").getAsString();
                int index = obj.get("index").getAsInt();
                return new ValueReference(Util.encodeString(handle), index);
            } catch (JsonParseException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }

    public static class AdminRecordGsonTypeAdapter implements JsonSerializer<AdminRecord>, JsonDeserializer<AdminRecord> {
        private static boolean[] allTrueArray = {true, true, true, true, true, true, true, true, true, true, true, true};

        private static String permsArrayToString(boolean[] perms) {
            if (perms == null) return null;
            // boolean allTrue = true;
            StringBuilder sb = new StringBuilder(perms.length);
            for (int i = perms.length - 1; i >= 0; i--) {
                // if(!perms[i]) allTrue = false;
                sb.append(perms[i] ? '1' : '0');
            }
            // if(allTrue) return "*";
            return sb.toString();
        }

        private static boolean[] permsStringToArray(String perms) {
            if ("*".equals(perms)) return allTrueArray.clone();
            boolean[] res = new boolean[12];
            if (perms == null) return res;
            for (int i = 0; i < res.length; i++) {
                if (perms.length() <= i) break;
                res[i] = '1' == perms.charAt(perms.length() - 1 - i);
            }
            return res;
        }

        private static boolean[] permsPrimitiveToArray(JsonPrimitive perms) {
            if (perms.isBoolean()) {
                if (perms.getAsBoolean()) return allTrueArray.clone();
                else return new boolean[12];
            } else if (perms.isString()) return permsStringToArray(perms.getAsString());
            else return permsStringToArray(Integer.toBinaryString(perms.getAsInt()));
        }

        private static boolean[] permsArrayToArray(JsonArray perms) {
            boolean[] res = new boolean[12];
            for (int i = 0; i < 12 && i < perms.size(); i++) {
                res[i] = isTruthy(perms.get(i));
            }
            return res;
        }

        private static boolean isTruthy(JsonElement json) {
            if (json == null || json.isJsonNull()) return false;
            if (!json.isJsonPrimitive()) return true;
            JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
            if (jsonPrimitive.isBoolean()) return jsonPrimitive.getAsBoolean();
            if (jsonPrimitive.isString()) return !jsonPrimitive.getAsString().isEmpty();
            double num = jsonPrimitive.getAsNumber().doubleValue();
            if (Double.isNaN(num)) return false;
            if (num == 0.0) return false;
            return true;
        }

        private static boolean[] permsElementToArray(JsonElement perms) {
            if (perms == null || perms.isJsonNull()) return new boolean[12];
            else if (perms.isJsonPrimitive()) return permsPrimitiveToArray(perms.getAsJsonPrimitive());
            else if (perms.isJsonArray()) return permsArrayToArray(perms.getAsJsonArray());
            else throw new IllegalStateException("Did not expect permissions object");
        }

        @Override
        public JsonElement serialize(AdminRecord src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("handle", Util.decodeString(src.adminId));
            json.addProperty("index", src.adminIdIndex);
            json.addProperty("permissions", permsArrayToString(src.perms));
            if (src.legacyByteLength) json.addProperty("legacyByteLength", true);
            return json;
        }

        @Override
        public AdminRecord deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject obj = json.getAsJsonObject();
                String handle = obj.get("handle").getAsString();
                int index = obj.get("index").getAsInt();
                JsonElement perms = obj.get("permissions");
                boolean[] permsArray = permsElementToArray(perms);
                AdminRecord res = new AdminRecord(Util.encodeString(handle), index, permsArray[0], permsArray[1], permsArray[2], permsArray[3], permsArray[4], permsArray[5], permsArray[6], permsArray[7], permsArray[8], permsArray[9],
                        permsArray[10], permsArray[11]);
                JsonElement legacyByteLengthElement = obj.get("legacyByteLength");
                if (legacyByteLengthElement != null && legacyByteLengthElement.getAsBoolean())
                    res.legacyByteLength = true;
                return res;
            } catch (JsonParseException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }

    public static class SiteInfoGsonTypeAdapter implements JsonSerializer<SiteInfo>, JsonDeserializer<SiteInfo> {
        private static JsonElement serializeAttributes(Attribute[] attributes) {
            JsonArray json = new JsonArray();
            for (Attribute att : attributes) {
                JsonObject attJson = new JsonObject();
                attJson.addProperty("name", Util.decodeString(att.name));
                attJson.addProperty("value", Util.decodeString(att.value));
                json.add(attJson);
            }
            return json;
        }

        private static Attribute[] deserializeAttributes(JsonElement json) {
            List<Attribute> atts = new ArrayList<>();
            if (json.isJsonArray()) {
                for (JsonElement el : json.getAsJsonArray()) {
                    JsonObject obj = el.getAsJsonObject();
                    atts.add(new Attribute(Util.encodeString(obj.get("name").getAsString()), Util.encodeString(obj.get("value").getAsString())));
                }
            } else { //if(json.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonArray()) {
                        for (JsonElement el : entry.getValue().getAsJsonArray()) {
                            atts.add(new Attribute(Util.encodeString(entry.getKey()), Util.encodeString(el.getAsString())));
                        }
                    } else {
                        atts.add(new Attribute(Util.encodeString(entry.getKey()), Util.encodeString(entry.getValue().getAsString())));
                    }
                }
            }
            return atts.toArray(new Attribute[atts.size()]);
        }

        @Override
        public JsonElement serialize(SiteInfo site, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("version", Integer.valueOf(site.dataFormatVersion));
            json.addProperty("protocolVersion", "" + Integer.valueOf(site.majorProtocolVersion) + "." + Integer.valueOf(site.minorProtocolVersion));
            json.addProperty("serialNumber", Integer.valueOf(site.serialNumber));
            json.addProperty("primarySite", Boolean.valueOf(site.isPrimary));
            json.addProperty("multiPrimary", Boolean.valueOf(site.multiPrimary));
            if (site.hashOption != SiteInfo.HASH_TYPE_BY_ALL)
                json.addProperty("hashOption", Integer.valueOf(site.hashOption));
            if (site.hashFilter != null && site.hashFilter.length > 0)
                json.addProperty("hashFilter", Util.decodeString(site.hashFilter));
            if (site.attributes != null) json.add("attributes", serializeAttributes(site.attributes));
            if (site.servers != null) json.add("servers", context.serialize(site.servers));
            return json;
        }

        @Override
        public SiteInfo deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject obj = json.getAsJsonObject();
                obj = lowerCaseIfNeeded(obj);
                SiteInfo site = new SiteInfo();
                if (obj.has("version")) site.dataFormatVersion = obj.get("version").getAsInt();
                if (obj.has("protocolVersion")) {
                    String versionString = obj.get("protocolVersion").getAsString();
                    int point = versionString.indexOf('.');
                    site.majorProtocolVersion = Byte.parseByte(versionString.substring(0, point));
                    site.minorProtocolVersion = Byte.parseByte(versionString.substring(point + 1));
                }
                if (obj.has("serialNumber")) site.serialNumber = obj.get("serialNumber").getAsInt();
                if (obj.has("primarySite")) site.isPrimary = obj.get("primarySite").getAsBoolean();
                if (obj.has("multiPrimary")) site.multiPrimary = obj.get("multiPrimary").getAsBoolean();
                if (obj.has("hashOption")) site.hashOption = obj.get("hashOption").getAsByte();
                if (obj.has("hashFilter")) site.hashFilter = Util.encodeString(obj.get("hashFilter").getAsString());
                if (obj.has("attributes")) site.attributes = deserializeAttributes(obj.get("attributes"));
                JsonElement servers = obj.get("servers");
                if (servers == null) servers = obj.get("serverList");
                site.servers = context.deserialize(servers, ServerInfo[].class);
                ensureNoTrailingComma(site.servers);
                return site;
            } catch (JsonParseException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }

    public static class ServerInfoGsonTypeAdapter implements JsonSerializer<ServerInfo>, JsonDeserializer<ServerInfo> {
        private static byte[] fill16(byte[] bytes) {
            if (bytes.length == 16) return bytes;
            byte[] res = new byte[16];
            System.arraycopy(bytes, 0, res, 16 - bytes.length, bytes.length);
            return res;
        }

        @Override
        public JsonElement serialize(ServerInfo server, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("serverId", Integer.valueOf(server.serverId));
            json.addProperty("address", server.getAddressString());
            try {
                PublicKey key = Util.getPublicKeyFromBytes(server.publicKey);
                json.add("publicKey", dataOfType("key", context.serialize(key)));
            } catch (Exception e) {
                json.add("publicKey", serializeBinary(server.publicKey));
            }
            json.add("interfaces", context.serialize(server.interfaces));
            return json;
        }

        @Override
        public ServerInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject origObj = json.getAsJsonObject();
                JsonObject obj = lowerCaseIfNeeded(origObj);
                boolean legacy = obj != origObj;
                ServerInfo server = new ServerInfo();
                if (obj.has("serverId")) server.serverId = obj.get("serverId").getAsInt();
                server.ipAddress = fill16(InetAddress.getByName(obj.get("address").getAsString()).getAddress());
                if (obj.has("publicKey")) {
                    if (legacy && obj.get("publicKey").isJsonPrimitive())
                        server.publicKey = Hex.decodeHex(obj.get("publicKey").getAsString().toCharArray());
                    else server.publicKey = deserializeData(obj.get("publicKey"), context);
                }
                JsonElement interfaces = obj.get("interfaces");
                if (interfaces == null) interfaces = obj.get("interfaceList");
                server.interfaces = context.deserialize(interfaces, Interface[].class);
                ensureNoTrailingComma(server.interfaces);
                return server;
            } catch (JsonParseException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }

    public static class InterfaceGsonTypeAdapter implements JsonSerializer<Interface>, JsonDeserializer<Interface> {
        @Override
        public JsonElement serialize(Interface intf, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("query", Boolean.valueOf((intf.type & Interface.ST_QUERY) != 0));
            json.addProperty("admin", Boolean.valueOf((intf.type & Interface.ST_ADMIN) != 0));
            serializeProtocol(json, intf.protocol);
            json.addProperty("port", Integer.valueOf(intf.port));
            return json;
        }

        private static void serializeProtocol(JsonObject json, byte protocol) {
            if (protocol == Interface.SP_HDL_UDP) json.addProperty("protocol", "UDP");
            else if (protocol == Interface.SP_HDL_TCP) json.addProperty("protocol", "TCP");
            else if (protocol == Interface.SP_HDL_HTTP) json.addProperty("protocol", "HTTP");
            else if (protocol == Interface.SP_HDL_HTTPS) json.addProperty("protocol", "HTTPS");
            else json.addProperty("protocol", Integer.valueOf(protocol));
        }

        @Override
        public Interface deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject obj = json.getAsJsonObject();
                obj = lowerCaseIfNeeded(obj);
                Interface intf = new Interface();
                boolean query = obj.has("query") && obj.get("query").getAsBoolean();
                boolean admin = obj.has("admin") && obj.get("admin").getAsBoolean();
                intf.type = query ? (admin ? Interface.ST_ADMIN_AND_QUERY : Interface.ST_QUERY) : (admin ? Interface.ST_ADMIN : Interface.ST_OUT_OF_SERVICE);
                String protocol = obj.get("protocol").getAsString();
                if ("UDP".equals(protocol)) intf.protocol = Interface.SP_HDL_UDP;
                else if ("TCP".equals(protocol)) intf.protocol = Interface.SP_HDL_TCP;
                else if ("HTTP".equals(protocol)) intf.protocol = Interface.SP_HDL_HTTP;
                else if ("HTTPS".equals(protocol)) intf.protocol = Interface.SP_HDL_HTTPS;
                else intf.protocol = obj.get("protocol").getAsByte();
                intf.port = obj.get("port").getAsInt();
                return intf;
            } catch (JsonParseException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }

    public static class ResponseGsonTypeHierarchyAdapter implements JsonSerializer<AbstractIdResponse> {
        @Override
        public JsonElement serialize(AbstractIdResponse resp, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            //json.addProperty("opCode",Integer.valueOf(resp.opCode));
            json.addProperty("responseCode", Integer.valueOf(resp.responseCode));
            if (resp instanceof ResolutionIdResponse) {
                ResolutionIdResponse rresp = (ResolutionIdResponse) resp;
                json.addProperty("handle", Util.decodeString(rresp.handle));
                try {
                    HandleValue[] values = rresp.getHandleValues();
                    json.add("values", context.serialize(values));
                } catch (Exception e) {
                    json.addProperty("responseCode", Integer.valueOf(AbstractMessage.RC_ERROR));
                    json.addProperty("message", "Error decoding values of resolution response " + e.toString());
                }
            } else if (resp instanceof ServiceReferralIdResponse) {
                ServiceReferralIdResponse rresp = (ServiceReferralIdResponse) resp;
                if (rresp.handle.length > 0) json.addProperty("referralHandle", Util.decodeString(rresp.handle));
                if (rresp.values != null && rresp.values.length > 0) {
                    try {
                        HandleValue[] values = rresp.getHandleValues();
                        json.add("values", context.serialize(values));
                    } catch (Exception e) {
                        json.addProperty("responseCode", Integer.valueOf(AbstractMessage.RC_ERROR));
                        json.addProperty("message", "Error decoding values of resolution response " + e.toString());
                    }
                }
            } else if (resp instanceof ErrorIdResponse) {
                ErrorIdResponse eresp = (ErrorIdResponse) resp;
                if (eresp.message != null && eresp.message.length > 0) {
                    json.addProperty("message", Util.decodeString(eresp.message));
                }
                if (resp.opCode == AbstractMessage.OC_RESOLUTION && resp.responseCode == AbstractMessage.RC_VALUES_NOT_FOUND) {
                    json.add("values", new JsonArray());
                }
            } else if (resp instanceof GetSiteInfoIdResponse) {
                GetSiteInfoIdResponse gresp = (GetSiteInfoIdResponse) resp;
                json.add("site", context.serialize(gresp.siteInfo));
            } else if (resp instanceof VerifyAuthIdResponse) {
                json.addProperty("isValid", Boolean.valueOf(((VerifyAuthIdResponse) resp).isValid));
            } else if (resp instanceof CreateHandleIdResponse) {
                CreateHandleIdResponse creResp = (CreateHandleIdResponse) resp;
                if (creResp.handle != null) {
                    json.addProperty("handle", Util.decodeString(creResp.handle));
                }
            }
            return json;
        }
    }

    public static class PublicKeyTypeHierarchyAdapter implements JsonSerializer<PublicKey>, JsonDeserializer<PublicKey> {
        @Override
        public JsonElement serialize(PublicKey key, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            if (key instanceof DSAPublicKey) {
                DSAPublicKey dsaKey = (DSAPublicKey) key;
                byte[] y = dsaKey.getY().toByteArray();
                DSAParams dsaParams = dsaKey.getParams();
                byte[] p = dsaParams.getP().toByteArray();
                byte[] q = dsaParams.getQ().toByteArray();
                byte[] g = dsaParams.getG().toByteArray();
                json.addProperty("kty", "DSA");
                json.addProperty("y", Base64.encodeBase64URLSafeString(unsigned(y)));
                json.addProperty("p", Base64.encodeBase64URLSafeString(unsigned(p)));
                json.addProperty("q", Base64.encodeBase64URLSafeString(unsigned(q)));
                json.addProperty("g", Base64.encodeBase64URLSafeString(unsigned(g)));
            } else if (key instanceof RSAPublicKey) {
                RSAPublicKey rsaKey = (RSAPublicKey) key;
                byte[] n = rsaKey.getModulus().toByteArray();
                byte[] e = rsaKey.getPublicExponent().toByteArray();
                json.addProperty("kty", "RSA");
                json.addProperty("n", Base64.encodeBase64URLSafeString(unsigned(n)));
                json.addProperty("e", Base64.encodeBase64URLSafeString(unsigned(e)));
            } else if (key instanceof SM2PublicKey) {
                SM2PublicKey ecKey = (SM2PublicKey) key;
                json.addProperty("kty", "SM2");
                json.addProperty("d", Base64.encodeBase64URLSafeString(key.getEncoded()));
            } else {
                throw new UnsupportedOperationException("Unsupported key type " + key.getClass().getName());
            }
            return json;
        }

        @Override
        public PublicKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject obj = json.getAsJsonObject();
                String kty = obj.get("kty").getAsString();
                if ("DSA".equalsIgnoreCase(kty)) {
                    byte[] y = Base64.decodeBase64(obj.get("y").getAsString());
                    byte[] p = Base64.decodeBase64(obj.get("p").getAsString());
                    byte[] q = Base64.decodeBase64(obj.get("q").getAsString());
                    byte[] g = Base64.decodeBase64(obj.get("g").getAsString());
                    DSAPublicKeySpec keySpec = new DSAPublicKeySpec(new BigInteger(1, y), new BigInteger(1, p), new BigInteger(1, q), new BigInteger(1, g));
                    KeyFactory dsaKeyFactory = KeyFactory.getInstance("DSA");
                    return dsaKeyFactory.generatePublic(keySpec);
                } else if ("RSA".equalsIgnoreCase(kty)) {
                    byte[] n = Base64.decodeBase64(obj.get("n").getAsString());
                    byte[] e = Base64.decodeBase64(obj.get("e").getAsString());
                    RSAPublicKeySpec keySpec = new RSAPublicKeySpec(new BigInteger(1, n), new BigInteger(1, e));
                    KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
                    return rsaKeyFactory.generatePublic(keySpec);
                } else if ("SM2".equalsIgnoreCase(kty)) {
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decodeBase64(obj.get("d").getAsString()));
                    Provider provider = SM2Factory.getProvider();
                    KeyFactory rsaKeyFactory = KeyFactory.getInstance("SM2",provider);
                    return rsaKeyFactory.generatePublic(keySpec);
                } else {
                    throw new UnsupportedOperationException("Unsupported key type " + kty);
                }
            } catch (JsonParseException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }

    public static class PrivateKeyTypeHierarchyAdapter implements JsonSerializer<PrivateKey>, JsonDeserializer<PrivateKey> {
        @Override
        public JsonElement serialize(PrivateKey key, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            if (key instanceof DSAPrivateKey) {
                DSAPrivateKey dsaKey = (DSAPrivateKey) key;
                byte[] x = dsaKey.getX().toByteArray();
                DSAParams dsaParams = dsaKey.getParams();
                byte[] p = dsaParams.getP().toByteArray();
                byte[] q = dsaParams.getQ().toByteArray();
                byte[] g = dsaParams.getG().toByteArray();
                json.addProperty("kty", "DSA");
                json.addProperty("x", Base64.encodeBase64URLSafeString(unsigned(x)));
                json.addProperty("p", Base64.encodeBase64URLSafeString(unsigned(p)));
                json.addProperty("q", Base64.encodeBase64URLSafeString(unsigned(q)));
                json.addProperty("g", Base64.encodeBase64URLSafeString(unsigned(g)));
            } else if (key instanceof RSAPrivateKey) {
                RSAPrivateKey rsaKey = (RSAPrivateKey) key;
                byte[] n = rsaKey.getModulus().toByteArray();
                byte[] d = rsaKey.getPrivateExponent().toByteArray();
                json.addProperty("kty", "RSA");
                if (key instanceof RSAPrivateCrtKey) {
                    RSAPrivateCrtKey rsacrtKey = (RSAPrivateCrtKey) rsaKey;
                    byte[] e = rsacrtKey.getPublicExponent().toByteArray();
                    byte[] p = rsacrtKey.getPrimeP().toByteArray();
                    byte[] q = rsacrtKey.getPrimeQ().toByteArray();
                    byte[] dp = rsacrtKey.getPrimeExponentP().toByteArray();
                    byte[] dq = rsacrtKey.getPrimeExponentQ().toByteArray();
                    byte[] qi = rsacrtKey.getCrtCoefficient().toByteArray();
                    json.addProperty("n", Base64.encodeBase64URLSafeString(unsigned(n)));
                    json.addProperty("e", Base64.encodeBase64URLSafeString(unsigned(e)));
                    json.addProperty("d", Base64.encodeBase64URLSafeString(unsigned(d)));
                    json.addProperty("p", Base64.encodeBase64URLSafeString(unsigned(p)));
                    json.addProperty("q", Base64.encodeBase64URLSafeString(unsigned(q)));
                    json.addProperty("dp", Base64.encodeBase64URLSafeString(unsigned(dp)));
                    json.addProperty("dq", Base64.encodeBase64URLSafeString(unsigned(dq)));
                    json.addProperty("qi", Base64.encodeBase64URLSafeString(unsigned(qi)));
                } else {
                    json.addProperty("n", Base64.encodeBase64URLSafeString(unsigned(n)));
                    json.addProperty("d", Base64.encodeBase64URLSafeString(unsigned(d)));
                }
            } else {
                throw new UnsupportedOperationException("Unsupported key type " + key.getClass().getName());
            }
            return json;
        }

        @Override
        public PrivateKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject obj = json.getAsJsonObject();
                String kty = obj.get("kty").getAsString();
                if ("DSA".equalsIgnoreCase(kty)) {
                    byte[] x = Base64.decodeBase64(obj.get("x").getAsString());
                    byte[] p = Base64.decodeBase64(obj.get("p").getAsString());
                    byte[] q = Base64.decodeBase64(obj.get("q").getAsString());
                    byte[] g = Base64.decodeBase64(obj.get("g").getAsString());
                    DSAPrivateKeySpec keySpec = new DSAPrivateKeySpec(new BigInteger(1, x), new BigInteger(1, p), new BigInteger(1, q), new BigInteger(1, g));
                    KeyFactory dsaKeyFactory = KeyFactory.getInstance("DSA");
                    return dsaKeyFactory.generatePrivate(keySpec);
                } else if ("RSA".equalsIgnoreCase(kty)) {
                    byte[] n = Base64.decodeBase64(obj.get("n").getAsString());
                    byte[] d = Base64.decodeBase64(obj.get("d").getAsString());
                    RSAPrivateKeySpec keySpec;
                    if (obj.has("qi")) {
                        byte[] e = Base64.decodeBase64(obj.get("e").getAsString());
                        byte[] p = Base64.decodeBase64(obj.get("p").getAsString());
                        byte[] q = Base64.decodeBase64(obj.get("q").getAsString());
                        byte[] dp = Base64.decodeBase64(obj.get("dp").getAsString());
                        byte[] dq = Base64.decodeBase64(obj.get("dq").getAsString());
                        byte[] qi = Base64.decodeBase64(obj.get("qi").getAsString());
                        keySpec = new RSAPrivateCrtKeySpec(new BigInteger(1, n), new BigInteger(1, e), new BigInteger(1, d), new BigInteger(1, p), new BigInteger(1, q), new BigInteger(1, dp), new BigInteger(1, dq), new BigInteger(1, qi));
                    } else {
                        keySpec = new RSAPrivateKeySpec(new BigInteger(1, n), new BigInteger(1, d));
                    }
                    KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
                    return rsaKeyFactory.generatePrivate(keySpec);
                } else {
                    throw new UnsupportedOperationException("Unsupported key type " + kty);
                }
            } catch (JsonParseException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }

    private static byte[] unsigned(byte[] arr) {
        if (arr.length == 0) return new byte[1];
        int zeros = 0;
        for (byte element : arr) {
            if (element == 0) zeros++;
            else break;
        }
        if (zeros == arr.length) zeros--;
        if (zeros == 0) return arr;
        byte[] res = new byte[arr.length - zeros];
        System.arraycopy(arr, zeros, res, 0, arr.length - zeros);
        return res;
    }

    private static <T> void ensureNoTrailingComma(T[] arr) {
        if (arr == null || arr.length == 0) return;
        if (arr[arr.length - 1] == null)
            throw new JsonParseException("While parsing JSON found array ending with null");
    }
}
