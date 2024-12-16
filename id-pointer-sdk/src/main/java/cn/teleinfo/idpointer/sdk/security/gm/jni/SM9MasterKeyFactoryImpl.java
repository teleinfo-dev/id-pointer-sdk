package cn.teleinfo.idpointer.sdk.security.gm.jni;

import cn.teleinfo.idpointer.sdk.security.gm.SM9EncMasterKeypair;
import cn.teleinfo.idpointer.sdk.security.gm.SM9MasterKeyFactory;
import cn.teleinfo.idpointer.sdk.security.gm.SM9SignMasterKeypair;
import org.apache.commons.io.IOUtils;
import org.gmssl.GmSSLJNI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SM9MasterKeyFactoryImpl implements SM9MasterKeyFactory {
    private TempFileManager tempFileManager = TempFileManager.getInstance();

    @Override
    public SM9SignMasterKeypair signMasterKeypairGenerate(String password) {

        SM9SignMasterKeypair sm9SignMasterKeypair = new SM9SignMasterKeypair();

        File tempKeyFile = new File(tempFileManager.getTempFileDir(), "sm9SignMasterPrivateKey" + System.nanoTime() + ".pem");

        long sm9SignMasterGenerate;
        try {
            sm9SignMasterGenerate = GmSSLJNI.sm9_sign_master_key_generate();
            GmSSLJNI.sm9_sign_master_key_info_encrypt_to_pem(sm9SignMasterGenerate, password, tempKeyFile.getAbsolutePath());

            String sm9SignMasterPrivateKeyPem = null;
            try(FileInputStream fileInputStream = new FileInputStream(tempKeyFile)){
                 sm9SignMasterPrivateKeyPem = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
            }

            long sm9_master = GmSSLJNI.sm9_sign_master_key_info_decrypt_from_pem(password, tempKeyFile.getAbsolutePath());
            SM9SignMasterPrivateKeyImpl sm9SignMasterPrivateKey = new SM9SignMasterPrivateKeyImpl(sm9SignMasterPrivateKeyPem,password, sm9_master);
            sm9SignMasterKeypair.setPrivateKey(sm9SignMasterPrivateKey);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            tempKeyFile.delete();
        }

        File tempPubKeyFile = new File(tempFileManager.getTempFileDir(), "key" + System.nanoTime() + ".pem");
        try {
            GmSSLJNI.sm9_sign_master_public_key_to_pem(sm9SignMasterGenerate, tempPubKeyFile.getAbsolutePath());
            String sm9SignMasterPublicKeyPem = null;
            try(FileInputStream fileInputStream = new FileInputStream(tempPubKeyFile)){
                sm9SignMasterPublicKeyPem = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
            }
            long sm9_master_pub = GmSSLJNI.sm9_sign_master_public_key_from_pem(tempPubKeyFile.getAbsolutePath());
            SM9SignMasterPublicKeyImpl sm9SignMasterPublicKey = new SM9SignMasterPublicKeyImpl(sm9SignMasterPublicKeyPem,sm9_master_pub);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            tempPubKeyFile.delete();
        }

        return sm9SignMasterKeypair;
    }

    public SM9EncMasterKeypair encMasterKeypairGenerate() {
        throw new RuntimeException("not impl");
    }

}
