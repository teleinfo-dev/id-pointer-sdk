/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.stream.xml.XParser;
import cn.teleinfo.idpointer.sdk.core.stream.xml.XTag;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used for high level interaction with handles.  Configuration
 * information for this resolver is located in the .handle/resolver.xml file
 * under the home directory of the user running this code as determined by the
 * user.home system property.
 *
 * The following is an example of an override in the resolver.xml file:
 * <pre>{@code
 * <hsconfig>
 *  <local_handles>
 *   <handle handle="200/0"
 *     case_sensitive="false"
 *     override_type="on_failure"  <!-- could also be "always" - determines when the override is applied -->
 *     >
 *    <hdlvalue
 *      type="URL"  <!-- default is empty -->
 *      admin_read="true"
 *      admin_write="true"
 *      public_read="true"
 *      public_write="false"
 *      ttl="86400"
 *      ttl_type="relative" <!-- could also be "absolute" -->
 *      encoding="text"  <!-- encoding for data value; could also be "hex" -->
 *      >
 *      http://www.handle.net/
 *    </hdlvalue>
 *   </handle>
 *  </local_handles>
 * </hsconfig>
 * }</pre>
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Resolver {
    private static final String[] PUBKEY_TYPES = { "HS_PUBKEY" };

    private final File configDir;
    private final File configFile;
    private long configTimestamp = -1;

    private HashMap localHandles = new HashMap();
    private HashMap localHandlesCI = new HashMap();

    private final HandleResolver resolver = new HandleResolver();

    private boolean secureMessages = false;
    private final boolean authoritativeMessages = false;
    private final AuthenticationInfo authenticationInfo = null;

    /** Construct a Resolver object with the default settings */
    public Resolver() {
        // the configure file is "resolver.xml" in the users .handle directory
        configDir = new File(System.getProperty("user.home", "."), ".handle");
        configFile = new File(configDir, "resolver.xml");

        loadConfiguration();
    }

    /** Return the file that contains the XML resolver configuration */
    public File getConfigFile() {
        return configFile;
    }

    /** Check if the configuration file has been modified, and if so, reload it */
    public void checkConfiguration() {
        try {
            if (!configDir.exists()) return;
            if (!configFile.exists()) return;
            if (configTimestamp == -1 || configFile.lastModified() > configTimestamp) {
                loadConfiguration();
            }
        } catch (Exception e) {
            System.err.println("Error checking config file timestamp: " + e);
        }
    }

    /** (re)read the configuration data from the ~/.handle/resolver.xml file */
    private void loadConfiguration() {
        XTag config = null;
        @SuppressWarnings("hiding")
        HashMap localHandles = new HashMap();
        @SuppressWarnings("hiding")
        HashMap localHandlesCI = new HashMap();

        try {
            // load the overall client configuration from the user config file
            if (configFile.exists() && configFile.canRead()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(configFile), "UTF-8")) {
                    config = new XParser().parse(in, false);
                }

                // Read any local handles from the config file that
                // should bypass any direct network resolution
                XTag localHdlsTag = config.getSubTag("local_handles");
                if (localHdlsTag != null) {
                    for (int i = 0; i < localHdlsTag.getSubTagCount(); i++) {
                        XTag localHdlTag = localHdlsTag.getSubTag(i);
                        if (!localHdlTag.getName().equalsIgnoreCase("handle")) continue;
                        String handle = localHdlTag.getAttribute("handle");
                        if (handle == null || handle.trim().length() <= 0 || handle.indexOf('/') < 0) {
                            System.err.println("Invalid override handle: '" + handle + "' in " + localHdlTag);
                            continue;
                        }
                        ArrayList valueList = new ArrayList();
                        boolean caseSensitive = localHdlTag.getBoolAttribute("case_sensitive", true);
                        String overrideTypeStr = localHdlTag.getAttribute("override_type", "always");
                        int overrideType = overrideTypeStr.equalsIgnoreCase("on_failure") ? ValueList.OVERRIDE_ON_FAILURE : ValueList.OVERRIDE_ALWAYS;
                        for (int j = 0; j < localHdlTag.getSubTagCount(); j++) {
                            XTag hdlValTag = localHdlTag.getSubTag(j);
                            if (!hdlValTag.getName().equalsIgnoreCase("hdlvalue")) continue;
                            valueList.add(getValueFromXML(hdlValTag));
                        }

                        ValueList hdlInfo = new ValueList(overrideType, (HandleValue[]) valueList.toArray(new HandleValue[valueList.size()]));

                        if (caseSensitive) {
                            localHandlesCI.put(handle.toLowerCase(), hdlInfo);
                        } else {
                            localHandles.put(handle, hdlInfo);
                        }
                    }
                }
                configTimestamp = configFile.lastModified();
            }
        } catch (Exception e) {
            System.err.println("Unable to load client config: " + e);
        }
        if (config == null) config = new XTag("hsconfig");

        this.localHandles = localHandles;
        this.localHandlesCI = localHandlesCI;
    }

    /** Resolve the given handle  */
    public HandleValue[] resolveHandle(String handle) throws HandleException {
        return resolveHandle(handle, null);
    }

    /** Resolve the given handle to its values that have the given types. */
    public HandleValue[] resolveHandle(String handle, String typeList[]) throws HandleException {
        return resolveHandle(handle, typeList, null, false);
    }

    /** Resolve the given handle to its values that have the given types.  If the
     * secure value is true then ensure the resolution can be trusted using
     * digital signatures. */
    public HandleValue[] resolveHandle(String handle, String typeList[], boolean secure) throws HandleException {
        return resolveHandle(handle, typeList, null, secure);
    }

    /** Resolve the given handle to its values that have the given types or indexes.
     * If the secure value is true then ensure the resolution can be trusted using
     * digital signatures. */
    public HandleValue[] resolveHandle(String handle, String typeList[], int indexes[], boolean secure) throws HandleException {
        checkConfiguration();

        byte types[][] = convertTypes(typeList);
        ValueList valList = (ValueList) localHandles.get(handle);
        if (valList == null) valList = (ValueList) localHandlesCI.get(handle.toLowerCase());
        if (valList != null && valList.getOverrideType() == ValueList.OVERRIDE_ALWAYS) {
            return filterValues(valList.getValues(), types);
        }

        try {
            ResolutionRequest req = new ResolutionRequest(Util.encodeString(handle), types, indexes, authenticationInfo);
            assignProperties(req);

            if (secure) {
                req.certify = true;
            }

            AbstractResponse response = resolver.processRequest(req);
            verifyResponse(req, response);
            if (response.responseCode == AbstractMessage.RC_SUCCESS) return ((ResolutionResponse) response).getHandleValues();
            if (response.responseCode == AbstractMessage.RC_HANDLE_NOT_FOUND) {
                throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST, "Handle: '" + handle + "' was not found");
            }

            if (response instanceof ErrorResponse) {
                ErrorResponse eResponse = (ErrorResponse) response;
                String msg = Util.decodeString(eResponse.message);
                throw new HandleException(HandleException.INTERNAL_ERROR, AbstractMessage.getResponseCodeMessage(response.responseCode) + ": " + msg);
            }

            throw new HandleException(HandleException.INTERNAL_ERROR, AbstractMessage.getResponseCodeMessage(response.responseCode));
        } catch (HandleException e) {
            // there was an error resolving - use the override if one is available
            if (valList != null) return filterValues(valList.getValues(), types);
            throw e;
        }
    }

    /** Set whether or not messages sent through this resolver will require digital
     * signatures on all responses. */
    public void setVerifyMessages(boolean verify) {
        this.secureMessages = verify;
    }

    /** Get the underlying resolver being used */
    public HandleResolver getResolver() {
        return resolver;
    }

    public PublicKey[] resolvePublicKeys(String handle) throws HandleException {
        HandleValue values[] = resolveHandle(handle, PUBKEY_TYPES, true);

        ArrayList keys = new ArrayList();
        // decode the public keys
        for (int i = values.length - 1; i >= 0; i--) {
            if (!values[i].hasType(Common.STD_TYPE_HSPUBKEY)) continue;
            try {
                PublicKey pubkey = Util.getPublicKeyFromBytes(values[i].getData(), 0);
                if (pubkey != null) keys.add(pubkey);
            } catch (Exception e) {
                System.err.println("Error decoding public key from value: " + values[i] + "; error: " + e);
            }
        }
        return (PublicKey[]) keys.toArray(new PublicKey[keys.size()]);
    }

    /** Set the properties of the given request according to the */
    private void assignProperties(AbstractRequest req) {
        if (secureMessages) req.certify = true;
        if (authoritativeMessages) req.authoritative = true;
        req.authInfo = authenticationInfo;
    }

    /** */
    @SuppressWarnings("unused")
    private void verifyResponse(AbstractRequest req, AbstractResponse response) throws HandleException {
        //;
    }

    private byte[][] convertTypes(String filterTypes[]) {
        if (filterTypes == null) return null;
        byte types[][] = new byte[filterTypes.length][];
        for (int i = filterTypes.length - 1; i >= 0; i--)
            types[i] = Util.encodeString(filterTypes[i]);
        return types;
    }

    /** Filter the given handle values into an array containing only values with
     * the given types (or subtypes thereof) */
    private HandleValue[] filterValues(HandleValue values[], byte types[][]) {
        if (values == null || values.length == 0 || types == null || types.length == 0) return values;

        ArrayList valueList = new ArrayList();
        for (int i = values.length - 1; i >= 0; i--) {
            if (values[i] == null) continue;
            for (int t = types.length - 1; t >= 0; t--) {
                if (values[i].hasType(types[t])) {
                    valueList.add(values[i]);
                    break;
                }
            }
        }

        return (HandleValue[]) valueList.toArray(new HandleValue[valueList.size()]);
    }

    public XTag getXMLForValue(HandleValue value) {
        if (value == null) return null;
        XTag valTag = new XTag("hdlvalue");
        valTag.setAttribute("type", value.getTypeAsString());
        valTag.setAttribute("admin_read", value.adminRead);
        valTag.setAttribute("admin_write", value.adminWrite);
        valTag.setAttribute("public_read", value.publicRead);
        valTag.setAttribute("public_write", value.publicWrite);
        valTag.setAttribute("ttl", value.getTTL());
        valTag.setAttribute("ttl_type", value.getTTLType() == HandleValue.TTL_TYPE_ABSOLUTE ? "absolute" : "relative");
        byte data[] = value.getData();
        if (Util.looksLikeBinary(data)) {
            valTag.setAttribute("encoding", "hex");
            valTag.setValue(Util.decodeHexString(data, false));
        } else {
            valTag.setAttribute("encoding", "text");
            valTag.setValue(Util.decodeString(data));
        }
        return valTag;
    }

    private static HandleValue getValueFromXML(XTag hdlValTag) throws Exception {
        HandleValue val = new HandleValue();
        val.setType(Util.encodeString(hdlValTag.getAttribute("type", "")));
        val.adminRead = hdlValTag.getBoolAttribute("admin_read", true);
        val.adminWrite = hdlValTag.getBoolAttribute("admin_write", true);
        val.publicRead = hdlValTag.getBoolAttribute("public_read", true);
        val.publicWrite = hdlValTag.getBoolAttribute("public_write", false);
        val.ttl = hdlValTag.getIntAttribute("ttl", 86400);
        val.ttlType = HandleValue.TTL_TYPE_RELATIVE;
        if (hdlValTag.getAttribute("ttl_type", "relative").equalsIgnoreCase("absolute")) val.ttlType = HandleValue.TTL_TYPE_ABSOLUTE;

        String dataEnc = hdlValTag.getAttribute("encoding", "text");
        val.setData(getValueWithEncoding(hdlValTag.getStrValue(), dataEnc));
        return val;
    }

    private static final byte[] getValueWithEncoding(String str, String encoding) throws Exception {
        encoding = encoding.toLowerCase();
        if (encoding.equals("text")) {
            return Util.encodeString(str);
        } else if (encoding.equals("hex")) {
            return Util.encodeHexString(str);
        }
        throw new Exception("Unrecognized encoding: " + encoding + " for data: " + str);
    }

    /** Verify that the given authentication object is valid and checks out
     * when tested via handle resolution.  This performs the same verification
     * procedure as a server in order to verify that the entity that is authenticating
     * with the given object is who they claim to be. */
    public boolean checkAuthentication(AuthenticationInfo authInfo) throws Exception {
        ResolutionRequest request = new ResolutionRequest(Common.BLANK_HANDLE, null, null, null);
        ChallengeResponse challengeResp = new ChallengeResponse(request, true);

        byte authBytes[] = authInfo.authenticate(challengeResp, request);
        if (Util.equals(authInfo.getAuthType(), Common.SECRET_KEY_TYPE)) {
            // Secret key authentication
            return verifySecretKeyAuth(authInfo, challengeResp, authBytes);

        } else if (Util.equals(authInfo.getAuthType(), Common.PUBLIC_KEY_TYPE)) {
            // Public key authentication
            return verifyPubKeyAuth(authInfo, challengeResp, authBytes);

        } else {
            // Unknown authentication type
            throw new HandleException(HandleException.UNABLE_TO_AUTHENTICATE, "Unknown authentication type: " + Util.decodeString(authInfo.getAuthType()));
        }
    }

    /**
     * Verify that the given secret key-based ChallengeResponse was actually
     * 'signed' by the given AuthenticationInfo object.
     */
    private boolean verifySecretKeyAuth(AuthenticationInfo authInfo, ChallengeResponse challengeResp, byte[] authBytes) throws HandleException {
        VerifyAuthRequest verifyAuthReq = new VerifyAuthRequest(authInfo.getUserIdHandle(), challengeResp.nonce, challengeResp.requestDigest, challengeResp.rdHashType, authBytes, authInfo.getUserIdIndex(), null);
        verifyAuthReq.certify = true;

        AbstractResponse response = resolver.processRequest(verifyAuthReq);

        // make sure we got a VerifyAuthResponse
        if (response instanceof VerifyAuthResponse) {
            return ((VerifyAuthResponse) response).isValid;
        } else {
            throw new HandleException(HandleException.UNABLE_TO_AUTHENTICATE, "Unable to verify authentication\n" + response);
        }
    }

    /**
     * Verify that the given public key-based ChallengeResponse was actually
     * signed by the given AuthenticationInfo object's private key.
     */
    private boolean verifyPubKeyAuth(AuthenticationInfo authInfo, ChallengeResponse challengeResp, byte[] authBytes) throws Exception {
        // first retrieve the public key (checking server signatures)
        ResolutionRequest request = new ResolutionRequest(authInfo.getUserIdHandle(), authInfo.getUserIdIndex() > 0 ? null : Common.PUBLIC_KEY_TYPES, authInfo.getUserIdIndex() > 0 ? new int[] { authInfo.getUserIdIndex() } : null, null);
        request.certify = true;
        AbstractResponse response = resolver.processRequest(request);

        // make sure we got a ResolutionResponse
        if (!(response instanceof ResolutionResponse)) throw new HandleException(HandleException.UNABLE_TO_AUTHENTICATE, "Unable to verify authentication\n" + response);

        Map<Integer, byte[]> indexToBytesMap = new HashMap<>();
        // make sure we got the handle values
        HandleValue values[] = ((ResolutionResponse) response).getHandleValues();
        for (int i = 0; values != null && i < values.length; i++) {
            if (values[i] != null && (authInfo.getUserIdIndex() == 0 || values[i].getIndex() == authInfo.getUserIdIndex())) {
                indexToBytesMap.put(Integer.valueOf(values[i].getIndex()), values[i].getData());
                break;
            }
        }

        if (indexToBytesMap.isEmpty()) {
            throw new HandleException(HandleException.UNABLE_TO_AUTHENTICATE, "The admin index specified (" + authInfo.getUserIdHandle() + ") does not exist");
        }

        // get the algorithm used to sign
        int offset = 0;
        byte hashAlgId[] = Encoder.readByteArray(authBytes, offset);
        offset += Encoder.INT_SIZE + hashAlgId.length;

        // get the actual bytes of the signature
        byte sigBytes[] = Encoder.readByteArray(authBytes, offset);
        offset += Encoder.INT_SIZE + sigBytes.length;

        // decode the public key
        for (byte[] pkBytes : indexToBytesMap.values()) {
            PublicKey pubKey = Util.getPublicKeyFromBytes(pkBytes, 0);

            if (pubKey instanceof DSAPublicKey) {
                if (verifyDSAPublicKey(hashAlgId, pubKey, challengeResp, sigBytes)) return true;
            } else if (pubKey instanceof RSAPublicKey) {
                if (verifyRSAPublicKeyImpl(hashAlgId, pubKey, challengeResp, sigBytes)) return true;
            } else {
                //            throw new HandleException(HandleException.UNABLE_TO_AUTHENTICATE,
                //                    "Unrecognized key type: "+pubKey);
            }
        }
        return false;
    }

    /**
     * Verify that the given ChallengeResponse was signed by the given DSA PublicKey.
     */
    private boolean verifyDSAPublicKey(byte[] hashAlgId, PublicKey pubKey, ChallengeResponse challengeResp, byte[] sigBytes) throws Exception {
        // load the signature
        String sigId = Util.getSigIdFromHashAlgId(hashAlgId, pubKey.getAlgorithm());
        Signature sig = Signature.getInstance(sigId);
        sig.initVerify(pubKey);

        // verify the signature
        sig.update(challengeResp.nonce);
        sig.update(challengeResp.requestDigest);
        return sig.verify(sigBytes);
    }

    /**
     * Verify that the given ChallengeResponse was signed by the given RSA PublicKey.
     */
    private boolean verifyRSAPublicKeyImpl(byte[] hashAlgId, PublicKey pubKey, ChallengeResponse challengeResp, byte[] sigBytes) throws Exception {
        // load the signature
        String sigId = Util.getSigIdFromHashAlgId(hashAlgId, pubKey.getAlgorithm());
        Signature sig = Signature.getInstance(sigId);
        sig.initVerify(pubKey);

        // verify the signature
        sig.update(challengeResp.nonce);
        sig.update(challengeResp.requestDigest);
        return sig.verify(sigBytes);
    }

    /** Internal class to contain locally overridden handles and their values */
    private class ValueList {
        public static final int OVERRIDE_ALWAYS = 0;
        public static final int OVERRIDE_ON_FAILURE = 1;

        private int overrideType = OVERRIDE_ALWAYS;

        private final HandleValue values[];

        ValueList(int overrideType, HandleValue values[]) {
            this.values = values;
            this.overrideType = overrideType;
        }

        public int getOverrideType() {
            return overrideType;
        }

        public HandleValue[] getValues() {
            return values;
        }
    }

}
