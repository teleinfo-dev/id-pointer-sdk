/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileBasedRequiredSignerStore extends AbstractRequiredSignerStore {

    protected File requiredSignersDir;
    private volatile long lastModified;

    public FileBasedRequiredSignerStore(File requiredSignersDir) {
        this.requiredSignersDir = requiredSignersDir;
    }

    //    public File getDir() {
    //        return certStoreDir;
    //    }

    @Override
    public synchronized void loadSigners() {
        List<JsonWebSignature> certs = new ArrayList<>();
        long newLastModified = requiredSignersDir.lastModified();
        File[] files = requiredSignersDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    continue;
                }
                long thisFileLastModified = f.lastModified();
                if (thisFileLastModified > newLastModified) {
                    newLastModified = thisFileLastModified;
                }
                try {
                    String serialization = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    JsonWebSignature cert = JsonWebSignatureFactory.getInstance().deserialize(serialization);
                    if (!validateSelfSignedCert(cert)) {
                        System.err.println("Required signer cert did not validate. " + f.getName());
                        continue;
                    }
                    certs.add(cert);
                } catch (TrustException e) {
                    System.err.println("Required signer cert could not be loaded. " + f.getName());
                    e.printStackTrace();
                } catch (IOException e) {
                    System.err.println("Required signer cert could not be loaded. " + f.getName());
                    e.printStackTrace();
                }
            }
        }
        lastModified = newLastModified;
        requiredSigners = certs;
    }

    @Override
    public boolean needsLoadSigners() {
        if (requiredSignersDir.lastModified() > lastModified) {
            return true;
        }
        File[] files = requiredSignersDir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isDirectory()) {
                continue;
            }
            if (f.lastModified() > lastModified) {
                return true;
            }
        }
        return false;
    }
}
