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
import cn.teleinfo.idpointer.sdk.core.trust.JsonWebSignature;

import java.io.StringReader;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A signature over some handle values.  The form of the signature is two handle values, one with type 10320/sig.digest containing a digest of all the signed values,
 * and a second of type 10320/sig.sig containing a signature of the digest value.  Example data:
 * <pre>{@code
 * <digests hdl="0.NA/0.NA">
 * <val index="200" md5="908E0C8CB00955EC3D09FC2B0F0C69E2" sha1="5389F007F1450528015F7C1D8EBAE5A488FE954F"/>
 * <val index="100" md5="9D2A7E852A44A15C4427D731E3606A24" sha1="233E5EBE829E12EF82F23E4770F2791C7F59038F"/>
 * <val index="4" md5="7FFFA079E968352EDABB395A47620EA9" sha1="ED8106C0D89A60BD5AFDAC19730557938DE4E2ED"/>
 * <val index="3" md5="16B94473F4B84157182AEA3CB628D9FB" sha1="AAF878B1A4BB8EECCAD0F13A61C960C6009D53F0"/>
 * <val index="5" md5="2080ED829ECE843126DF65EEA68D920E" sha1="98B8FA558ADC3D256C741B229517AD5D7949AFD5"/>
 * <val index="6" md5="4F6F689F296B3DC6D28F012BC8773C9F" sha1="B2FCAD94DB174F2F179987F35D51B1AB5CB4BFCC"/>
 * <val index="101" md5="EA3474B36F7112F5551117429920533E" sha1="D8FCB133C7A211B3FA8C5627B3F48F97333BDE7C"/>
 * <val index="102" md5="EEBE40475DDED108CAA8AD9A3F66014B" sha1="0514AF918AB38775CA7F5A6CE95553CB29FE9480"/>
 * <val index="300" md5="0C9063ABA3D52F97FDE6BC3F88478A34" sha1="1B58B607566A0BCEF26CFBFF03384F47653B2D4C"/>
 * <val index="2" md5="02A5A450B5152E1CEE7668DFB63BC2BB" sha1="B20086A5702ED182551E26620C2EDAA048C57B98"/>
 * <val index="1" md5="355E6A68668771D0D13DE1851D865E6D" sha1="2CF199CF5A756EB25FA8803B4CB2B9C7FE6C33CD"/>
 * <val index="7" md5="9CEAAE6CC1B0CBA28BEAA48E54545ECF" sha1="CCEF7E44A4082F4AB285B058C941841A1456CA3E"/>
 * </digests>
 * }</pre>
 * and
 * <pre>{@code
 * <signature ofindex="400" hdl="0.NA/0.NA" signer="0.NA/0.NA" signerIndex="300">
 * <sig alg="SHA1withDSA" signer="0.NA/0.NA" signerIndex="300">302C02142306D496402DC1CE701244AD0905A38122CFA9FD0214432390E2C7132EFDC2F516FB6B9C670538B8CA32</sig>
 * </signature>
 * }</pre>
 *
 * The hash of a handle value is a hash of its binary representation starting at offset 8, which corresponds to excluding the index and the timestamp from the hash.
 * The signature is a signature of the binary represenation of the digest value, again excluding offset and timestamp.
 * The signature value specifies the index of the corresponding digest value.  The signed handle is specifed in the digest value.
 * The signature value specifies the signer as handle and index.  It can actually contain multiple signatures of multiple signers.
 * The signature algorithm defaults to SHA1with(key-algorithm) if not specified.
 *
 * @deprecated Use {@link JsonWebSignature} and allied classes.
 */
@Deprecated
public class HandleSignature {
    public static final byte[] METADATA_TYPE = Util.encodeString("10320/sig.digest");
    public static final byte[] SIGNATURE_TYPE = Util.encodeString("10320/sig.sig");

    public static class Digest {
        final String algorithm;
        final byte[] digest;

        public Digest(String algorithm, byte[] digest) {
            this.algorithm = algorithm;
            this.digest = digest;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public byte[] getDigest() {
            return digest;
        }
    }

    public static class DigestsValue {
        final String handle;
        @SuppressWarnings("hiding")
        final Map<Integer, List<Digest>> digests;

        public DigestsValue(String handle, Map<Integer, List<Digest>> digests) {
            this.handle = handle;
            this.digests = digests;
        }

        public String getHandle() {
            return handle;
        }

        public Map<Integer, List<Digest>> getDigests() {
            return digests;
        }

        public static DigestsValue ofXml(String xml) throws Exception {
            XParser parser = new XParser();
            XTag root = parser.parse(new StringReader(xml), false);
            String handle = root.getAttribute("hdl");
            Map<Integer, List<Digest>> digests = new HashMap<>();
            if (root.getSubTags() != null) {
                for (XTag child : root.getSubTags()) {
                    int index;
                    try {
                        index = Integer.parseInt(child.getAttribute("index"));
                    } catch (Exception e) {
                        throw new Exception("bad index attribute " + child, e);
                    }
                    List<Digest> digestList = new ArrayList<>();
                    for (Map.Entry<String, String> entry : child.getAttributes().entrySet()) {
                        String name = entry.getKey();
                        if ("index".equals(name)) continue;
                        digestList.add(new Digest(name, Util.encodeHexString(entry.getValue())));
                    }
                    if (digests.containsKey(Integer.valueOf(index))) throw new Exception("duplicate index attribute " + child);
                    digests.put(Integer.valueOf(index), digestList);
                }
            }
            return new DigestsValue(handle, digests);
        }
    }

    final HandleValue digestsValue;
    final DigestsValue parsedDigestsValue;
    final String algorithm;
    final ValueReference signer;
    final byte[] signature;

    public HandleSignature(HandleValue digestsValue, DigestsValue parsedDigestValue, String algorithm, ValueReference signer, byte[] signature) throws Exception {
        this.digestsValue = digestsValue;
        this.parsedDigestsValue = parsedDigestValue;
        this.algorithm = algorithm;
        this.signer = signer;
        this.signature = signature;
    }

    public String getHandle() {
        return parsedDigestsValue.getHandle();
    }

    public HandleValue getDigestsValue() {
        return digestsValue;
    }

    public DigestsValue getParsedDigestsValue() {
        return parsedDigestsValue;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public ValueReference getSigner() {
        return signer;
    }

    public byte[] getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return "HandleSignature [digestsValue=" + digestsValue + ", algorithm=" + algorithm + ", signer=" + signer + "]";
    }

    public static final int VALUE_DIGEST_OFFSET = Encoder.INT_SIZE * 2;

    public static void updateForHandleValue(MessageDigest digest, byte[] encodedHandleValue) {
        digest.update(encodedHandleValue, VALUE_DIGEST_OFFSET, encodedHandleValue.length - VALUE_DIGEST_OFFSET);
    }

    public static void updateForHandleValue(Signature sig, byte[] encodedHandleValue) throws SignatureException {
        sig.update(encodedHandleValue, VALUE_DIGEST_OFFSET, encodedHandleValue.length - VALUE_DIGEST_OFFSET);
    }

    public boolean verifySignature(PublicKey pubKey) throws Exception {
        String alg = algorithm;
        if (alg == null) alg = Util.getSigIdFromHashAlgId(Common.HASH_ALG_SHA1, pubKey.getAlgorithm());
        Signature sig = Signature.getInstance(alg);
        sig.initVerify(pubKey);
        updateForHandleValue(sig, Encoder.encodeHandleValue(digestsValue));
        return sig.verify(signature);
    }

    public boolean verifyValue(String handle, HandleValue value) throws NoSuchAlgorithmException {
        if (!Util.equalsPrefixCI(handle, getHandle())) return false;
        @SuppressWarnings("hiding")
        List<Digest> digests = parsedDigestsValue.getDigests().get(Integer.valueOf(value.getIndex()));
        if (digests == null) return false;
        byte[] encodedHandleValue = Encoder.encodeHandleValue(value);
        boolean foundSha = false;
        for (Digest digest : digests) {
            MessageDigest messageDigest = getMessageDigest(digest.getAlgorithm());
            byte[] checkDigest;
            synchronized (messageDigest) {
                messageDigest.reset();
                updateForHandleValue(messageDigest, encodedHandleValue);
                checkDigest = messageDigest.digest();
            }
            if (!Util.equals(checkDigest, digest.getDigest())) return false;
            if (digest.getAlgorithm().toLowerCase().startsWith("sha")) foundSha = true;
        }
        return foundSha;
    }

    public boolean signsMissingValues(HandleValue[] values) {
        List<Integer> signedIndices = new ArrayList<>(parsedDigestsValue.getDigests().keySet());
        for (HandleValue value : values) {
            signedIndices.remove(Integer.valueOf(value.getIndex()));
        }
        return !signedIndices.isEmpty();
    }

    private static ConcurrentMap<String, MessageDigest> digests = new ConcurrentHashMap<>();

    private static MessageDigest getMessageDigest(String algorithm) throws NoSuchAlgorithmException {
        MessageDigest res = digests.get(algorithm);
        if (res != null) return res;
        MessageDigest newDigest = MessageDigest.getInstance(algorithm);
        digests.putIfAbsent(algorithm, newDigest);
        return newDigest;
    }

    public static List<HandleSignature> getSignatures(HandleValue[] values, HandleValue value, boolean throwOnError) throws Exception {
        try {
            if (!value.hasType(SIGNATURE_TYPE)) return null;
            XParser parser = new XParser();
            XTag root = parser.parse(new StringReader(value.getDataAsString()), false);
            String defaultSigner = root.getAttribute("signer");
            String defaultSignerIndex = root.getAttribute("signerIndex", "300");
            String ofIndex = root.getAttribute("ofIndex", root.getAttribute("ofindex"));
            String handle = root.getAttribute("hdl");

            HandleValue digestsValue = getValueOfIndex(values, ofIndex);
            if (digestsValue == null) throw new Exception("Unable to find digests value for signature value " + root);
            DigestsValue parsedDigestValue = DigestsValue.ofXml(digestsValue.getDataAsString());

            if (handle != null && !Util.equalsPrefixCI(handle, parsedDigestValue.getHandle())) {
                throw new Exception("Handle does not match in signature and digests value " + root);
            }

            List<HandleSignature> res = new ArrayList<>();
            for (XTag child : root.getSubTags()) {
                try {
                    res.add(getHandleSignature(child, digestsValue, parsedDigestValue, defaultSigner, defaultSignerIndex));
                } catch (Exception e) {
                    if (throwOnError) throw e;
                }
            }
            return res;
        } catch (Exception e) {
            if (throwOnError) throw e;
            else return null;
        }
    }

    public static List<HandleSignature> getSignaturesQuietly(HandleValue[] values) {
        try {
            return getSignatures(values, false);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static List<HandleSignature> getSignatures(HandleValue[] values, boolean throwOnError) throws Exception {
        List<HandleSignature> res = new ArrayList<>();
        for (HandleValue value : values) {
            List<HandleSignature> sublist = getSignatures(values, value, throwOnError);
            if (sublist != null) res.addAll(sublist);
        }
        return res;
    }

    private static HandleValue getValueOfIndex(HandleValue[] values, String ofIndex) {
        HandleValue digestsValue = null;
        for (HandleValue candidate : values) {
            if (String.valueOf(candidate.getIndex()).equals(ofIndex)) {
                digestsValue = candidate;
                break;
            }
        }
        return digestsValue;
    }

    private static HandleSignature getHandleSignature(XTag child, HandleValue digestsValue, DigestsValue parsedDigestValue, String defaultSigner, String defaultSignerIndex) throws Exception {
        String algorithm = child.getAttribute("alg");
        String signer = child.getAttribute("signer", defaultSigner);
        String signerIndex = child.getAttribute("signerIndex", defaultSignerIndex);
        String signatureHex = child.getStrValue();
        ValueReference signerRef;
        try {
            signerRef = new ValueReference(Util.encodeString(signer), Integer.parseInt(signerIndex));
        } catch (NumberFormatException e) {
            throw new Exception("Invalid signer index " + signerIndex + " in " + child, e);
        }
        return new HandleSignature(digestsValue, parsedDigestValue, algorithm, signerRef, Util.encodeHexString(signatureHex));
    }

    public static HandleValue createDigestsValue(int index, String handle, HandleValue[] values) {
        XTag root = new XTag("digests");
        root.setAttribute("hdl", handle);
        for (HandleValue value : values) {
            XTag child = new XTag("val");
            child.setAttribute("index", value.getIndex());
            byte[] encodedHandleValue = Encoder.encodeHandleValue(value);
            for (String alg : new String[] { "md5", "sha1" }) {
                MessageDigest messageDigest;
                try {
                    messageDigest = getMessageDigest(alg);
                } catch (NoSuchAlgorithmException e) {
                    throw new AssertionError(e);
                }
                byte[] digest;
                synchronized (messageDigest) {
                    messageDigest.reset();
                    updateForHandleValue(messageDigest, encodedHandleValue);
                    digest = messageDigest.digest();
                }
                child.setAttribute(alg, Util.decodeHexString(digest, false));
            }
            root.addSubTag(child);
        }
        return new HandleValue(index, METADATA_TYPE, Util.encodeString(root.toString()));
    }

    public static HandleValue createSignatureValue(int index, String handle, ValueReference signer, String alg, PrivateKey privKey, HandleValue digestsValue) throws Exception {
        XTag root = new XTag("signature");
        if (handle != null) root.setAttribute("hdl", handle);
        root.setAttribute("ofindex", digestsValue.getIndex());
        root.setAttribute("signer", Util.decodeString(signer.handle));
        root.setAttribute("signerIndex", signer.index);
        if (alg == null) alg = Util.getDefaultSigId(privKey.getAlgorithm());
        XTag child = new XTag("sig");
        child.setAttribute("alg", alg);
        child.setAttribute("signer", Util.decodeString(signer.handle));
        child.setAttribute("signerIndex", signer.index);
        Signature sig = Signature.getInstance(alg);
        sig.initSign(privKey);
        updateForHandleValue(sig, Encoder.encodeHandleValue(digestsValue));
        byte[] signature = sig.sign();
        child.setValue(Util.decodeHexString(signature, false));
        root.addSubTag(child);
        return new HandleValue(index, SIGNATURE_TYPE, Util.encodeString(root.toString()));
    }

    public static HandleValue[] signedHandleValues(String handle, HandleValue[] values, HandleSignature signature, PublicKey pubKey, boolean throwOnError) throws Exception {
        if (!Util.equalsPrefixCI(handle, signature.getHandle())) {
            return new HandleValue[0];
        }
        try {
            if (!signature.verifySignature(pubKey)) return new HandleValue[0];
        } catch (Exception e) {
            if (throwOnError) throw e;
            else return new HandleValue[0];
        }
        List<HandleValue> res = new ArrayList<>();
        for (HandleValue value : values) {
            try {
                if (signature.verifyValue(handle, value)) res.add(value);
            } catch (Exception e) {
                if (throwOnError) throw e;
            }
        }
        return res.toArray(new HandleValue[res.size()]);
    }

    public static boolean signsAllValues(String handle, HandleValue[] values, HandleSignature signature, PublicKey pubKey, boolean throwOnError) throws Exception {
        if (!Util.equalsPrefixCI(handle, signature.getHandle())) {
            return false;
        }
        try {
            if (!signature.verifySignature(pubKey)) return false;
        } catch (Exception e) {
            if (throwOnError) throw e;
            else return false;
        }
        for (HandleValue value : values) {
            try {
                if (valueNeedsSignature(value) && !signature.verifyValue(handle, value)) return false;
            } catch (Exception e) {
                if (throwOnError) throw e;
            }
        }
        return true;
    }

    public static boolean valueNeedsSignature(HandleValue value) {
        if (value.hasType(SIGNATURE_TYPE) || value.hasType(METADATA_TYPE) || !value.publicRead) return false;
        else return true;
    }
}
