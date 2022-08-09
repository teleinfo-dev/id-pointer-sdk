

package cn.teleinfo.idpointer.sdk.core.trust;


import cn.teleinfo.idpointer.sdk.core.*;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HandleRecordTrustVerifier {

    private final ChainBuilder chainBuilder;
    private final ChainVerifier chainVerifier;
    private boolean isThrowing;

    public HandleRecordTrustVerifier(HandleResolverInterface resolver,List<PublicKey> rootKeys) {
        this.chainBuilder = new ChainBuilder(resolver);
        this.chainVerifier = new ChainVerifier(rootKeys);
    }

    public HandleRecordTrustVerifier(ChainBuilder chainBuilder, ChainVerifier chainVerifier) {
        this.chainBuilder = chainBuilder;
        this.chainVerifier = chainVerifier;
    }

    public void setThrowing(boolean isThrowing) {
        this.isThrowing = isThrowing;
    }

    public boolean validateHandleRecord(HandleRecord handleRecord) throws TrustException {
        HandleRecordTrustVerificationReport report = validateHandleRecordReturnReport(handleRecord);
        String errorMessage = report.getErrorMessage();
        if (errorMessage == null) return true;
        else if (isThrowing) throw new TrustException(errorMessage);
        else return false;
    }

    public HandleRecordTrustVerificationReport validateHandleRecordReturnReport(HandleRecord handleRecord) {
        HandleRecordTrustVerificationReport report = new HandleRecordTrustVerificationReport();
        List<HandleValue> valuesList = handleRecord.getValues();
        Set<Integer> allValues = valuesList.stream()
            .filter(value -> valueNeedsSignature(handleRecord.getHandle(), value))
            .map(HandleValue::getIndex)
            .collect(Collectors.toSet());
        Set<Integer> donaVerifiedValues = new HashSet<>();
        Set<Integer> localCertVerifiedValues = new HashSet<>();
        List<JsonWebSignature> signatures;
        try {
            signatures = getJsonWebSignaturesFromValues(handleRecord.getValuesAsArray());
        } catch (TrustException e) {
            report.exceptions.add(e);
            report.exceptionParsingSignature = true;
            return report;
        }
        if (signatures.isEmpty()) {
            report.noSignatures = true;
            return report;
        }
        for (JsonWebSignature jws : signatures) {
            List<IssuedSignature> chain;
            try {
                chain = chainBuilder.buildChain(jws);
            } catch (TrustException e) {
                report.exceptions.add(e);
                ChainVerificationReport chainReport = buildChainReportWhenUnableToBuildChain(handleRecord, jws);
                report.signatureReports.add(chainReport);
                continue;
            }
            ChainVerificationReport chainReport = chainVerifier.verifyValues(handleRecord.getHandle(), valuesList, chain);
            report.signatureReports.add(chainReport);
            report.requiredSignerNeeded = report.requiredSignerNeeded || chainReport.chainNeedsRequiredSigner;
            if (chainReport.canTrustAndAuthorized()) { //Are there no permissions problems in the DONA sense
                donaVerifiedValues.addAll(chainReport.valuesReport.verifiedValues);
            }
            if (chainReport.canTrustAndAuthorizedUpToRequiredSigner()) { //Are there no permissions problems in the local cert sense
                localCertVerifiedValues.addAll(chainReport.valuesReport.verifiedValues);
            }
        }
        report.signedValues.addAll(donaVerifiedValues);
        report.valuesSignedUpToRequiredSigner.addAll(localCertVerifiedValues);
        Set<Integer> unsignedValues = new HashSet<>(allValues);
        unsignedValues.removeAll(donaVerifiedValues);
        report.unsignedValues.addAll(unsignedValues);
        Set<Integer> valuesNotSignedUpToRequiredSigner = new HashSet<>(allValues);
        valuesNotSignedUpToRequiredSigner.removeAll(localCertVerifiedValues);
        report.valuesNotSignedUpToRequiredSigner.addAll(valuesNotSignedUpToRequiredSigner);
        return report;
    }

    private ChainVerificationReport buildChainReportWhenUnableToBuildChain(HandleRecord handleRecord, JsonWebSignature jws) {
        ChainVerificationReport chainReport = new ChainVerificationReport();
        chainReport.unableToBuildChain = true;
        try {
            HandleClaimsSet claims = HandleVerifier.getInstance().getHandleClaimsSet(jws);
            String issuer = claims.iss;
            ValueReference issuerValRef = ValueReference.fromString(issuer);
            HandleValue handleValue = chainBuilder.resolveValueReference(issuerValRef);
            if (handleValue != null) {
                PublicKey issuerPublicKey = Util.getPublicKeyFromBytes(handleValue.getData());
                chainReport.valuesReport = HandleVerifier.getInstance().verifyValues(handleRecord.getHandle(), Util.filterOnlyPublicValues(handleRecord.getValues()), jws, issuerPublicKey);
            }
        } catch (Exception ex) {
            // ignore
        }
        return chainReport;
    }

    private List<JsonWebSignature> getJsonWebSignaturesFromValues(HandleValue[] newValues) throws TrustException {
        HandleValue[] hsSignatureValues = getSignatureValues(newValues);
        List<JsonWebSignature> signatures = new ArrayList<>();
        if (hsSignatureValues == null) return signatures;
        for (HandleValue hsSignatureValue : hsSignatureValues) {
            JsonWebSignature signature = JsonWebSignatureFactory.getInstance().deserialize(hsSignatureValue.getDataAsString());
            signatures.add(signature);
        }
        return signatures;
    }

    @SuppressWarnings("deprecation")
    private static boolean valueNeedsSignature(String handle, HandleValue value) {
        if (value.hasType(Common.HS_SIGNATURE_TYPE)) return false;
        if (!value.getAnyoneCanRead()) return false;
        if ("0.NA/0.NA".equalsIgnoreCase(handle) && (value.hasType(HandleSignature.SIGNATURE_TYPE) || value.hasType(HandleSignature.METADATA_TYPE))) return false;
        return true;
    }

    private static HandleValue[] getSignatureValues(HandleValue[] newValues) {
        return Util.filterValues(newValues, null, Common.HS_SIGNATURE_TYPE_LIST);
    }
}
