/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.util;


import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.core.trust.HandleClaimsSet;
import cn.teleinfo.idpointer.sdk.core.trust.HandleVerifier;
import cn.teleinfo.idpointer.sdk.core.trust.JsonWebSignature;
import cn.teleinfo.idpointer.sdk.core.trust.JsonWebSignatureFactory;

import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class X509HSTrustManager implements X509TrustManager {
    private final HandleResolver resolver;

    public X509HSTrustManager(HandleResolver resolver) {
        this.resolver = resolver;
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

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    private void authenticate(X509Certificate cert) throws CertificateException {
        ValueReference identity = parseIdentity(cert);
        if (identity == null) throw new CertificateException("Unable to parse identity from certificate");
        try {
            byte[][] reqTypes;
            int[] reqIndexes;
            if (identity.index == 0) {
                reqIndexes = null;
                reqTypes = new byte[][] { Common.PUBLIC_KEY_TYPE };
            } else {
                reqTypes = null;
                reqIndexes = new int[] { identity.index };
            }
            ResolutionRequest resReq = new ResolutionRequest(identity.handle, reqTypes, reqIndexes, null);
            AbstractResponse resp = resolver.processRequest(resReq);
            if (resp instanceof ResolutionResponse) {
                HandleValue[] values = ((ResolutionResponse) resp).getHandleValues();
                byte[] pubKeyBytes = getPublicKeyBytesFromCertificate(cert);
                authenticate(identity, values, pubKeyBytes);
            } else {
                throw new CertificateException("Unexpected response validating X509 certificate", HandleException.ofResponse(resp));
            }
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException("Exception validating X509 certificate", e);
        }
    }

    private static void authenticate(ValueReference identity, HandleValue[] values, byte[] pubKeyBytes) throws CertificateException {
        for (HandleValue value : values) {
            if (identity.index == 0 || value.getIndex() == identity.index) {
                if (Util.equals(value.getData(), pubKeyBytes)) return;
                if (value.hasType(Common.SITE_INFO_TYPE) || value.hasType(Common.DERIVED_PREFIX_SITE_TYPE) || value.hasType(Common.LEGACY_DERIVED_PREFIX_SITE_TYPE)) {
                    try {
                        SiteInfo site = new SiteInfo();
                        Encoder.decodeSiteInfoRecord(value.getData(), 0, site);
                        for (ServerInfo server : site.servers) {
                            if (Util.equals(server.publicKey, pubKeyBytes)) return;
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                } else if (value.hasType(Common.HS_CERT_TYPE)) {
                    try {
                        JsonWebSignature jws = JsonWebSignatureFactory.getInstance().deserialize(value.getDataAsString());
                        HandleClaimsSet claims = HandleVerifier.getInstance().getHandleClaimsSet(jws);
                        if (Util.equals(pubKeyBytes, Util.getBytesFromPublicKey(claims.publicKey))) return;
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        throw new CertificateException("Unable to validate X509 certificate, no matching handle value");
    }

    private byte[] getPublicKeyBytesFromCertificate(java.security.cert.Certificate certificate) throws Exception {
        return Util.getBytesFromPublicKey(certificate.getPublicKey());
    }

    private static ValueReference parseIdentityFromRfc2253Dn(String dn) {
        return new Rfc2253DnParser(dn).getValueReference();
    }

    public static ValueReference parseIdentity(X509Certificate cert) {
        if (cert == null) return null;
        return parseIdentityFromRfc2253Dn(cert.getSubjectX500Principal().getName());
    }

    public static ValueReference parseIdentity(X509Certificate[] cert) {
        if (cert == null || cert.length == 0) return null;
        return parseIdentity(cert[0]);
    }

    private static String parseIdentityHandleFromRfc2253Dn(String dn) {
        return new Rfc2253DnParser(dn).getHandle();
    }

    public static String parseIdentityHandle(X509Certificate cert) {
        if (cert == null) return null;
        return parseIdentityHandleFromRfc2253Dn(cert.getSubjectX500Principal().getName());
    }

    public static String parseIdentityHandle(X509Certificate[] cert) {
        if (cert == null || cert.length == 0) return null;
        return parseIdentityHandle(cert[0]);
    }

    private static class Rfc2253DnParser {
        private final String dn;
        private int index;

        Rfc2253DnParser(String dn) {
            this.dn = dn;
        }

        private static String trim(String s) {
            int start = 0, end = 0;
            for (start = 0; start < s.length(); start++) {
                char ch = s.charAt(start);
                if (ch == ' ') continue;
                else break;
            }
            boolean escaped = false;
            for (int i = start; i < s.length(); i++) {
                char ch = s.charAt(i);
                boolean isSpace = false;
                if (escaped) escaped = false;
                else if (ch == '\\') escaped = true;
                else if (ch == ' ') isSpace = true;
                if (!isSpace) end = i;
            }
            if (start > end) return "";
            return s.substring(start, end + 1);
        }

        private String getType() {
            int start = index;
            index = dn.indexOf('=', start);
            if (index < 0) return null;
            else {
                String type = dn.substring(start, index);
                index++;
                return trim(type);
            }
        }

        private char findSeparator() {
            boolean quoted = false;
            boolean escaped = false;
            for (; index < dn.length(); index++) {
                char ch = dn.charAt(index);
                if (!quoted && !escaped && (ch == '+' || ch == ',' || ch == ';')) return ch;
                else if (escaped) escaped = false;
                else if (ch == '\\') escaped = true;
                else if (ch == '"') quoted = !quoted;
            }
            return ',';
        }

        private static boolean isHexChar(byte ch) {
            return ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'));
        }

        private static int nibbleDecode(byte b) {
            if (b >= '0' && b <= '9') return b - '0';
            if (b >= 'a' && b <= 'f') return 10 + b - 'a';
            if (b >= 'A' && b <= 'F') return 10 + b - 'A';
            return b;
        }

        private static byte hexDecode(byte b1, byte b2) {
            return (byte) ((nibbleDecode(b1) << 4 | nibbleDecode(b2)) & 0xFF);
        }

        private static String unescape(String value) {
            boolean quoted = false;
            boolean escaped = false;
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] bytes = Util.encodeString(value);
            for (int i = 0; i < bytes.length; i++) {
                byte b = bytes[i];
                if (escaped) {
                    escaped = false;
                    if (isHexChar(b)) {
                        i++;
                        if (i >= bytes.length) break;
                        byte b2 = bytes[i];
                        bout.write(hexDecode(b, b2));
                    } else {
                        bout.write(b);
                    }
                } else if (b == '\\') escaped = true;
                else if (b == '"') quoted = !quoted;
                else bout.write(b);
            }
            return Util.decodeString(bout.toByteArray());
        }

        private String getValue() {
            int start = index;
            findSeparator();
            String value = dn.substring(start, index);
            index++;
            value = trim(value);
            if (value.startsWith("#")) return null;
            return unescape(value);
        }

        String getHandleOrValueReference() {
            String uid = null, cn = null, o = null;
            while (index >= 0 && index < dn.length()) {
                String type = getType();
                if (type == null) break;
                String value = getValue();
                if (value == null) continue;
                if ("UID".equalsIgnoreCase(type) && uid == null) uid = value;
                if ("CN".equalsIgnoreCase(type) && cn == null) cn = value;
                if ("O".equalsIgnoreCase(type) && o == null) o = value;
            }
            if (uid != null) return uid;
            if (cn != null) return cn;
            return o;
        }

        private static boolean isDigits(String s) {
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch < '0' || ch > '9') return false;
            }
            return true;
        }

        ValueReference getValueReference() {
            String handleOr = getHandleOrValueReference();
            if (handleOr == null) return null;
            int colon = handleOr.indexOf(':');
            if (colon < 0) return new ValueReference(Util.encodeString(handleOr), 0);
            String maybeIndex = handleOr.substring(0, colon);
            if (isDigits(maybeIndex)) {
                String handle = handleOr.substring(colon + 1);
                return new ValueReference(Util.encodeString(handle), Integer.parseInt(maybeIndex));
            }
            return new ValueReference(Util.encodeString(handleOr), 0);
        }

        String getHandle() {
            String handleOr = getHandleOrValueReference();
            if (handleOr == null) return null;
            int colon = handleOr.indexOf(':');
            if (colon < 0) return handleOr;
            String maybeIndex = handleOr.substring(0, colon);
            if (isDigits(maybeIndex)) {
                String handle = handleOr.substring(colon + 1);
                return handle;
            }
            return handleOr;
        }
    }
}
