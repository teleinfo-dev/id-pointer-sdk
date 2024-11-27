package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SampleIdClientTest {
    private ValueHelper valueHelper = ValueHelper.getInstance();

    private static IDClient idClient;

    @BeforeAll
    public static void setUp() throws Exception {
        //InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.149.5", 2641);
        InetSocketAddress inetSocketAddress = new InetSocketAddress("10.14.153.106", 30041);
        idClient = new SampleIdClient(inetSocketAddress, 60);
    }

    @Order(1)
    @Test
    void homeNa() throws IDException {
        idClient.homeNa("88.888.1024");

    }

    @Order(2)
    @Test
    void createHandle() throws IDException {
        List<HandleValue> list = new ArrayList<>();

        HandleValue hv = new HandleValue(1, "URL", "https://www.teleinfo.cn");
        list.add(hv);

        HandleValue[] hvs = valueHelper.listToArray(list);

        idClient.createHandle("88.888.1024/test", hvs);
    }
    @Order(3)
    @Test
    void addHandleValues() throws IDException {
        List<HandleValue> list = new ArrayList<>();
        HandleValue hv = new HandleValue(2, "EMAIL", "lilong@teleinfo.cn");
        list.add(hv);
        HandleValue[] hvs = valueHelper.listToArray(list);

        idClient.addHandleValues("88.888.1024/test", hvs);
    }

    @Order(4)
    @Test
    void updateHandleValues() throws IDException {
        List<HandleValue> list = new ArrayList<>();
        HandleValue hv = new HandleValue(2, "EMAIL", "test@teleinfo.cn");
        list.add(hv);
        HandleValue[] hvs = valueHelper.listToArray(list);

        idClient.updateHandleValues("88.888.1024/test", hvs);
    }

    @Order(5)
    @Test
    void resolveHandle() throws IDException {

        HandleValue[] handleValues = idClient.resolveHandle("88.888.1024/admin");
        Assertions.assertTrue(handleValues.length > 0,"");

    }

    @Order(6)
    @Test
    void deleteHandle() throws IDException {
        idClient.deleteHandle("88.888.1024/test");
    }

    @Order(7)
    @Test
    void unhomeNa() throws IDException {
        idClient.unhomeNa("88.888.1024");
    }

    @AfterAll
    public static void close() throws Exception {
        idClient.close();
    }
}