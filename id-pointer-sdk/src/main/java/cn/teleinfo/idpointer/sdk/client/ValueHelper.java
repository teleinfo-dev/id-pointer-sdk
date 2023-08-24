package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.core.trust.*;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ValueHelper {
    private static ValueHelper valueHelper;
    public static final ZoneOffset zoneOffsetBj = ZoneOffset.of("+8");

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ValueHelper() {

    }

    public static ValueHelper getInstance() {
        if (valueHelper == null) {
            synchronized (ValueHelper.class) {
                if (valueHelper == null) {
                    valueHelper = new ValueHelper();
                }
            }
        }
        return valueHelper;
    }

    public SiteInfo getPrimarySite(SiteInfo[] sites) {
        for (SiteInfo site : sites) {
            if (site.isPrimary) {
                return site;
            }
        }
        return null;
    }

    public HandleValue[] listToArray(List<HandleValue> values) {
        HandleValue[] result = new HandleValue[values.size()];
        values.toArray(result);
        return result;
    }

    public HandleValue newIdisPublicKeyValue(int index, PublicKey publicKey) {
        return new HandleValue(index, Common.STD_TYPE_HSPUBKEY, Util.encodeString(KeyConverter.toX509Pem(publicKey)), HandleValue.TTL_TYPE_RELATIVE, 86400, 0, null, true, true, true, false);
    }

    public HandleValue newPublicKeyValue(int index, PublicKey publicKey) throws HandleException {
        byte[] bytesFromPublicKey = Util.getBytesFromPublicKey(publicKey);
        return new HandleValue(index, Common.STD_TYPE_HSPUBKEY, bytesFromPublicKey, HandleValue.TTL_TYPE_RELATIVE, 86400, 0, null, true, true, true, false);
    }

    public String extraPrefix(String identifier) {
        String prefix;
        int separator = identifier.indexOf("/");
        if (separator != -1) {
            if (identifier.toUpperCase().startsWith("0.NA/")) {
                //前缀
                prefix = identifier;
            } else {
                prefix = identifier.substring(0, separator);
            }
        } else {
            //前缀
            prefix = identifier;
        }
        return prefix;
    }

    public String getPrefix(String handle) {
        String prefix = valueHelper.extraPrefix(handle);
        if (prefix.startsWith("0.NA/")) {
            prefix = prefix.substring("0.NA/".length());
        }
        return prefix;
    }

    public List<HandleValue> filterOnlyPublicValues(List<HandleValue> values) {
        return Util.filterOnlyPublicValues(values);
    }

    /**
     * todo: 未实现的权限
     *
     * @param valueIndex
     * @param adminId
     * @param adminIdIndex
     * @param addHandle
     * @param deleteHandle
     * @param addNA        不行
     * @param deleteNA     不行
     * @param modifyValue
     * @param removeValue
     * @param addValue
     * @param modifyAdmin  不行
     * @param removeAdmin  不行
     * @param addAdmin     不行
     * @param readValue
     * @param listHandles  不行
     * @return
     */
    public HandleValue newAdminValue(int valueIndex, String adminId, int adminIdIndex,
                                     boolean addHandle, boolean deleteHandle,
                                     boolean addNA, boolean deleteNA,
                                     boolean modifyValue, boolean removeValue, boolean addValue,
                                     boolean modifyAdmin, boolean removeAdmin, boolean addAdmin,
                                     boolean readValue, boolean listHandles) {
        HandleValue value = new HandleValue();
        value.setIndex(valueIndex);
        value.setType(Common.ADMIN_TYPE);
        AdminRecord admin = new AdminRecord(Util.encodeString(adminId), adminIdIndex, addHandle, deleteHandle, addNA, deleteNA, modifyValue, removeValue, addValue, modifyAdmin, removeAdmin, addAdmin, readValue, listHandles);
        value.setData(Encoder.encodeAdminRecord(admin));
        return value;
    }

    /**
     * 默认权限
     *
     * @param valueIndex
     * @param adminId
     * @param adminIdIndex
     * @return
     */
    public HandleValue newAdminValue(int valueIndex, String adminId, int adminIdIndex) {
        return newAdminValue(valueIndex, adminId, adminIdIndex, true, true, true, true, true, true, true, true, true, true, true, true);
    }

    /**
     * 设置操作权限
     *
     * @param valueIndex
     * @param adminId
     * @param adminIdIndex
     * @param addHandle
     * @param deleteHandle
     * @param modifyValue
     * @param removeValue
     * @param addValue
     * @param readValue
     * @return
     */
    public HandleValue newAdminValue(int valueIndex, String adminId, int adminIdIndex,
                                     boolean addHandle, boolean deleteHandle,
                                     boolean modifyValue, boolean removeValue,
                                     boolean addValue, boolean readValue) {
        return newAdminValue(valueIndex, adminId, adminIdIndex, addHandle, deleteHandle, true, true, modifyValue, removeValue, addValue, true, true, true, readValue, true);
    }

    /**
     * @param index
     * @param pubKey
     * @param issue           300:88.111/test
     * @param subject
     * @param admPrvKey
     * @param expirationTime  "2020-12-12 23:59:59"
     * @param notBefore       "2019-11-25 00:00:00"
     * @param issuedAfterTime "2019-11-24 15:44:00"
     * @return
     * @throws Exception
     */
    public HandleValue newCertValue(int index, PublicKey pubKey, String issue, String subject, PrivateKey admPrvKey, String expirationTime, String notBefore, String issuedAfterTime) throws Exception {
        List<Permission> perms = new ArrayList<>(1);
        perms.add(new Permission(null, Permission.EVERYTHING));
        return newCertValue(index, pubKey, perms, issue, subject, admPrvKey, expirationTime, notBefore, issuedAfterTime);
    }

    public HandleValue newCertValue(int index, PublicKey pubKey, String issue, String subject, PrivateKey admPrvKey, LocalDateTime expirationTime, LocalDateTime notBeforeTime, LocalDateTime issuedAfterTime) throws Exception {
        List<Permission> perms = new ArrayList<>(1);
        perms.add(new Permission(null, Permission.EVERYTHING));
        return newCertValue(index, pubKey, perms, issue, subject, admPrvKey, expirationTime, notBeforeTime, issuedAfterTime);
    }

    public HandleValue newCertValue(int index, PublicKey pubKey, List<Permission> perms, String issue, String subject, PrivateKey admPrvKey, String expirationTime, String notBefore, String issuedAfterTime) throws Exception {

        LocalDateTime expirationTimeObj = LocalDateTime.parse(expirationTime, dateTimeFormatter);
        // String notBefore, String issuedAfterTime

        LocalDateTime notBeforeTime = LocalDateTime.parse(notBefore, dateTimeFormatter);

        LocalDateTime issuedAfterTimeObj = LocalDateTime.parse(issuedAfterTime, dateTimeFormatter);

        return newCertValue(index, pubKey, perms, issue, subject, admPrvKey, expirationTimeObj, notBeforeTime, issuedAfterTimeObj);

    }

    /**
     * @param index
     * @param pubKey
     * @param perms
     * @param issue
     * @param subject
     * @param admPrvKey
     * @param expirationTime
     * @param notBeforeTime
     * @param issuedAfterTime
     * @return
     * @throws Exception
     */
    public HandleValue newCertValue(int index, PublicKey pubKey, List<Permission> perms, String issue, String subject, PrivateKey admPrvKey, LocalDateTime expirationTime, LocalDateTime notBeforeTime, LocalDateTime issuedAfterTime) throws Exception {

        HandleValue value = new HandleValue();
        value.setIndex(index);
        value.setType(Common.HS_CERT_TYPE);

        HandleSigner handleSigner = HandleSigner.getInstance();
        HandleClaimsSet claimsSet = new HandleClaimsSet();
        claimsSet.perms = perms;
        claimsSet.iss = issue;
        claimsSet.sub = subject;

        claimsSet.publicKey = pubKey;

        claimsSet.exp = expirationTime.toEpochSecond(zoneOffsetBj);
        claimsSet.nbf = notBeforeTime.toEpochSecond(zoneOffsetBj);
        claimsSet.iat = issuedAfterTime.toEpochSecond(zoneOffsetBj);

        JsonWebSignature jws = handleSigner.signClaims(claimsSet, admPrvKey);
        String jwsStr = jws.serialize();
        value.setData(Util.encodeString(jwsStr));

        return value;

    }

    public HandleValue newSignatureValue(int index, String handleToSign, List<HandleValue> valuesToSign, ValueReference signer, PrivateKey privateKey, LocalDateTime expirationTime, LocalDateTime notBeforeTime) throws Exception {

        HandleValue value = new HandleValue();
        value.setIndex(index);
        value.setType(Common.HS_SIGNATURE_TYPE);

        HandleSigner handleSigner = HandleSigner.getInstance();
        JsonWebSignature jsonWebSignature = handleSigner.signHandleValues(handleToSign, valuesToSign, signer, privateKey, null, notBeforeTime.toEpochSecond(zoneOffsetBj), expirationTime.toEpochSecond(zoneOffsetBj));

        value.setData(Util.encodeString(jsonWebSignature.serialize()));

        return value;

    }

    public JsonWebSignature getJsonWebSignature(String jwsStr) throws TrustException {
        JsonWebSignatureFactory jsonWebSignatureFactory = JsonWebSignatureFactory.getInstance();
        JsonWebSignature jsonWebSignature = jsonWebSignatureFactory.deserialize(jwsStr);
        return jsonWebSignature;
    }

    public HandleClaimsSet getHandleClaimsSet(JsonWebSignature jsonWebSignature) {
        HandleClaimsSet handleClaimsSet = HandleVerifier.getInstance().getHandleClaimsSet(jsonWebSignature);
        return handleClaimsSet;
    }


    /**
     * @param index
     * @param siteVersion
     * @param isPrimary
     * @param isMultiPrimary
     * @param hashingOption
     * @param siteDescription
     * @param listenAddr
     * @param port
     * @param httpPort
     * @param pubKeyPem
     * @param disableUDP
     * @return
     * @throws Exception
     */
    public HandleValue newSiteInfoValue(int index, int siteVersion, boolean isPrimary, boolean isMultiPrimary, byte hashingOption, String siteDescription, InetAddress listenAddr, int port, int httpPort, String pubKeyPem, boolean disableUDP) throws Exception {
        byte[] pkbuf = getPublicKeyBytes(pubKeyPem);

        SiteInfo site = new SiteInfo(siteVersion, isPrimary, isMultiPrimary, hashingOption, siteDescription, listenAddr, port, httpPort, pkbuf, disableUDP);
        byte[] dataBytes = Encoder.encodeSiteInfoRecord(site);
        return new HandleValue(index, Common.SITE_INFO_TYPE, dataBytes);
    }

    /**
     * @param index
     * @param siteVersion
     * @param isPrimary
     * @param isMultiPrimary
     * @param hashingOption
     * @param siteDescription
     * @param listenAddr
     * @param tcpPort
     * @param udpPort
     * @param httpPort
     * @param pubKeyPem
     * @return
     * @throws Exception
     */
    public HandleValue newSiteInfoValue(int index, int siteVersion, boolean isPrimary, boolean isMultiPrimary, byte hashingOption, String siteDescription, InetAddress listenAddr, int tcpPort, Integer udpPort, Integer httpPort, String pubKeyPem) throws Exception {
        byte[] pkbuf = getPublicKeyBytes(pubKeyPem);

        SiteInfo site = new SiteInfo(siteVersion, isPrimary, isMultiPrimary, hashingOption, siteDescription, listenAddr, tcpPort, udpPort, httpPort, pkbuf);
        byte[] dataBytes = Encoder.encodeSiteInfoRecord(site);
        return new HandleValue(index, Common.SITE_INFO_TYPE, dataBytes);
    }

    private byte[] getPublicKeyBytes(String pubKeyPem) throws Exception {
        if (pubKeyPem != null) {
            PublicKey publicKey = KeyConverter.fromX509Pem(pubKeyPem);
            byte pkbuf[] = Util.getBytesFromPublicKey(publicKey);
            return pkbuf;
        } else {
            return new byte[]{};
        }

    }


    public HandleValue newSiteInfoValue(int index, int siteVersion, boolean isPrimary, boolean isMultiPrimary, byte hashingOption, String siteDescription, InetAddress listenAddr, int port, String pubKeyPem, boolean disableUDP) throws Exception {
        byte[] pkbuf = getPublicKeyBytes(pubKeyPem);

        SiteInfo site = new SiteInfo(siteVersion, isPrimary, isMultiPrimary, hashingOption, siteDescription, listenAddr, port, pkbuf, disableUDP);
        byte[] dataBytes = Encoder.encodeSiteInfoRecord(site);
        return new HandleValue(index, Common.SITE_INFO_TYPE, dataBytes);
    }


    public HandleValue newSiteInfoValue(int index, String siteDescription, InetAddress listenAddr, int port, String pubKeyPem, boolean disableUDP) throws Exception {
        byte[] pkbuf = getPublicKeyBytes(pubKeyPem);

        SiteInfo site = new SiteInfo(1, true, false, SiteInfo.HASH_TYPE_BY_ALL, siteDescription, listenAddr, port, pkbuf, disableUDP);
        byte[] dataBytes = Encoder.encodeSiteInfoRecord(site);
        return new HandleValue(index, Common.SITE_INFO_TYPE, dataBytes);
    }


    public HandleValue[] filterValues(HandleValue[] allValues, int[] indexList, byte[][] typeList) {
        return Util.filterValues(allValues, indexList, typeList);
    }

    public HandleValue newHVListValue(int index, ValueReference[] vr) {
        return newVListValue(index, vr);
    }

    public HandleValue newVListValue(int index, ValueReference[] vr) {
        HandleValue hv = new HandleValue();
        byte[] dataBytes = Encoder.encodeValueReferenceList(vr);
        hv = new HandleValue(index, Common.STD_TYPE_HSVALLIST, dataBytes);
        return hv;
    }


    public String generateUserToken(int userIndex, String userHandle, long nonce, PrivateKey privateKey) {
        LocalDateTime localDateTime = LocalDateTime.now();
        // 获取毫秒数currentTimeMillis
        long milliSecond = Timestamp.valueOf(localDateTime).getTime();

        Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
        String token = JWT.create()
                .withClaim("user_index", userIndex)
                .withClaim("user_handle", userHandle)
                .withClaim("nonce", nonce)
                .withClaim("timestamp", milliSecond)
                .sign(algorithm);
        return token;
    }


}
