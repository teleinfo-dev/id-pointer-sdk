/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import cn.teleinfo.idpointer.sdk.core.trust.HandleRecordTrustVerifier;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class that resolves handles while verifying digital signatures on
 * those values. This provides a higher level of security because handle values
 * can be signed by private keys that are kept offline instead of on a handle
 * server.
 *
 * @deprecated Use {@link HandleRecordTrustVerifier} and allied classes
 */
@Deprecated
public class SecureResolver {
    public static final String SIGNED_INDEX_TAGNAME = "ofindex";
    public static final String SIG_ALG_TAGNAME = "alg";
    public static final String SIG_TAGNAME = "sig";
    public static final String DEFAULT_ALGORITHM = "DSA";
    public static final String VALUE_HASH_ELEMENT_NAME = "val";
    public static final String VALUE_INDEX_ATTRIBUTE = "index";
    public static final String SIG_HANDLE_ATTRIBUTE = "hdl";
    public static final String SIGNER_HANDLE_ATTRIBUTE = "signer";
    public static final String SIGNER_INDEX_ATTRIBUTE = "signerIndex";
    public static final String VALUE_MD5HASH_ATTRIBUTE = "md5";
    public static final String VALUE_SHA1HASH_ATTRIBUTE = "sha1";
    public static final int VALUE_DIGEST_OFFSET = Encoder.INT_SIZE * 2;

    public static final byte[] METADATA_TYPE = Util.encodeString("10320/sig.digest");
    public static final byte[] SIGNATURE_TYPE = Util.encodeString("10320/sig.sig");
    // the resolver to use when getting handle values
    private final HandleResolver resolver;

    // if true, trust keys that are provided by higher level namespaces
    private boolean trustNamespaceKeys = true;

    // the set of keys that are trusted by this resolver
    private volatile Map<ValueReference, PublicKey> trustedKeys = new HashMap<>();

    // If an unsigned value is encountered during secure resolution.
    // This applies to values that have no signatures - not values
    // that have invalid signatures.
    public boolean ignoreUnsignedValues = true;

    // reportMissingValues tells the resolver whether to throw an exception
    // if there are values in the digest but not in the resolution results.
    public boolean reportMissingValues = false;

    // If we encounter a broken signature then an exception is thrown
    // unless ignoreInvalidSignatures is true. The default is false.
    // Even if false, values with invalid signatures will be ignored.
    public boolean ignoreInvalidSignatures = false;

    public boolean traceMessages = false;

    /**
     * Construct a SecureResolver using a new instance of the default
     * HandleResolver to process resolution requests.
     */
    public SecureResolver() {
        this(new HandleResolver());
    }

    /**
     * Construct a SecureResolver using the given HandleResolver
     * to process resolution requests.
     */
    public SecureResolver(HandleResolver resolver) {
        this.resolver = resolver;
        setRootKeysAsTrusted();
    }

    public void printState() {
        // debugging stuff
        if (traceMessages) {
            System.err.println("trusted keys: " + trustedKeys);
            System.err.println("  trustNSKeys: " + trustNamespaceKeys);
            System.err.println("  ignoreUnsignedValues: " + ignoreUnsignedValues);
            System.err.println("  reportMissingValues: " + reportMissingValues);
            System.err.println("  ignoreInvalidSignatures: " + ignoreInvalidSignatures);
        }
    }

    public void setRootKeysAsTrusted() {
        Configuration conf = resolver.getConfiguration();
        Map<ValueReference, PublicKey> pubkeys = new HashMap<>();
        for (HandleValue rootVal : conf.getGlobalValues()) {
            if (rootVal.hasType(Common.STD_TYPE_HSPUBKEY)) {
                try {
                    pubkeys.put(new ValueReference(Common.ROOT_HANDLE, rootVal.getIndex()), Util.getPublicKeyFromBytes(rootVal.getData(), 0));
                } catch (Exception e) {
                    System.err.println("Warning: error parsing root service public key: " + e);
                }
            }
        }
        if (traceMessages) {
            System.err.println("putting trusted root keys: " + pubkeys);
        }
        setTrustedKeys(pubkeys);
    }

    /**
     * Specify the set of identities that are trusted to verify handle values.
     * The given map will include the trusted identifiers as the keys and their
     * associated public keys as the values.
     */
    public void setTrustedKeys(Map<ValueReference, PublicKey> keyIDsMap) {
        Map<ValueReference, PublicKey> newMap = new HashMap<>();
        for (Map.Entry<ValueReference, PublicKey> entry : keyIDsMap.entrySet()) {
            newMap.put(new ValueReference(entry.getKey().handle.clone(), entry.getKey().index), entry.getValue());
        }
        this.trustedKeys = newMap;
    }

    public PublicKey getTrustedKey(ValueReference valRef) {
        return trustedKeys.get(valRef);
    }

    /**
     * Sets whether or not the resolver should trust keys that are provided on
     * prefix handles.  These prefixes handle records themselves must
     * themselves be signed by the root keys. Any levels of indirection that
     * might occur can also be accompanied by another level of keys to which
     * trust can be delegated.
     */
    public void setTrustNamespaceKeys(boolean trustThem) {
        this.trustNamespaceKeys = trustThem;
    }

    /**
     * Resolve the given handle retrieving only the given types and indexes,
     * if any. This will verify that any values returned are signed according
     * to the policy of this object.
     */
    public HandleValue[] resolveHandle(byte handle[], byte types[][], int indexes[]) throws HandleException {
        return resolveHandle(new ResolutionIdRequest(handle, types, indexes, null));
    }

    /**
     * Process the given ResolutionRequest while verifying that any values returned are
     * signed according to the policy of this object.
     */
    public HandleValue[] resolveHandle(ResolutionIdRequest req) throws HandleException {
        // if any types or indexes were requested, add the metadata and signature
        // types to them
        byte[][] types = req.requestedTypes;
        byte[][] origTypes = types;
        int[] indexes = req.requestedIndexes;
        if ((types != null && types.length > 0) || (indexes != null && indexes.length > 0)) {
            // need to add the metadata and signature types to the query...
            if (types == null) {
                // there were no types requested, but there were indexes requested,
                // so we need to add the requested types to make sure they are included
                // in the response
                types = new byte[][] { METADATA_TYPE, SIGNATURE_TYPE };
            } else {
                byte newTypes[][] = new byte[2 + types.length][];
                newTypes[0] = METADATA_TYPE;
                newTypes[1] = SIGNATURE_TYPE;
                System.arraycopy(types, 0, newTypes, 2, types.length);
                types = newTypes;
            }
            req.requestedTypes = types;
            req.requestedIndexes = indexes;
            req.clearBuffers();
        }

        // perform a normal resolution...
        HandleValue values[] = null;
        AbstractIdResponse resp = resolver.processRequest(req);
        if (resp instanceof ResolutionIdResponse) {
            values = ((ResolutionIdResponse) resp).getHandleValues();
        } else {
            if (resp.responseCode == AbstractMessage.RC_HANDLE_NOT_FOUND) {
                throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST);
            } else if (resp instanceof ErrorIdResponse) {
                String msg = Util.decodeString(((ErrorIdResponse) resp).message);
                throw new HandleException(HandleException.INTERNAL_ERROR, "Error(" + resp.responseCode + "): " + msg);
            } else {
                throw new HandleException(HandleException.INTERNAL_ERROR, "Unknown response: " + resp);
            }
        }

        // If there are no signatures or value metadata then we can't verify the handle
        int numSigs = 0;
        int numMetadata = 0;
        for (int i = 0; values != null && i < values.length; i++) {
            if (values[i] == null) {
                continue;
            }
            if (values[i].hasType(SIGNATURE_TYPE)) {
                numSigs++;
            }
            if (values[i].hasType(METADATA_TYPE)) {
                numMetadata++;
            }
        }
        if (numSigs == 0 && numMetadata == 0) {
            if (ignoreUnsignedValues) {
                return new HandleValue[0];
            } else {
                throw new HandleException(HandleException.INVALID_VALUE, "No signatures found in " + Util.decodeString(req.handle));
            }
        }

        try {
            Map<ValueReference, PublicKey> keys = this.trustedKeys;
            if (this.trustNamespaceKeys) {
                // if we trust any namespace keys, then we need to resolve the namespace keys
                Map<ValueReference, PublicKey> localKeys = new HashMap<>();
                localKeys.putAll(this.trustedKeys);
                addKeysFromHandle(Util.getZeroNAHandle(req.handle), localKeys);
                keys = localKeys;
            }

            // we now have the trusted keys in the localKeys map and can verify the handle contents
            // based on those keys
            return secureHandleValues(req.handle, values, keys, origTypes, indexes);
        } catch (Exception e) {
            if (e instanceof HandleException) {
                throw (HandleException) e;
            } else {
                throw new HandleException(HandleException.ENCRYPTION_ERROR, "Error verifying signature: " + e);
            }
        }
    }

    public HandleValue[] secureHandleValues(byte handle[], HandleValue[] values) throws Exception {
        return secureHandleValues(handle, values, this.trustedKeys);
    }

    public HandleValue[] secureHandleValues(byte handle[], HandleValue[] aValues, Map<ValueReference, PublicKey> keys) throws Exception {
        return secureHandleValues(handle, aValues, keys, null, null);
    }

    private HandleValue[] secureHandleValues(byte handle[], HandleValue[] aValues, Map<ValueReference, PublicKey> keys, byte[][] types, int[] indexes) throws Exception {
        HandleValue[] values = new HandleValue[aValues.length];
        System.arraycopy(aValues, 0, values, 0, aValues.length);

        List<HandleSignature> sigList = HandleSignature.getSignatures(values, !ignoreInvalidSignatures);

        // throw out the invalid or untrusted signatures
        Iterator<HandleSignature> sigIter = sigList.iterator();
        while (sigIter.hasNext()) {
            HandleSignature sig = sigIter.next();
            PublicKey signerKey = keys.get(sig.getSigner());
            if (signerKey == null) {
                if (traceMessages || resolver.traceMessages) {
                    System.err.println("ignoring signature: " + sig.getSigner());
                }
                sigIter.remove();
                continue;
            }
            if (!Util.equalsPrefixCI(handle, Util.encodeString(sig.getHandle()))) {
                if (ignoreInvalidSignatures) {
                    sigIter.remove();
                    continue;
                } else {
                    throw new HandleException(HandleException.ENCRYPTION_ERROR, "Signature for wrong handle encountered: " + sig);
                }
            }
            if (!sig.verifySignature(signerKey)) {
                // we have keys for this signer, but the signature was not valid
                if (traceMessages || resolver.traceMessages) {
                    System.err.println("verify-signature failed: " + sig);
                }
                if (ignoreInvalidSignatures) {
                    sigIter.remove();
                    continue;
                } else {
                    throw new HandleException(HandleException.ENCRYPTION_ERROR, "Invalid signature encountered: " + sig);
                }
            }
            if (reportMissingValues && sig.signsMissingValues(values)) {
                throw new HandleException(HandleException.ENCRYPTION_ERROR, "Signature signs missing values: " + sig);
            }
        }

        // Now go through the values and remove all but the ones that are signed.
        // If the policy is to throw an error if any unsigned values are found
        // (except for the signatures themselves) then we throw an error
        int numVals = 0;
        for (int i = 0; i < values.length; i++) {
            HandleValue value = values[i];
            if (value == null) {
                continue;
            }
            if (!valueNeedsSignature(value)) {
                numVals++;
                continue;
            }
            boolean isSigned = false;
            String handleString = Util.decodeString(handle);
            for (HandleSignature sig : sigList) {
                if (sig.verifyValue(handleString, value)) {
                    // the signature was valid, we don't need to check any more signatures
                    isSigned = true;
                    break;
                }
            }
            if (isSigned) {
                numVals++;
                continue;
            }
            if (ignoreUnsignedValues) {
                values[i] = null;
            } else {
                throw new HandleException(HandleException.ENCRYPTION_ERROR, "Encountered unsigned value: " + value);
            }
        }
        if ((types != null && types.length > 0) || (indexes != null && indexes.length > 0)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) continue;
                if ((types != null && types.length > 0 && Util.isParentTypeInArray(types, values[i].type)) || (indexes != null && indexes.length > 0 && Util.isInArray(indexes, values[i].index))) continue;
                values[i] = null;
                numVals--;
            }
        }
        HandleValue returnVals[] = new HandleValue[numVals];
        for (int i = values.length - 1; i >= 0; i--) {
            if (values[i] != null) {
                returnVals[--numVals] = values[i];
            }
        }
        return returnVals;
    }

    /**
     * Adds keys from the given handle if the handle does not already have any keys for it.
     * Any handles that are resolved are also verified before adding them to the trusted
     * key map.
     */
    private void addKeysFromHandle(byte[] trustedHandle, @SuppressWarnings("hiding") Map<ValueReference, PublicKey> trustedKeys) throws Exception {
        HandleValue values[] = null;
        if (Util.equalsCI(trustedHandle, Common.ROOT_HANDLE)) {
            // the root values....
            values = resolver.getConfiguration().getGlobalValues();
        } else {
            values = resolveHandle(trustedHandle, new byte[][] { Common.STD_TYPE_HSPUBKEY }, null);
        }

        for (int i = 0; values != null && i < values.length; i++) {
            if (values[i].hasType(Common.STD_TYPE_HSPUBKEY)) {
                try {
                    trustedKeys.put(new ValueReference(trustedHandle, values[i].getIndex()), Util.getPublicKeyFromBytes(values[i].getData(), 0));
                } catch (Exception e) {
                    System.err.println("Error loading namespace key: " + e);
                }
            }
        }
    }

    public static void main(String argv[]) throws Exception {
        if (argv.length <= 0) {
            System.err.println("usage: java SecureResolver <handle1> [<handle2>...]");
            return;
        }
        SecureResolver r = new SecureResolver();
        for (String hdl : argv) {
            System.out.println("Resolving " + hdl);
            HandleValue values[] = r.resolveHandle(Util.encodeString(hdl), null, null);
            for (HandleValue val : values) {
                System.out.println("  " + val);
            }
        }

    }

    public static boolean valueNeedsSignature(HandleValue value) {
        if (value.hasType(SIGNATURE_TYPE) || value.hasType(METADATA_TYPE) || !value.publicRead) {
            return false;
        } else {
            return true;
        }

    }
}
