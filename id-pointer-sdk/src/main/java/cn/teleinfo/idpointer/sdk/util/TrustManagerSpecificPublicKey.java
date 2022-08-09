/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.util;


import cn.teleinfo.idpointer.sdk.core.Util;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class TrustManagerSpecificPublicKey implements X509TrustManager {
    final byte[] pubKeyBytes;

    public TrustManagerSpecificPublicKey(byte[] pubKeyBytes) {
        this.pubKeyBytes = pubKeyBytes;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) throw new IllegalArgumentException("null or empty certificate chain");
        authenticate(chain[0]);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) throw new IllegalArgumentException("null or empty certificate chain");
        authenticate(chain[0]);
    }

    private void authenticate(X509Certificate cert) throws CertificateException {
        try {
            byte[] certPubKeyBytes = getPublicKeyBytesFromCertificate(cert);
            if (Util.equals(pubKeyBytes, certPubKeyBytes)) return;
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException("Exception validating X509 certificate", e);
        }
        throw new CertificateException("Unable to validate X509 certificate, public key does not match expected public key");
    }

    private byte[] getPublicKeyBytesFromCertificate(java.security.cert.Certificate certificate) throws Exception {
        return Util.getBytesFromPublicKey(certificate.getPublicKey());
    }
}
