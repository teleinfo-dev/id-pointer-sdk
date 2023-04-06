package cn.teleinfo.idpointer.sdk.security.gm;

public interface SM9MasterKeyFactory {
    SM9SignMasterKeypair signMasterKeypairGenerate(String password);
}
