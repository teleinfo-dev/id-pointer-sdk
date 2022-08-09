/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.util.AutoSelfSignedKeyManager;
import cn.teleinfo.idpointer.sdk.util.TrustManagerSpecificPublicKey;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class SSLEngineHelper {
    public static final String[] ENABLED_SERVER_PROTOCOLS;
    public static final String[] ENABLED_CLIENT_PROTOCOLS;
    public static final String[] ENABLED_CIPHER_SUITES;
    public static final String[] COMPATIBILITY_CIPHER_SUITES;
    // prefer GCM to CBC; SHA2 to SHA1; ECDHE to DHE; AES256 to AES128
    private static final String[] DESIRED_CIPHER_SUITES = {
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
    };
    private static final String[] COMPATIBILITY_ONLY_CIPHER_SUITES = {
            "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_256_CBC_SHA256",
            "TLS_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_3DES_EDE_CBC_SHA"
    };

    static {
        SSLContext context = getAllTrustingClientSSLContext();
        String[] supportedCipherSuitesArray = context.getSupportedSSLParameters().getCipherSuites();
        List<String> supportedCipherSuites = java.util.Arrays.asList(supportedCipherSuitesArray);
        List<String> enabledCipherSuites = new ArrayList<>(java.util.Arrays.asList(DESIRED_CIPHER_SUITES));
        for (String tlsSuite : DESIRED_CIPHER_SUITES) {
            enabledCipherSuites.add(tlsSuite.replaceFirst("TLS", "SSL"));
        }
        enabledCipherSuites.retainAll(supportedCipherSuites);
        ENABLED_CIPHER_SUITES = enabledCipherSuites.toArray(new String[enabledCipherSuites.size()]);
        List<String> compatibilityOnlyCipherSuites = new ArrayList<>(java.util.Arrays.asList(COMPATIBILITY_ONLY_CIPHER_SUITES));
        for (String tlsSuite : COMPATIBILITY_ONLY_CIPHER_SUITES) {
            compatibilityOnlyCipherSuites.add(tlsSuite.replaceFirst("TLS", "SSL"));
        }
        compatibilityOnlyCipherSuites.retainAll(supportedCipherSuites);
        List<String> compatibilityCipherSuites = new ArrayList<>(enabledCipherSuites);
        compatibilityCipherSuites.addAll(compatibilityOnlyCipherSuites);
        COMPATIBILITY_CIPHER_SUITES = compatibilityCipherSuites.toArray(new String[compatibilityCipherSuites.size()]);
    }

    static {
        String[] supportedProtocols = getSupportedProtocols();
        List<String> serverProtocols = new ArrayList<>();
        List<String> clientProtocols = new ArrayList<>();
        for (String protocol : supportedProtocols) {
            if ("SSLv3".equals(protocol)) continue;
            serverProtocols.add(protocol);
            if ("SSLv2Hello".equals(protocol)) continue;
            clientProtocols.add(protocol);
        }
        ENABLED_CLIENT_PROTOCOLS = clientProtocols.toArray(new String[clientProtocols.size()]);
        ENABLED_SERVER_PROTOCOLS = serverProtocols.toArray(new String[serverProtocols.size()]);
    }

    private static String[] getSupportedProtocols() {
        SSLContext context = getAllTrustingClientSSLContext();
        return context.getSupportedSSLParameters().getProtocols();
    }

    public static SSLContext getServerSSLContext(X509Certificate cert, PrivateKey privateKey) throws KeyManagementException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManager[] km = new KeyManager[] { new AutoSelfSignedKeyManager(null, cert, privateKey) };
            sslContext.init(km, null, null);
            return sslContext;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static SSLContext getClientSSLContext(PublicKey publicKey) throws KeyManagementException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] tm = new TrustManager[] { new TrustManagerSpecificPublicKey(Util.getBytesFromPublicKey(publicKey)) };
            sslContext.init(null, tm, null);
            return sslContext;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (HandleException e) {
            throw new KeyManagementException(e.getMessage(), e);
        }
    }

    public static SSLContext getClientSSLContext(byte[] publicKey) throws KeyManagementException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] tm = new TrustManager[] { new TrustManagerSpecificPublicKey(publicKey) };
            sslContext.init(null, tm, null);
            return sslContext;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static SSLEngine getSSLEngine(SSLContext sslContext, boolean clientMode) {
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setEnabledCipherSuites(ENABLED_CIPHER_SUITES);
        engine.setEnabledProtocols(clientMode ? ENABLED_CLIENT_PROTOCOLS : ENABLED_SERVER_PROTOCOLS);
        engine.setUseClientMode(clientMode);
        return engine;
    }

    public static SSLContext getAllTrustingClientSSLContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            // since we are tunneling handle protocol, we consider it acceptable to ignore server cert here
            TrustManager[] tm = new TrustManager[] { new AllTrustingTrustManager() };
            sslContext.init(null, tm, null);
            return sslContext;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (KeyManagementException e) {
            throw new AssertionError(e);
        }
    }

    private static class AllTrustingTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // always trust
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // always trust
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    public static class SocketFactoryWrapper extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        private final boolean clientMode;

        public SocketFactoryWrapper(SSLSocketFactory delegate, boolean clientMode) {
            this.delegate = delegate;
            this.clientMode = clientMode;
        }

        private Socket fix(Socket s) {
            if (s instanceof SSLSocket) {
                ((SSLSocket) s).setEnabledProtocols(clientMode ? SSLEngineHelper.ENABLED_CLIENT_PROTOCOLS : SSLEngineHelper.ENABLED_SERVER_PROTOCOLS);
                ((SSLSocket) s).setEnabledCipherSuites(SSLEngineHelper.ENABLED_CIPHER_SUITES);
            }
            return s;
        }

        @Override
        public Socket createSocket() throws IOException {
            return fix(delegate.createSocket());
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return fix(delegate.createSocket(address, port, localAddress, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return fix(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return fix(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
            return fix(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
            return fix(delegate.createSocket(host, port));
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
