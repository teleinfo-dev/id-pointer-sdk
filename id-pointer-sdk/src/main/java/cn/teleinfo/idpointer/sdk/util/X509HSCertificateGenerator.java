/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.util;

import cn.teleinfo.idpointer.sdk.core.Util;
import cn.teleinfo.idpointer.sdk.core.ValueReference;
import cn.teleinfo.idpointer.sdk.core.stream.util.FastDateFormat;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class X509HSCertificateGenerator {
    private static SecureRandom random = new SecureRandom();
    static {
        random.setSeed(System.nanoTime());
    }

    public static X509Certificate generate(String handle, PublicKey pubKey, PrivateKey privKey) throws Exception {
        if (handle == null) return generateWithUid(null, pubKey, privKey);
        int colon = handle.indexOf(':');
        if (colon < 0) return generateWithUid(handle, pubKey, privKey);
        String maybeIndex = handle.substring(0, colon);
        if (isDigits(maybeIndex)) return generateWithUid("0:" + handle, pubKey, privKey);
        return generateWithUid(handle, pubKey, privKey);
    }

    public static X509Certificate generate(String handle, int index, PublicKey pubKey, PrivateKey privKey) throws Exception {
        return generateWithUid("" + index + ":" + handle, pubKey, privKey);
    }

    public static X509Certificate generate(ValueReference valRef, PublicKey pubKey, PrivateKey privKey) throws Exception {
        return generateWithUid("" + valRef.index + ":" + Util.decodeString(valRef.handle), pubKey, privKey);
    }

    private static final Date notBefore, notAfter;
    static {
        try {
            notBefore = new Date(FastDateFormat.parseUtc("20000101000000Z"));
            notAfter = new Date(FastDateFormat.parseUtc("99991231235959Z"));
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }

    public static X509Certificate generateWithUid(String uid, PublicKey pubKey, PrivateKey privKey) throws Exception {
        return generateWithCnAndUid(null, uid, pubKey, privKey);
    }

    public static X509Certificate generateWithCnAndUid(String cn, String uid, PublicKey pubKey, PrivateKey privKey) throws Exception {
        X500Name name;
        if (uid == null) {
            if (cn == null) cn = "anonymous";
            //            String anon = Util.decodeHexString(Util.doSHA1Digest(pubKey.getEncoded()), false);
            name = new X500Name(new RDN[] { new RDN(new AttributeTypeAndValue(BCStyle.CN, new DERUTF8String(cn))), });
        } else if (cn == null) {
            name = new X500Name(new RDN[] { new RDN(new AttributeTypeAndValue(BCStyle.UID, new DERUTF8String(uid))), });
        } else {
            name = new X500Name(new RDN[] { new RDN(new AttributeTypeAndValue(BCStyle.CN, new DERUTF8String(cn))), new RDN(new AttributeTypeAndValue(BCStyle.UID, new DERUTF8String(uid))), });
        }
        ASN1InputStream in = new ASN1InputStream(pubKey.getEncoded());
        SubjectPublicKeyInfo pubKeyInfo;
        try {
            pubKeyInfo = SubjectPublicKeyInfo.getInstance(in.readObject());
        } finally {
            in.close();
        }
        byte[] serialBytes = new byte[20];
        random.nextBytes(serialBytes);
        serialBytes[0] = (byte) (serialBytes[0] & 0x7F);
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(name, new BigInteger(serialBytes), notBefore, notAfter, name, pubKeyInfo);
        ContentSigner signer = new JcaContentSignerBuilder(Util.getDefaultSigId(privKey.getAlgorithm())).build(privKey);
        X509CertificateHolder certHolder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    public static void storeCertAndKey(KeyStore keyStore, Certificate cert, PrivateKey privKey, String alias, String keyPass) throws KeyStoreException {
        keyStore.setKeyEntry(alias, privKey, keyPass.toCharArray(), new Certificate[] { cert });
    }

    private static void addBCProviderIfNeeded() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    public static void writeCertAsPem(Writer writer, Certificate cert) throws IOException {
        addBCProviderIfNeeded();
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        try {
            pemWriter.writeObject(cert);
        } finally {
            pemWriter.close();
        }
    }

    public static X509Certificate readCertAsPem(Reader reader) throws IOException {
        addBCProviderIfNeeded();
        PEMParser pemReader = new PEMParser(reader);
        try {
            Object obj = pemReader.readObject();
            if (obj instanceof X509Certificate) return (X509Certificate) obj;
            if (obj instanceof X509CertificateHolder) return new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) obj);
            return null;
        } catch (CertificateException e) {
            return null;
        } finally {
            pemReader.close();
            reader.close();
        }
    }

    public static X509Certificate[] readCertChainAsPem(Reader reader) throws IOException {
        addBCProviderIfNeeded();
        List<X509Certificate> certs = new ArrayList<>();
        PEMParser pemReader = new PEMParser(reader);
        try {
            while (true) {
                Object obj = pemReader.readObject();
                if (obj == null) break;
                else if (obj instanceof X509Certificate) certs.add((X509Certificate) obj);
                else if (obj instanceof X509CertificateHolder) certs.add(new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) obj));
            }
        } catch (CertificateException e) {
            return null;
        } finally {
            pemReader.close();
            reader.close();
        }
        return certs.toArray(new X509Certificate[certs.size()]);
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return true;
    }
}
