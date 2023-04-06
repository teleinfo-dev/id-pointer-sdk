package cn.teleinfo.idpointer.sdk.security.gm.jni;

import cn.teleinfo.idpointer.sdk.security.gm.SM9EncMasterKeypair;
import cn.teleinfo.idpointer.sdk.security.gm.SM9MasterKeyFactory;
import cn.teleinfo.idpointer.sdk.security.gm.SM9SignMasterKeypair;
import org.gmssl.GmSSLJNI;

import java.io.File;

public class SM9MasterKeyFactoryImpl implements SM9MasterKeyFactory {
    private TempFileManager tempFileManager = TempFileManager.getInstance();

    public SM9SignMasterKeypair signMasterKeypairGenerate() {
        File tempKeyFile = new File(tempFileManager.getTempFileDir(),"key"+System.nanoTime()+".pem");
        try{
            long sm9MasterGenerate = GmSSLJNI.sm9_sign_master_key_generate();
            System.out.println(sm9MasterGenerate);
            GmSSLJNI.sm9_sign_master_key_info_encrypt_to_pem(sm9MasterGenerate, "1234", tempKeyFile.getAbsolutePath());



            long sm9MasterKey = GmSSLJNI.sm9_sign_master_key_info_decrypt_from_pem("1234", tempKeyFile.getAbsolutePath());

        }finally {
            tempKeyFile.delete();
        }


        //System.out.println(sm9MasterKey);
        //GmSSLJNI.sm9_sign_master_public_key_to_pem(sm9MasterGenerate, "sm9pub.pem");
        //long sm9_master_pub = GmSSLJNI.sm9_sign_master_public_key_from_pem("sm9pub.pem");



        return null;
    }

    public SM9EncMasterKeypair encMasterKeypairGenerate() {
        return null;
    }

}
