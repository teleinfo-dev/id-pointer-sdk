package cn.teleinfo.idpointer.sdk.security.gm;

public class SM9SignMasterKeypair {
    private SM9SignMasterPublicKey publicKey;
    private SM9SignMasterPrivateKey privateKey;

    public SM9SignMasterPublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(SM9SignMasterPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public SM9SignMasterPrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(SM9SignMasterPrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
