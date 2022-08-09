/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import java.util.List;

public class ChainVerificationReport {
    public ValuesSignatureVerificationReport valuesReport;
    public List<IssuedSignatureVerificationReport> issuedSignatureVerificationReports;
    public boolean rootIsTrusted;
    public boolean chainNeedsRequiredSigner;
    public boolean chainGoodUpToRequiredSigner;
    public boolean unableToBuildChain;

    public boolean canTrustAndAuthorized() {
        if (!rootIsTrusted) return false;
        if (!valuesReport.correctHandle) return false;
        if (!valuesReport.canTrust()) return false;
        for (IssuedSignatureVerificationReport issuedSignatureVerificationReport : issuedSignatureVerificationReports) {
            if (!issuedSignatureVerificationReport.canTrustAndAuthorized()) return false;
        }
        return true;
    }

    public boolean canTrustAndAuthorizedUpToRequiredSigner() {
        if (!chainNeedsRequiredSigner) return false; // not relevant in this case
        if (!rootIsTrusted) return false;
        if (isRequiredSignerNeededAndChainIsGoodUpToRequiredSigner()) return false;
        if (!valuesReport.correctHandle) return false;
        if (!valuesReport.canTrust()) return false;
        // no need to check issued sigs, since ChainVerifier checks them when setting chainGoodUpToLocalCert
        return true;
    }

    public boolean isRequiredSignerNeededAndChainIsGoodUpToRequiredSigner() {
        return (chainNeedsRequiredSigner && !chainGoodUpToRequiredSigner);
    }

    public boolean canTrust() {
        if (!rootIsTrusted) return false;
        for (IssuedSignatureVerificationReport issuedSignatureVerificationReport : issuedSignatureVerificationReports) {
            if (!issuedSignatureVerificationReport.canTrust()) return false;
        }
        return true;
    }
}
