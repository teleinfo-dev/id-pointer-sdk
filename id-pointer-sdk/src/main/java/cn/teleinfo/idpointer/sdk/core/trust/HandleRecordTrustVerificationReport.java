package cn.teleinfo.idpointer.sdk.core.trust;

import java.util.ArrayList;
import java.util.List;

public class HandleRecordTrustVerificationReport {
    public boolean noSignatures;
    public boolean exceptionParsingSignature;
    public List<ChainVerificationReport> signatureReports = new ArrayList<>();
    public List<Integer> signedValues = new ArrayList<>();
    public List<Integer> unsignedValues = new ArrayList<>();
    public boolean requiredSignerNeeded;
    public List<Integer> valuesSignedUpToRequiredSigner = new ArrayList<>();
    public List<Integer> valuesNotSignedUpToRequiredSigner = new ArrayList<>();
    public List<Exception> exceptions = new ArrayList<>();

    public String getErrorMessage() {
        if (noSignatures) return "no signatures";
        if (exceptionParsingSignature) return "exception parsing signature";
        for (ChainVerificationReport chainReport : signatureReports) {
            if (chainReport.unableToBuildChain) return "unable to build chain";
            if (!chainReport.valuesReport.correctHandle) return "incorrect handle";
            if (!chainReport.valuesReport.validPayload) return "invalid signature payload";
            if (!chainReport.valuesReport.signatureVerifies) return "signature does not verify";
            if (!chainReport.valuesReport.dateInRange) return "incorrect date range in signature";
            if (!chainReport.valuesReport.badDigestValues.isEmpty()) return "bad digests";
            if (!chainReport.valuesReport.missingValues.isEmpty()) return "missing values";
            if (!chainReport.canTrust()) return "chain not trusted";
        }
        if (!unsignedValues.isEmpty()) return "unsigned values";
        if (requiredSignerNeeded && !valuesNotSignedUpToRequiredSigner.isEmpty()) return "values not signed up to requiredSigners";
        return null;
    }
}
