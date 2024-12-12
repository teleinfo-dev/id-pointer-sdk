/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
 All rights reserved.

 The HANDLE.NET software is made available subject to the
 Handle.Net Public License Agreement, which may be obtained at
 http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
 \**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.SM2;
import cn.teleinfo.idpointer.sdk.core.GsonUtility;
import cn.teleinfo.idpointer.sdk.core.Util;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class JsonWebSignatureImpl implements JsonWebSignature {
    private final String hashAlg;
    private final String keyAlg;
    private final byte[] header;
    private final byte[] serializedHeader;
    private final byte[] payload;
    private final byte[] serializedPayload;
    private final byte[] signature;
    private final byte[] serializedSignature;

    public JsonWebSignatureImpl(String payload, PrivateKey privateKey) throws TrustException {
        this(Util.encodeString(payload), privateKey);
    }

    public JsonWebSignatureImpl(byte[] payload, PrivateKey privateKey) throws TrustException {
        this.payload = payload;
        keyAlg = privateKey.getAlgorithm();
        if ("RSA".equals(keyAlg)) {
            hashAlg = "SHA256";
            header = Util.encodeString("{\"alg\":\"RS256\"}");
        } else if ("DSA".equals(keyAlg)) {
            hashAlg = "SHA256";
            header = Util.encodeString("{\"alg\":\"DS256\"}");
        } else if ("SM2".equals(keyAlg)) {
            hashAlg = "SM3";
            /**
             * SM2256：遵循与 RS256 类似的命名规则，其中：
             * SM2 表示使用的公钥算法是 SM2。
             * 256 表示使用的哈希算法（SM3）的输出长度为 256 位。
             */
            header = Util.encodeString("{\"alg\":\"SM2256\"}");
        } else {
            throw new IllegalArgumentException("Unsupported key algorithm " + keyAlg);
        }
        serializedHeader = Base64.encodeBase64URLSafe(header);
        serializedPayload = Base64.encodeBase64URLSafe(payload);

        if("SM2".equals(keyAlg)&&"SM3".equals(hashAlg)){
            try {
                PrivateKey privateKeyInner = SecureUtil.generatePrivateKey("SM2", privateKey.getEncoded());
                SM2 sm2 = new SM2(privateKeyInner, null);
                byte[] data = new byte[serializedHeader.length + 1 + serializedPayload.length];
                System.arraycopy(serializedHeader, 0, data, 0, serializedHeader.length);
                data[serializedHeader.length] = '.';
                System.arraycopy(serializedPayload, 0, data, serializedHeader.length + 1, serializedPayload.length);

                signature = sm2.sign(data);
                serializedSignature = Base64.encodeBase64URLSafe(signature);
            } catch (Exception e) {
                throw new TrustException("Error creating JWS", e);
            }
        }else {
            try {
                Signature sig = Signature.getInstance(hashAlg + "with" + keyAlg);
                sig.initSign(privateKey);
                sig.update(serializedHeader);
                sig.update((byte) '.');
                sig.update(serializedPayload);
                signature = sig.sign();
                serializedSignature = Base64.encodeBase64URLSafe(signature);
            } catch (Exception e) {
                throw new TrustException("Error creating JWS", e);
            }
        }

    }

    public JsonWebSignatureImpl(String serialization) throws TrustException {
        if (isCompact(serialization)) {
            try {
                String[] dotSeparatedParts = serialization.split("\\.");
                serializedHeader = Util.encodeString(dotSeparatedParts[0]);
                header = Base64.decodeBase64(serializedHeader);
                serializedPayload = Util.encodeString(dotSeparatedParts[1]);
                payload = Base64.decodeBase64(serializedPayload);
                serializedSignature = Util.encodeString(dotSeparatedParts[2]);
                signature = Base64.decodeBase64(serializedSignature);
            } catch (Exception e) {
                throw new TrustException("Couldn't parse JWS", e);
            }
        } else {
            Gson gson = GsonUtility.getGson();
            JsonWebSignatureJsonSerialization jwsjs = gson.fromJson(serialization, JsonWebSignatureJsonSerialization.class);
            serializedHeader = Util.encodeString(jwsjs.signatures.get(0).protectedPart);
            header = Base64.decodeBase64(serializedHeader);
            serializedPayload = Util.encodeString(jwsjs.payload);
            payload = Base64.decodeBase64(serializedPayload);
            serializedSignature = Util.encodeString(jwsjs.signatures.get(0).signature);
            signature = Base64.decodeBase64(serializedSignature);
        }
        String algString = getAlgStringFromHeader(header);
        keyAlg = getKeyAlgFromAlg(algString);
        hashAlg = getHashAlgFromAlg(algString);
    }

    private static String getAlgStringFromHeader(byte[] header) throws TrustException {
        try {
            String alg = JsonParser.parseString(Util.decodeString(header))
                    .getAsJsonObject()
                    .get("alg")
                    .getAsString();
            return alg;
        } catch (Exception e) {
            throw new TrustException("Couldn't parse JWS header", e);
        }
    }

    private static String getKeyAlgFromAlg(String alg) throws TrustException {
        if (alg.startsWith("RS")) {
            return "RSA";
        } else if (alg.startsWith("DS")) {
            return "DSA";
        } else if (alg.startsWith("SM2")) {
            return "SM2";
        }
        throw new TrustException("Couldn't parse JWS header");
    }

    private static String getHashAlgFromAlg(String alg) throws TrustException {
        if (alg.equals("SM2256")) return "SM3";
        else if(alg.endsWith("256")) return "SHA256";
        else if (alg.endsWith("160") || alg.endsWith("128") || alg.equals("DSA") || alg.equals("DS")) return "SHA1";
        else if (alg.endsWith("384")) return "SHA384";
        else if (alg.endsWith("512")) return "SHA512";
        throw new TrustException("Couldn't parse JWS header");
    }

    private static boolean isCompact(String serialization) {
        return !serialization.trim().startsWith("{");
    }

    @Override
    public String getPayloadAsString() {
        return Util.decodeString(payload);
    }

    @Override
    public byte[] getPayloadAsBytes() {
        return payload.clone();
    }

    @Override
    public boolean validates(PublicKey publicKey) throws TrustException {
        if (!keyAlg.equals(publicKey.getAlgorithm())) return false;
        if(keyAlg.equals("SM2")){
            try {
                PublicKey publicKeyInner = SecureUtil.generatePublicKey("SM2", publicKey.getEncoded());
                SM2 sm2 = new SM2(null, publicKeyInner);
                byte[] data = new byte[serializedHeader.length + 1 + serializedPayload.length];
                System.arraycopy(serializedHeader, 0, data, 0, serializedHeader.length);
                data[serializedHeader.length] = '.';
                System.arraycopy(serializedPayload, 0, data, serializedHeader.length + 1, serializedPayload.length);

                return sm2.verify(data, signature);

            } catch (Exception e) {
                throw new TrustException("Error creating JWS", e);
            }
        }else{
            try {
                Signature sig = Signature.getInstance(hashAlg + "with" + publicKey.getAlgorithm());
                sig.initVerify(publicKey);
                sig.update(serializedHeader);
                sig.update((byte) '.');
                sig.update(serializedPayload);
                return sig.verify(signature);
            } catch (Exception e) {
                throw new TrustException("Error validating JWS", e);
            }
        }

    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(Util.decodeString(serializedHeader));
        sb.append('.');
        sb.append(Util.decodeString(serializedPayload));
        sb.append('.');
        sb.append(Util.decodeString(serializedSignature));
        return sb.toString();
    }

    @Override
    public String serializeToJson() {
        String headerEncoded = Util.decodeString(serializedHeader);
        String payloadEncoded = Util.decodeString(serializedPayload);
        String signatureEncoded = Util.decodeString(serializedSignature);
        String json = "{\"payload\":\"" + payloadEncoded + "\",\"signatures\":[{\"protected\":\"" + headerEncoded + "\",\"signature\":\"" + signatureEncoded + "\"}]}";
        return json;
    }

}
