/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;


import cn.teleinfo.idpointer.sdk.core.*;

import java.util.*;

public class ChainBuilder {
    private static final int MAX_CHAIN_LENGTH = 50;

    private Map<String, HandleRecord> handleMap;
    private HandleResolverInterface resolver;
    private HandleStorage storage;

    private final JsonWebSignatureFactory signatureFactory = JsonWebSignatureFactory.getInstance();
    private final HandleVerifier handleVerifier = new HandleVerifier();

    public ChainBuilder(Map<String, HandleRecord> handleMap, HandleResolverInterface resolver) {
        this.handleMap = handleMap;
        fixHandleMapCase();
        this.resolver = resolver;
    }

    public ChainBuilder(HandleResolverInterface resolver) {
        this.resolver = resolver;
    }

    public ChainBuilder(HandleStorage storage) {
        this.storage = storage;
    }

    public ChainBuilder(HandleStorage storage, HandleResolver resolver) {
        this.storage = storage;
        this.resolver = resolver;
    }

    private void fixHandleMapCase() {
        Map<String, HandleRecord> newEntries = null;
        for (Map.Entry<String, HandleRecord> entry : handleMap.entrySet()) {
            String key = entry.getKey();
            String upperCaseKey = Util.upperCasePrefix(key);
            if (!key.equals(upperCaseKey)) {
                if (newEntries == null) newEntries = new HashMap<>();
                newEntries.put(upperCaseKey, entry.getValue());
            }
        }
        if (newEntries != null) handleMap.putAll(newEntries);
    }

    public List<IssuedSignature> buildChain(JsonWebSignature childSignature) throws TrustException {
        List<IssuedSignature> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        List<String> chain = null;
        while (true) {
            HandleClaimsSet childClaims = handleVerifier.getHandleClaimsSet(childSignature);
            if (childClaims == null) throw new TrustException("signature payload not valid");
            if (childClaims.isSelfIssued()) {
                IssuedSignature issuedSignature = new IssuedSignature(childSignature, childClaims.publicKey, childClaims.perms);
                result.add(issuedSignature);
                break; //If we reach a self signed cert the chain is complete.
            }
            if (result.size() >= MAX_CHAIN_LENGTH) throw new TrustException("chain too long");
            boolean noChain = false;
            if (chain == null || chain.isEmpty()) {
                chain = childClaims.chain;
                if (chain == null || chain.isEmpty()) {
                    noChain = true;
                    String handleOfIssuer = ValueReference.fromString(childClaims.iss).getHandleAsString();
                    chain = Collections.singletonList(handleOfIssuer);
                }
            }
            String nextLinkInChain = chain.get(0);
            if (seenIds.contains(nextLinkInChain)) throw new TrustException("cycle in chain");
            else seenIds.add(nextLinkInChain);
            String parentSignatureString;
            try {
                parentSignatureString = lookup(nextLinkInChain, childClaims.iss);
            } catch (HandleException e) {
                throw new TrustException("handle resolution exception", e);
            }
            if (parentSignatureString == null) {
                if (noChain) throw new TrustException("no chain and unable to resolve issuer " + nextLinkInChain);
                throw new TrustException("unable to resolve chain " + nextLinkInChain);
            }
            JsonWebSignature parentSignature;
            try {
                parentSignature = signatureFactory.deserialize(parentSignatureString);
            } catch (TrustException e) {
                if (noChain) throw new TrustException("no chain and not a signature at issuer " + nextLinkInChain);
                throw new TrustException("not a signature at chain " + nextLinkInChain);
            }
            HandleClaimsSet parentClaims = handleVerifier.getHandleClaimsSet(parentSignature);
            if (parentClaims == null) throw new TrustException("signature payload not valid");
            if (!Util.equalsPrefixCI(parentClaims.sub, childClaims.iss)) throw new TrustException("chain is broken");
            IssuedSignature issuedSignature = new IssuedSignature(childSignature, parentClaims.publicKey, parentClaims.perms);
            result.add(issuedSignature);
            childSignature = parentSignature;
            chain = chain.subList(1, chain.size()); //chain = tail of chain.
        }
        return result;
    }

    public HandleValue resolveValueReference(ValueReference valueReference) throws HandleException {
        if (handleMap != null) {
            return handleMapLookup(valueReference);
        }
        if (storage != null) {
            return storageLookup(valueReference);
        }
        if (resolver != null) {
            return resolver.resolveValueReference(valueReference);
        }
        return null;
    }

    private String lookup(String nextLinkInChain, String subject) throws HandleException {
        ValueReference valueReference = ValueReference.fromString(nextLinkInChain);
        if (valueReference.index > 0) {
            HandleValue value = resolveValueReference(valueReference);
            if (value == null) return null;
            else return value.getDataAsString();
        } else {
            List<HandleValue> values = null;
            if (handleMap != null) {
                HandleRecord record = handleMap.get(Util.upperCasePrefix(valueReference.getHandleAsString()));
                if (record != null) values = record.getValues();
            }
            if (values == null && storage != null) {
                values = storageLookup(valueReference.handle);
            }
            if (values == null) {
                if (resolver != null) {
                    values = Arrays.asList(resolver.resolveHandle(valueReference.handle));
                }
            }
            if (values == null) return null;
            JsonWebSignature latestCert = getLatestHsCertAboutSubject(subject, values);
            if (latestCert == null) return null;
            return latestCert.serialize();
        }
    }

    private HandleValue storageLookup(ValueReference valueReference) throws HandleException {
        byte[][] handleValuesBytes = storage.getRawHandleValues(Util.upperCasePrefix(valueReference.handle), new int[] { valueReference.index }, null);
        if (handleValuesBytes == null || handleValuesBytes.length == 0) return null;
        HandleValue[] handleValues = Encoder.decodeHandleValues(handleValuesBytes);
        return handleValues[0];
    }

    private List<HandleValue> storageLookup(byte[] handle) throws HandleException {
        byte[][] handleValuesBytes = storage.getRawHandleValues(Util.upperCasePrefix(handle), null, null);
        if (handleValuesBytes == null || handleValuesBytes.length == 0) return null;
        HandleValue[] handleValues = Encoder.decodeHandleValues(handleValuesBytes);
        return Arrays.asList(handleValues);
    }

    private HandleValue handleMapLookup(ValueReference valueReference) {
        HandleRecord record = handleMap.get(Util.upperCasePrefix(valueReference.getHandleAsString()));
        if (record == null) return null;
        return record.getValueAtIndex(valueReference.index);
    }

    JsonWebSignature getLatestHsCertAboutSubject(String subject, List<HandleValue> values) throws TrustException {
        JsonWebSignature latestCertAboutSubject = null;
        HandleClaimsSet latestCertClaimsSet = null;
        for (HandleValue value : values) {
            if (value.hasType(Common.HS_CERT_TYPE)) {
                String signatureString = value.getDataAsString();
                JsonWebSignature signature = signatureFactory.deserialize(signatureString);
                HandleClaimsSet claimsSet = handleVerifier.getHandleClaimsSet(signature);
                if (subject.equals(claimsSet.sub)) {
                    if (latestCertAboutSubject == null) {
                        latestCertAboutSubject = signature;
                        latestCertClaimsSet = claimsSet;
                    } else if (issuedLater(claimsSet, latestCertClaimsSet)) {
                        latestCertAboutSubject = signature;
                        latestCertClaimsSet = claimsSet;
                    }
                }
            }
        }
        return latestCertAboutSubject;
    }

    private static boolean issuedLater(HandleClaimsSet claims1, HandleClaimsSet claims2) {
        if (claims1.iat == null) return false;
        if (claims2.iat == null) return true;
        return claims1.iat > claims2.iat;
    }
}
