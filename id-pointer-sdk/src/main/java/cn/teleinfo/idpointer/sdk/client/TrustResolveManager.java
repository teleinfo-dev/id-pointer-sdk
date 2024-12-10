package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.config.IDClientConfig;
import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.core.trust.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrustResolveManager {

    private static Logger log = LoggerFactory.getLogger(TrustResolveManager.class);

    private static TrustResolveManager trustResolveManager;

    private IDResolver idResolver = GlobalIdClientFactory.getIdResolver();

    private TrustResolveManager() {

    }

    public static TrustResolveManager getInstance() {
        if (trustResolveManager == null) {
            synchronized (TrustResolveManager.class) {
                if (trustResolveManager == null) {
                    trustResolveManager = new TrustResolveManager();
                }
            }
        }
        return trustResolveManager;
    }

    public ChainVerificationReport validateCertValue(HandleValue certValue) throws IDException, TrustException {
        if (!Common.STR_HS_CERT_TYPE.equals(certValue.getTypeAsString())) {
            throw new IDException(0, "must cert type");
        }

        String signatureString = certValue.getDataAsString();
        return validateCertJws(signatureString);
    }

    public ChainVerificationReport validateCertJws(String signatureString) throws IDException {
        JsonWebSignature jws = null;
        try {
            jws = JsonWebSignatureFactory.getInstance().deserialize(signatureString);
            HandleResolverInterface resolver = new HandleResolveAdaptor(idResolver);
            ChainBuilder chainBuilder = new ChainBuilder(resolver);
            List<IssuedSignature> issuedSignatures;
            try {
                issuedSignatures = chainBuilder.buildChain(jws);
            } catch (TrustException e) {
                String message = "Signature NOT VERIFIED unable to build chain: " + e.getMessage();
                throw new IDException(0, message, e);
            }
            IDClientConfig idClientConfig = GlobalIdClientFactory.getIdClientConfig();
            ChainVerifier chainVerifier = new ChainVerifier(getRootKeys(), idClientConfig.getTrustRootHandle());
            ChainVerificationReport chainReport = chainVerifier.verifyChain(issuedSignatures);

            // if (chainReport.canTrust()) {
            //     message = "Signature VERIFIED";
            //     String publicKeyIssue = checkPublicKeyIssue(jws,idResolver);
            //     if (publicKeyIssue != null) {
            //         message += "; WARNING " + publicKeyIssue;
            //     }
            // } else {
            //     message = "Signature NOT VERIFIED";
            // }

            return chainReport;
        } catch (Exception e) {
            if(jws!=null){
                log.error("payload is {}",jws.getPayloadAsString());
            }
            throw new IDException(0, "validate error", e);
        }
    }

    public ChainVerificationReport validateSignatureValue(String handle, HandleValue[] values, HandleValue signatureValue) throws IDException {

        if (!Common.STR_HS_SIGNATURE_TYPE.equals(signatureValue.getTypeAsString())) {
            throw new IDException(0, "must signature type");
        }

        String signatureString = signatureValue.getDataAsString();

        HandleResolverInterface resolver = new HandleResolveAdaptor(idResolver);

        return validateSignatureJws(handle, values, signatureString, resolver);
    }

    public ChainVerificationReport validateSignatureJws(String handle, HandleValue[] values, String signatureString, HandleResolverInterface resolver) throws IDException {
        try {
            JsonWebSignature jws = JsonWebSignatureFactory.getInstance().deserialize(signatureString);
            String message = "";
            ChainBuilder chainBuilder = new ChainBuilder(resolver);
            List<IssuedSignature> issuedSignatures;
            try {
                issuedSignatures = chainBuilder.buildChain(jws);
            } catch (TrustException e) {
                try {
                    HandleClaimsSet claims = HandleVerifier.getInstance().getHandleClaimsSet(jws);
                    String issuer = claims.iss;
                    ValueReference issuerValRef = ValueReference.fromString(issuer);
                    HandleValue handleValue = resolver.resolveValueReference(issuerValRef);
                    if (handleValue != null) {
                        PublicKey issuerPublicKey = Util.getPublicKeyFromBytes(handleValue.getData());
                        ValuesSignatureVerificationReport valuesReport = HandleVerifier.getInstance().verifyValues(handle, Util.filterOnlyPublicValues(Arrays.asList(values)), jws, issuerPublicKey);

                        String valuesReportJson = GsonUtility.getPrettyGson().toJson(valuesReport);
                        System.out.println(valuesReportJson);
                    }
                } catch (Exception ex) {
                    // ignore
                }
                message = "Signature NOT VERIFIED unable to build chain: " + e.getMessage();
                throw new IDException(0, message, e);
            }
            ChainVerifier chainVerifier = new ChainVerifier(getRootKeys(), GlobalIdClientFactory.getIdClientConfig().getTrustRootHandle());

            ChainVerificationReport report = chainVerifier.verifyValues(handle, Arrays.asList(values), issuedSignatures);

            boolean badDigests = report.valuesReport.badDigestValues.size() != 0;
            boolean missingValues = report.valuesReport.missingValues.size() != 0;
            if (report.canTrustAndAuthorized() && !badDigests && !missingValues) {
                message = "Signature VERIFIED";
            } else {
                message = "Signature NOT VERIFIED";
                if (badDigests) {
                    message += " bad digests";
                }
                if (missingValues) {
                    message += " missing values";
                }
            }

            return report;
        } catch (Exception e) {
            throw new IDException(0, "exception", e);
        }
    }

    public String checkPublicKeyIssue(JsonWebSignature jws, IDResolver idResolver) {
        try {
            HandleClaimsSet claims = HandleVerifier.getInstance().getHandleClaimsSet(jws);
            PublicKey pubKeyInCert = claims.publicKey;
            String pubKeyPem = KeyConverter.toX509Pem(pubKeyInCert);
            byte[] certPubKeyBytes = Util.getBytesFromPublicKey(pubKeyInCert);
            ValueReference valRef = ValueReference.fromString(claims.sub);
            @SuppressWarnings("hiding")
            HandleValue[] values;
            if (valRef.index == 0) {
                values = idResolver.resolveHandle(valRef.getHandleAsString(), new String[]{"HS_PUBKEY"}, null);
            } else {
                values = idResolver.resolveHandle(valRef.getHandleAsString(), null, new int[]{valRef.index});
            }
            for (HandleValue value : values) {
                // ll 兼容id-hub idis
                if (pubKeyPem.equals(value.getDataAsString())) {
                    return null;
                }
                if (Util.equals(certPubKeyBytes, value.getData())) {
                    return null;
                }
            }
            return "publicKey does not match subject";
        } catch (Exception e) {
            e.printStackTrace();
            return "exception checking publicKey: " + e.getMessage();
        }
    }


    private List<PublicKey> getRootKeys() {
        List<PublicKey> rootKeys = new ArrayList<>();

        try {
            String rootPublicKeyPem = GlobalIdClientFactory.getIdClientConfig().getTrustRootPubKeyPem();
            PublicKey rootPublicKey = KeyConverter.fromX509Pem(rootPublicKeyPem);
            rootKeys.add(rootPublicKey);

        } catch (Exception e) {
            throw new RuntimeException("load public key error", e);
        }
        return rootKeys;
    }

}