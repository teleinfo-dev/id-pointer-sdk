/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import cn.teleinfo.idpointer.sdk.core.SSLEngineHelper;
import cn.teleinfo.idpointer.sdk.core.Util;
import cn.teleinfo.idpointer.sdk.core.stream.util.StreamUtil;
import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class RemoteJsonWebSignatureSigner {

    private final String baseUri;

    static {
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
    }

    public RemoteJsonWebSignatureSigner(String baseUri) {
        this.baseUri = baseUri;
    }

    public JsonWebSignature create(byte[] payload, String username, String password, String privateKeyId, String privateKeyPassphrase) throws TrustException {
        try {
            String serialization = postBytesToUrlPreemptiveBasicAuth(baseUri, payload, username, password, privateKeyId, privateKeyPassphrase);
            JsonWebSignature jws = JsonWebSignatureFactory.getInstance().deserialize(serialization);
            return jws;
        } catch (IOException e) {
            throw new TrustException("Problem communicating with box of trust", e);
        } catch (KeyManagementException e) {
            throw new TrustException("Problem communicating with box of trust", e);
        } catch (NoSuchAlgorithmException e) {
            throw new TrustException("Problem communicating with box of trust", e);
        }
    }

    public JsonWebSignature create(String payloadString, String username, String password, String privateKeyId, String privateKeyPassphrase) throws TrustException {
        try {
            byte[] payload = Util.encodeString(payloadString);
            String serialization = postBytesToUrlPreemptiveBasicAuth(baseUri, payload, username, password, privateKeyId, privateKeyPassphrase);
            JsonWebSignature jws = JsonWebSignatureFactory.getInstance().deserialize(serialization);
            return jws;
        } catch (IOException e) {
            throw new TrustException("Problem communicating with box of trust", e);
        } catch (KeyManagementException e) {
            throw new TrustException("Problem communicating with box of trust", e);
        } catch (NoSuchAlgorithmException e) {
            throw new TrustException("Problem communicating with box of trust", e);
        }
    }

    private static String postBytesToUrlPreemptiveBasicAuth(String urlString, byte[] payload, String username, String password, String privateKeyId, String privateKeyPassphrase)
        throws IOException, TrustException, KeyManagementException, NoSuchAlgorithmException {
        String result = null;
        byte[] entity = payload;
        urlString = urlString + "?privateKeyId=" + privateKeyId;
        if (privateKeyPassphrase != null) {
            urlString = urlString + "&privateKeyPassphrase=" + privateKeyPassphrase;
        }
        URL url = new URL(urlString);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", String.valueOf(entity.length));
            if (connection instanceof HttpsURLConnection) {
                setConnectionToTrustAllCerts((HttpsURLConnection) connection);
            }
            byte[] basicAuthBytes = Util.encodeString(username + ":" + password);
            String basicAuthBase64 = Base64.encodeBase64String(basicAuthBytes);
            connection.setRequestProperty("Authorization", "Basic " + basicAuthBase64);

            OutputStream out = connection.getOutputStream();
            out.write(entity);
            out.close();
            int status = connection.getResponseCode();
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            byte[] responseBytes = StreamUtil.readFully(in);
            result = Util.decodeString(responseBytes);
            in.close();
            if (status != 200) {
                throw new TrustException(result);
            }
        } finally {
            // HttpURLConnection magic reuse, no need to disconnect
            // if (connection != null) connection.disconnect();
        }
        return result;
    }

    private static void setConnectionToTrustAllCerts(HttpsURLConnection connection) throws NoSuchAlgorithmException, KeyManagementException {

        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
            }
        } };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, null);
        connection.setSSLSocketFactory(new SSLEngineHelper.SocketFactoryWrapper(sc.getSocketFactory(), true));

        HostnameVerifier allHostsValid = (hostname, session) -> true;

        connection.setHostnameVerifier(allHostsValid);
    }
}
