package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.AuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.core.PublicKeyAuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.Util;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SampleIdClientLoginTest {

    private ValueHelper valueHelper = ValueHelper.getInstance();

    private static IDClient idClient;

    @BeforeAll
    static void beforeAll() {
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCVk1fcAkQmlC+A/xHlgksSJMmf\n" +
                "OggsoQRdo2VYUmN0kLVWC5PfRkS8ZOCB/FiJ4Wb8VbrdtJtvEduJ0yv+edUw+M0iHrppBs21g3s7\n" +
                "vf8cjd8Y+8DNvpdjzC0ja5VQ7zZq0khe3GonTQZBgyT98lh9nFsVmLSHXlvmbCJnmpKFJjB9TnOE\n" +
                "52wIv9tro3hRg0oF2sPeH2DNlWioKzDL7hLXz5QCSUkpHmy7ZXNmeXlvpJPBKqi3Y/UpG8tWN3eC\n" +
                "1P2onOH1Tr6qRLbpUVts5ta9vnmPYaYrSGJ/jIXyk4lDn6Yl/xm6kXHGw2K0YazFIrWhKUQIA2hM\n" +
                "+rRk6IOghVIxAgMBAAECggEAPlWZV+bZ3/oBkhC6tJsIDhemruTQY0j3OM7PfW9YY0urRqCsj3AJ\n" +
                "VTn1FjbD7zIwaRiRN7P80tzuURHgU70SS8ZkNsP3i616LRsOUGdt2ciGITNJtnSnQr/kT86V+5S1\n" +
                "aO8Zme7hjl9bLBIraGmG11JB404wbfm08uI9tE+GoFYTiZsrcWg3+gMsFMjN2GzDwQNcRqAbkwHz\n" +
                "01pKv3Om0XcOXEnPsGi5CMVm0SAvjgb9/7IUxTxisPmFT/SdAEQecmrHTjz8zt/XpnErbU6TD2tg\n" +
                "3W/DlH3nsRLbIzr0U2wU7BYSBcUSP/RkWpXKclkNNqVT065/CpNB5CQwv7PswQKBgQDvoYMED+WW\n" +
                "Q6726b676wfrtJb1jfrFNg6ZeRubAsvrM0wBxEqtscGbOCTGW4b0P/UDhr62SGUCIXUt7pyW7ef2\n" +
                "GmVP4ceU76OPfzkBfGd0u15YuxQZhvc/S4ccLi+JlFFuZZjw4X4S+0kAlDUKqZ24iQCen2sTDS/A\n" +
                "SSrHxoXWfwKBgQCfywKTyDTjGKUoEdHQooVNZD6VFKN2XrX8bN3QCG+hhVcIFnnFXL0pO0gHQ5IV\n" +
                "LMZlbILZxRF4rbQ0CVSRzSeHuB9VjbbACdhul3Qdql4V/jDigkNXgyjUxzGKQPWMrCb/STgi41tu\n" +
                "WD0BQRKkIXxLYcVEcWvqiPT6fN17U+FfTwKBgQDDuI9iCfnjOXT2hwQaSGU3x1BlT4m6+eQCxkAd\n" +
                "47LacBNsff7gz4bqWHjw6mDXrkVYk/3AtHCLxPgIhBOx0q7a+8Qz8p0osHJPCgJtjEaTuVXd8y7/\n" +
                "ipT3RaEdCzwYiuPX33ODiymGJ2gA6QxJoTAJQR0YcezqqqKN0zT15tf50QKBgQCHaJEWimHGz+uk\n" +
                "uPUDx7UOkBQ4YPTkKf4tmlOdJojyZvwJboJiLORfPE0dWrVAHGFDMWDxCX50tT4vmnh/1UnaSLzE\n" +
                "0wI6Wh1+Gfnb6bMxD+Z9C8XlMtA2/1WLwuBSBNBHJNTdO60PJNcNaQdS7s1VU9TG2xaH+OcgHQ/S\n" +
                "opIpIQKBgQCE54D5THB1Ql/1UTGrPml8hKYAhbsgtHPm6L2LcHw6W6qR1mIoSSdQh4Ica8ReTalp\n" +
                "+LH1waGVjfdSW1CGQl71sc/WV+W2e/Sohfrz1QqSPqyBcRITQik1FgNyg+tG1tkJWfwTr+VPrnLK\n" +
                "y5IRSXRfu/1LsaTcXt1ifyyq5gvfzw==\n" +
                "-----END PRIVATE KEY-----";
        PrivateKey privateKey = null;
        try {
            privateKey = KeyConverter.fromPkcs8Pem(privateKeyPem);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        InetSocketAddress inetSocketAddress = new InetSocketAddress("139.198.19.204", 2644);
        AuthenticationInfo authenticationInfo = new PublicKeyAuthenticationInfo(Util.encodeString("88.111.421/App_lilong_app"), 300, privateKey);

        idClient = new SampleIdClient(inetSocketAddress, 2, 10000,authenticationInfo,false);

    }
    @Test
    void createHandleTest() throws IDException {
        List<HandleValue> list = new ArrayList<>();

        HandleValue hv = new HandleValue(1, "URL", "https://www.teleinfo.cn");
        list.add(hv);

        HandleValue[] hvs = valueHelper.listToArray(list);

        idClient.createHandle("88.111.421/test", hvs);
    }
}
