package cn.teleinfo.idpointer.sdk.security.gm.jni;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SM9MasterKeyFactoryImplTest {

    @Test
    void signMasterKeypairGenerate() {
        SM9MasterKeyFactoryImpl sm9MasterKeyFactory = new SM9MasterKeyFactoryImpl();
        sm9MasterKeyFactory.signMasterKeypairGenerate("1234 ");
    }

    @Test
    void encMasterKeypairGenerate() {
    }
}