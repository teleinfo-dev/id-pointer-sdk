package cn.teleinfo.idpointer.sdk.security.gm.jni;

import cn.teleinfo.idpointer.sdk.security.gm.SM9IdPrivateKey;
import cn.teleinfo.idpointer.sdk.security.gm.SM9KeyConverter;
import cn.teleinfo.idpointer.sdk.security.gm.SM9SignMasterPrivateKey;
import cn.teleinfo.idpointer.sdk.security.gm.SM9SignMasterPublicKey;
import org.apache.commons.io.IOUtils;
import org.gmssl.GmSSLJNI;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SM9KeyConverterImpl implements SM9KeyConverter {

    private TempFileManager tempFileManager = TempFileManager.getInstance();

    @Override
    public SM9SignMasterPrivateKey signMasterPrivateKeyFromPem(String pem, String password){

        File tempKeyFile = new File(tempFileManager.getTempFileDir(), "sm9SignMasterPrivateKey" + System.nanoTime() + ".pem");

        try {
            try(FileOutputStream fileInputStream = new FileOutputStream(tempKeyFile)){
                IOUtils.write(pem,fileInputStream, StandardCharsets.UTF_8);
            }
            long sm9_master = GmSSLJNI.sm9_sign_master_key_info_decrypt_from_pem(password, tempKeyFile.getAbsolutePath());
            return new SM9SignMasterPrivateKeyImpl(pem,password, sm9_master);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            tempKeyFile.delete();
        }
    }

    @Override
    public SM9SignMasterPublicKey signMasterPublicKeyFromPem(String pem){
        TempFileManager tempFileManager = TempFileManager.getInstance();
        File tempPubKeyFile = new File(tempFileManager.getTempFileDir(), "key" + System.nanoTime() + ".pem");
        try {
            try(FileOutputStream fileInputStream = new FileOutputStream(tempPubKeyFile)){
                IOUtils.write(pem,fileInputStream, StandardCharsets.UTF_8);
            }
            long sm9_master_pub = GmSSLJNI.sm9_sign_master_public_key_from_pem(tempPubKeyFile.getAbsolutePath());
            SM9SignMasterPublicKeyImpl sm9SignMasterPublicKey = new SM9SignMasterPublicKeyImpl(pem,sm9_master_pub);
            return  sm9SignMasterPublicKey;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            tempPubKeyFile.delete();
        }
    }

    @Override
    public SM9IdPrivateKey idPrivateKeyFromPem(String pem, String password){

        File tempKeyFile = new File(tempFileManager.getTempFileDir(), "sm9IdPrivateKey" + System.nanoTime() + ".pem");
        try {
            try(FileOutputStream fileInputStream = new FileOutputStream(tempKeyFile)){
                IOUtils.write(pem,fileInputStream, StandardCharsets.UTF_8);
            }
            long sm9IdKey = GmSSLJNI.sm9_sign_key_info_decrypt_from_pem(password, tempKeyFile.getAbsolutePath());
            return new SM9IdPrivateKeyImpl(pem,password, sm9IdKey);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            tempKeyFile.delete();
        }
    }




}
