package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.*;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.util.KeyConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * -----BEGIN PUBLIC KEY-----
 * MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqQhSJmd/fIafPpo40ooIWYmlDTwhFIP5
 * 0QCRRCQLUenHF2f5VY0NSSnxmbUQXkQ7pvPMhH7aBVL6BpsofNkwdsapp5ZwhwXNx7j6UnQRGPZv
 * pCK3Dxcnr6rbcoXjbCSkxDPDdx/qwNSWIQ/3hTJP9A3EZ9r4CIEoJty5tyGWHMPYIaqrLtvZbaYr
 * RbmlpOGB7AIWRLLII8bRFNcSZ2qOGhHQ0Hm8axPKlVvWBIzViunHu7Z6KJ+Ef8RVYEJvgmPHH89b
 * wjM64Q08cN62aXzx132RyZaKSaa8bIYydT+VqJbHT6y1jq2TELXgMGnn+w9aUMJgAzd+6DZ3ISRD
 * 0nL3UwIDAQAB
 * -----END PUBLIC KEY-----
 * <p>
 * -----BEGIN PRIVATE KEY-----
 * MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCpCFImZ398hp8+mjjSighZiaUN
 * PCEUg/nRAJFEJAtR6ccXZ/lVjQ1JKfGZtRBeRDum88yEftoFUvoGmyh82TB2xqmnlnCHBc3HuPpS
 * dBEY9m+kIrcPFyevqttyheNsJKTEM8N3H+rA1JYhD/eFMk/0DcRn2vgIgSgm3Lm3IZYcw9ghqqsu
 * 29ltpitFuaWk4YHsAhZEssgjxtEU1xJnao4aEdDQebxrE8qVW9YEjNWK6ce7tnoon4R/xFVgQm+C
 * Y8cfz1vCMzrhDTxw3rZpfPHXfZHJlopJprxshjJ1P5WolsdPrLWOrZMQteAwaef7D1pQwmADN37o
 * NnchJEPScvdTAgMBAAECggEAJNK4Or+PSUx2gofMWylQ2lifCTwRJMv/byWFf+euQ6D/Cz1OVvHM
 * dCTcChUkxLRunlc8NZ0A1+oUp73DWzdhVG7A5M5kIzYDdz/34GCCnQKKnaImaPYtYhqBhfhe698r
 * ucZhDaF5XZqyPe+hh3XHby5UnB9aq1efrJ+nIZsCfa9dWeZnsFIf1THyuPn6H+yfv1GH3BWtvnpN
 * 92uOBSgnrpVnJJ7WZDHmytjIj/CQhb5pKyXImaOOtOAvQHc8ny+J1jTQLdWinUGm1Zsrzc9IN1zA
 * jiOXW+J/JBVpRpMWtAkeCeT2aU53RF83gqFC4riLaaVxYEcyKWVs5JQ3IyLq8QKBgQDmXtX4Ziq5
 * 9IIzPgi8nbRlZ7HUawyX3YulBNf6G11r1wYlmROx9HxCbgGT36+mGM7lD6hRborArhGQ6UCX7puf
 * 6X0cavcgZMmuLqUVuRwF+HMA1EONvpoNgHR2+E9RsmCI+kTqy8BktQkhwKUdL5/yh4KdQLlZneUU
 * OzXAchkXawKBgQC71oX9Kpkg631yGu+z3VR1L7XZyTJkxfhjq9fDtD6uDmGCVVyOISnm/rXzKcZZ
 * 2Mv0fEUs/KNPRzg5ocODOPoJYTNPRQ5fG+qLfiHcoegjWx+56h1dM0e6um0xD7uLJTZLw4BaGLXV
 * /1Jfw5T/OZzylgp4OdAZn+RKEFGIbTThuQKBgHuJ9gL8fwMz3TKvnK3RgLE0t4erVJiIRV/cRhoo
 * 3KN9Lx2whoBTFOPm7E+pkB9phGIQUHpC5oPHlUH55BrV6X0LEH3R2u7zPbh76SnKF/4Xq0yRiByW
 * TzTYYxx9ssOj/eLXG2gyld0rvFbuYV1Scdr00pWT5RHq+7MqwXuPdGc7AoGAI93oo9jYeIK+52Th
 * VHT0xZOgo79ZUgjDTzTm9EWcmlvAnsRx8em/OI52a8IUT9+nwj1gcCClhmPRBqAu1wWwKM5Yn9BN
 * 2DdaZ9xj1t9LlkS2ICfmB3/dOHiUlQOuCfnHDlx8S7fgMMaaEhMkhj+YCnI/+YkAhYQUcVgAWY6Q
 * xHECgYEAx2yxqhNRrZIqRQvLDWfHu/tvlaDcISBX/HuWE//sqlUG05fFY7DI3r1Va/S5DKf6mueA
 * ojq/oxjQrsFl3gfOwv9ttjYqfd7vSpojyNbZDdHYtzOkdyrOvpsWlYYn+4ef/qdCHpthsyx4Tv8H
 * lg/0xZ8oZVr5aJlFPR7uJAQrHdA=
 * -----END PRIVATE KEY-----
 *
 */
@Slf4j
public class DefaultIdClientTest {
    private static DefaultIdClient idClient;

    @BeforeAll
    public static void setUp() throws Exception {
        // InetSocketAddress inetSocketAddress = new InetSocketAddress("139.198.126.227", 2341);
        // 本地

        // InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.3.2", 2641);
        // InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.4.179", 2641);
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 2641);
        idClient = new DefaultIdClient(inetSocketAddress, 4, 1, 10);

    }

    /**
     * 添加标识（已存在的标识），否则，报错标识不存在
     *
     * @throws IDException
     */
    @Test
    public void addHandleValues() throws IDException {

        List<HandleValue> valueList = new ArrayList<>();
        valueList.add(new HandleValue(3001, "EMAIL", "test@teleinfo.cn"));
        ValueHelper valueHelper = ValueHelper.getInstance();
        idClient.addHandleValues("88.888.3/1", valueHelper.listToArray(valueList));

    }

    /**
     * 创建标识（一个标识下可以有多个value）
     *
     * @throws Exception
     */
    @Test
    public void createHandle() throws Exception {

        List<HandleValue> valueList = new ArrayList<>();
        valueList.add(new HandleValue(3000, "URL", "test"));
        ValueHelper valueHelper = ValueHelper.getInstance();
        // String s = "-----BEGIN PUBLIC KEY-----\n" +
        //         "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqQhSJmd/fIafPpo40ooIWYmlDTwhFIP5\n" +
        //         "0QCRRCQLUenHF2f5VY0NSSnxmbUQXkQ7pvPMhH7aBVL6BpsofNkwdsapp5ZwhwXNx7j6UnQRGPZv\n" +
        //         "pCK3Dxcnr6rbcoXjbCSkxDPDdx/qwNSWIQ/3hTJP9A3EZ9r4CIEoJty5tyGWHMPYIaqrLtvZbaYr\n" +
        //         "RbmlpOGB7AIWRLLII8bRFNcSZ2qOGhHQ0Hm8axPKlVvWBIzViunHu7Z6KJ+Ef8RVYEJvgmPHH89b\n" +
        //         "wjM64Q08cN62aXzx132RyZaKSaa8bIYydT+VqJbHT6y1jq2TELXgMGnn+w9aUMJgAzd+6DZ3ISRD\n" +
        //         "0nL3UwIDAQAB\n" +
        //         "-----END PUBLIC KEY-----";
        // PublicKey publicKey = KeyConverter.fromX509Pem(s);
        // HandleValue handleValue = valueHelper.newStringPublicKeyValue(300, publicKey);
        // valueList.add(handleValue);
        try {
            for (int i = 0; i < 3; i++) {
                idClient.createHandle("88.888.3/20220121-1-" + i, valueHelper.listToArray(valueList));
            }
            for (int i = 0; i < 3; i++) {
                idClient.deleteHandle("88.888.3/20220121-1-" + i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Test
    public void createHandleTemplated() throws Exception {
        ValueHelper valueHelper = ValueHelper.getInstance();
        List<HandleValue> valueList = new ArrayList<>();
        valueList.add(new HandleValue(2000, "terminalType", "test"));
        valueList.add(new HandleValue(2001, "subject", "test"));
        valueList.add(new HandleValue(2002, "useFor", "test"));
        valueList.add(new HandleValue(2003, "serOrImei", "354891092796964"));
        valueList.add(new HandleValue(2004, "licenseNo", "354891092796964"));
        valueList.add(new HandleValue(2005, "licenseMark", "354891092796964"));
        valueList.add(new HandleValue(2006, "licenseSign", "354891092796964"));
        valueList.add(new HandleValue(2007, "EMAIL", "test@teleinfo.cn"));
        valueList.add(new HandleValue(2008, "URL", "www.teleinfo.cn"));
        valueList.add(new HandleValue(2009, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2010, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2011, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2012, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2013, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2014, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2015, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2016, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2017, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2018, "orgName", "北京泰尔英福科技有限公司"));
        valueList.add(new HandleValue(2019, "orgName", "北京泰尔英福科技有限公司"));

        idClient.createHandle("88.888.3/1", valueHelper.listToArray(valueList));

    }

    /**
     * 按照标识删除
     *
     * @throws IDException
     */
    @Test
    public void deleteHandle() throws IDException {
        idClient.deleteHandle("88.888.3/1");
    }

    /**
     * 按照标识和index删除
     *
     * @throws IDException
     */
    @Test
    public void deleteHandleValues() throws IDException {
        idClient.deleteHandleValues("88.888.3/1", new int[]{3001});
    }

    /**
     * 按照标识解析
     *
     * @throws IDException
     */
    @Test
    public void resolveHandle() throws IDException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 2641);
        try {

            GlobalIdClientFactory.newInstance(inetSocketAddress).resolveHandle("0", null, null);
        } catch (IDException e) {
            int code = e.getCode();
            if (code == IDException.CHANNEL_GET_ERROR || code == IDException.PROMISE_GET_ERROR || code == IDException.CLIENT_ERROR) {

            }
        }

        // idClient.resolveHandle("88.888.3/1", null, null);
    }

    /**
     * 按照标识和type进行解析
     *
     * @throws IDException
     */
    @Test
    public void testResolveHandle() throws IDException {
        idClient.resolveHandle("88.888.3/1", null, null);
        //        idClient.resolveHandle("88.888.3/test55", null, null);
    }

    /**
     * 更新标识value
     *
     * @throws IDException
     */
    @Test
    public void updateHandleValues() throws IDException {
        List<HandleValue> valueList = new ArrayList<>();
        valueList.add(new HandleValue(3000, "URL", "test1"));
        ValueHelper valueHelper = ValueHelper.getInstance();
        idClient.updateHandleValues("88.888.3/1", valueHelper.listToArray(valueList), true);
    }

    /**
     * 预置前缀 homePrefix
     *
     * @throws IDException
     */
    @Test
    public void homeNa() throws IDException {
        idClient.homeNa("88.111.1");
    }

    @Test
    void unhomeNa() throws IDException {
        idClient.unhomeNa("88.888.3");
    }

    @Test
    public void patchCreateTest(int i) throws IDException {

    }

    /**
     * 更新
     *
     * @param i
     * @throws IDException
     */
    private void updateHandValues(int i) throws IDException {
        List<HandleValue> valueList = new ArrayList<>();
        valueList.add(new HandleValue(3000, "URL", "test666"));
        ValueHelper valueHelper = ValueHelper.getInstance();
        try {
            idClient.updateHandleValues("88.888.3/test67" + i, valueHelper.listToArray(valueList));
            log.debug("更新成功");
        } catch (IDException e) {
            log.debug("更新失败");
            throw e;
            //            e.printStackTrace();
        }
    }

    /**
     * 删除
     *
     * @param i
     * @throws IDException
     */
    private void deleteHandvalues(int i) throws IDException {
        try {
            idClient.deleteHandle("88.888.3/test67" + i);
            log.debug("删除成功");
        } catch (IDException e) {
            log.debug("删除失败");
            throw e;
            //            e.printStackTrace();
        }
    }

    /**
     * 增加
     *
     * @param i
     * @throws Exception
     */
    private void addHandValues(int i) throws Exception {
        List<HandleValue> valueList = new ArrayList<>();
        valueList.add(new HandleValue(3000, "URL", "__value__" + i));
        ValueHelper valueHelper = ValueHelper.getInstance();
        String s = "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqQhSJmd/fIafPpo40ooIWYmlDTwhFIP5\n" +
                "0QCRRCQLUenHF2f5VY0NSSnxmbUQXkQ7pvPMhH7aBVL6BpsofNkwdsapp5ZwhwXNx7j6UnQRGPZv\n" +
                "pCK3Dxcnr6rbcoXjbCSkxDPDdx/qwNSWIQ/3hTJP9A3EZ9r4CIEoJty5tyGWHMPYIaqrLtvZbaYr\n" +
                "RbmlpOGB7AIWRLLII8bRFNcSZ2qOGhHQ0Hm8axPKlVvWBIzViunHu7Z6KJ+Ef8RVYEJvgmPHH89b\n" +
                "wjM64Q08cN62aXzx132RyZaKSaa8bIYydT+VqJbHT6y1jq2TELXgMGnn+w9aUMJgAzd+6DZ3ISRD\n" +
                "0nL3UwIDAQAB\n" +
                "-----END PUBLIC KEY-----";
        try {
            PublicKey publicKey = KeyConverter.fromX509Pem(s);
            for (int j = 0; j < 0; j++) {
                HandleValue handleValue = valueHelper.newIdisPublicKeyValue(300 + j, publicKey);
                valueList.add(handleValue);
            }
            idClient.createHandle("88.888.3/0917test-" + i, valueHelper.listToArray(valueList));
        } catch (Exception e) {
            throw e;
            //            e.printStackTrace();
        }
    }

    /**
     * 模拟并发测试
     *
     * @throws Exception
     */
    @Test
    public void performTest() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(200);
        CountDownLatch cdl = new CountDownLatch(1);

        // CyclicBarrier cyclicBarrier= new CyclicBarrier(200);
        AtomicInteger atomicInteger = new AtomicInteger();

        long begin = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++) {
            final int temp = i;
            CompletableFuture completableFuture = CompletableFuture.runAsync(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + "被创建");
                    // testResolveHandle();
                    addHandValues(temp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                atomicInteger.incrementAndGet();
            }, executor);
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
        System.out.println(atomicInteger.get());
        System.out.println(System.currentTimeMillis() - begin);

    }

    @Test
    void perfTest() {

        ExecutorService executor = Executors.newFixedThreadPool(50);
        ValueHelper valueHelper = ValueHelper.getInstance();
        AtomicInteger atomicInteger = new AtomicInteger();
        for (int i = 0; i < 10000000; i++) {
            int finalI = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<HandleValue> valueList = new ArrayList<>();
                        valueList.add(new HandleValue(2000, "terminalType", "test"));
                        valueList.add(new HandleValue(2001, "subject", "test"));
                        valueList.add(new HandleValue(2002, "useFor", "test"));
                        valueList.add(new HandleValue(2003, "serOrImei", "354891092796964"));
                        valueList.add(new HandleValue(2004, "licenseNo", "354891092796964"));
                        valueList.add(new HandleValue(2005, "licenseMark", "354891092796964"));
                        valueList.add(new HandleValue(2006, "licenseSign", "354891092796964"));
                        valueList.add(new HandleValue(2007, "EMAIL", "test@teleinfo.cn"));
                        valueList.add(new HandleValue(2008, "URL", "www.teleinfo.cn"));
                        valueList.add(new HandleValue(2009, "orgName", "北京泰尔英福科技有限公司"));
                        idClient.createHandle("88.888.3/20211109-04-" + finalI, valueHelper.listToArray(valueList));
                        atomicInteger.incrementAndGet();
                        Thread.sleep(1L);
                    } catch (IDException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
        }

        System.out.println(atomicInteger.get());
    }


    @Test
    void loginTest() throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 2641);

        String privKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                " MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCLhIKfwJaBaDCd80FB130OlT4l\n" +
                " 67DtwC24qk2IZX+PhjVEvmSMJ7/7SAnDJTJma2OIXW3u3FSq0KikUOy2sm+VROd6fUXO+bcjTwD/\n" +
                " LGb0+Pr2Y5c29+C6SrWeQ9soTPX/8inRGPyXKHWty4lZNx1/Tc3TXtPPDtsPHQ6VqGgpIotJfb8f\n" +
                " xO/tiVm4T2iXquHOgfeLiYwiWdMCs8I6J0VB1muR1iz/PR3rJ3sv4q6Cvj2kJCPl2ZidDUid6TIr\n" +
                " Tsbcg9q73iY/z0zdknxH5e5yzjpZIqBKRnlnTD/1+1/L9LeTn03W0iBS5QqJf+o6i7rXxwTNu6mR\n" +
                " PRNfFZhu3RspAgMBAAECggEAMXJORUYhUU3W5TQAbdBsjw2P0Cq/MxQIYEljXzLxybE8EdgUlKPF\n" +
                " 4839P79gJHJs/Th3rM9D5qI7Erlnm7yT1WhtTbk2U4unP4lRhh57gtj3QPm87wtdB0lRIv1Tn+3e\n" +
                " xc5Sybu8UepSAr1/OTJSbvrpNr+F+/1839dhguMDv4frqe8xImqreE3Cv+KIPz9WW9pAN053Abog\n" +
                " I547syL9qyLiKX4I/uePXRpz3xeafw3dc6AY13BFoogM6hoQHQMFd7s2bjqddLw8XUAZqswYaddZ\n" +
                " yVZqn1xCNZL9ATO+HLolqRtH/vW8w9vNFo4Q0riGsOk2kXjZkYj1YP0anUcF3QKBgQDCAPPL5AQA\n" +
                " iR83lYblNYas9uRhh9yGk2oKoz9rsM5Jb23q8ob+DhzZ5aue7VGVAV381cUd8oPf5lxpLUaiycz+\n" +
                " 0QBc3dIk7tC5vAufi1vbQN72cfg9NFGdLkL5wNMRl32ACArTtjHjAvdCsg0KpRWXhPRdhLEaiiGP\n" +
                " 7n65Ey5TDwKBgQC4GilM68K4zWHd09qkc2UYYdp9rCJmHfDxAzXJYMqTM/RuGg4pRwXPjy3tMb4V\n" +
                " YxWhhttMXKrlZcjOkz5Fi8YFsRtE5D2VgnwtKg1tMs3pNAgGJBzzvIcwQPVoJvmxAkf+9Q7Q+bYe\n" +
                " 2lJPSzfvuM3YAfrmNh+cD+hwpgGIjvLORwKBgGIs8zMR/4h528dYLvydOrsa8fX9N/cOcJJTjIul\n" +
                " 3K7C6gAEnZXBdI48ML5rYGakW3jQgUEncd8Dh25eUCmfL9Mf1ywnwOmZxRzI8aTadDCfQnW6tSHb\n" +
                " 8RU2zQPwtZ9oZ0U8TKZYxCQaYyBoVDs30ZZs3PDabnl8v4FD8vMbpsy3AoGBAK5rSrmvm3oOSywo\n" +
                " rLqlASCKhMAPtz9cGoucaPbzFrlDvc4aLVR6D01H71ELkXUdvL6dz2KXYOmY9xiPc63b8K4vzxAD\n" +
                " 1TyytQbtEm9kdKT+QKPgzvU0pjfXMymL04AaEy+/6FugWST93oF1V/dr9MkzhwtYN7ZZc/RVspo4\n" +
                " JF29AoGBALxjuBsM/xdvVRG05YtOtIBEnv7BLUpPQk5kKBUQk1YkTYUCCBoUUGKZYXw23Vl0l08g\n" +
                " OLYwX50JOOQzdnTl8oBP7lqXKqI2+G57Z3DStvEg8DWUgyyDj9ZjSW/hpnCFa4eMtGZ+WlWAWsR2\n" +
                " PcXdqm7icfcBm/aGDsNrqR/gEuIq\n" +
                " -----END PRIVATE KEY-----";

        PrivateKey privateKey = KeyConverter.fromPkcs8Pem(privKeyPem);
        String userHdl = "88.111.1/admin";
        AuthenticationInfo authenticationInfo = new PublicKeyAuthenticationInfo(Util.encodeString(userHdl), 300, privateKey);

        IDClient idClient = GlobalIdClientFactory.newInstance(inetSocketAddress);
        idClient.login(authenticationInfo);

        ValueHelper valueHelper = ValueHelper.getInstance();
        String pem = "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqQhSJmd/fIafPpo40ooIWYmlDTwhFIP5\n" +
                "0QCRRCQLUenHF2f5VY0NSSnxmbUQXkQ7pvPMhH7aBVL6BpsofNkwdsapp5ZwhwXNx7j6UnQRGPZv\n" +
                "pCK3Dxcnr6rbcoXjbCSkxDPDdx/qwNSWIQ/3hTJP9A3EZ9r4CIEoJty5tyGWHMPYIaqrLtvZbaYr\n" +
                "RbmlpOGB7AIWRLLII8bRFNcSZ2qOGhHQ0Hm8axPKlVvWBIzViunHu7Z6KJ+Ef8RVYEJvgmPHH89b\n" +
                "wjM64Q08cN62aXzx132RyZaKSaa8bIYydT+VqJbHT6y1jq2TELXgMGnn+w9aUMJgAzd+6DZ3ISRD\n" +
                "0nL3UwIDAQAB\n" +
                "-----END PUBLIC KEY-----";

        HandleValue hv = valueHelper.newSiteInfoValue(1, "siteDesc", inetSocketAddress.getAddress(), 2641, pem, true);
        // HandleValue handleValue = new HandleValue(2, Common.STD_TYPE_URL, Util.encodeString("test"));
        // idClient.createHandle("88.111.1/88", new HandleValue[]{hv});

        idClient.resolveHandle("88.111.1/88", null, null);

        // idClient.homeNa("88.802.1");
        // idClient.deleteHandle("88.802.1");
        // idClient.createHandle("88.802.1",new HandleValue[]{hv});

    }

}