/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import java.security.PrivateKey;

public class JsonWebSignatureFactoryImpl extends JsonWebSignatureFactory {

    @Override
    public JsonWebSignature create(String payload, PrivateKey privateKey) throws TrustException {
        JsonWebSignature jws = new JsonWebSignatureImpl(payload, privateKey);
        return jws;
    }

    @Override
    public JsonWebSignature create(byte[] payload, PrivateKey privateKey) throws TrustException {
        JsonWebSignature jws = new JsonWebSignatureImpl(payload, privateKey);
        return jws;
    }

    @Override
    public JsonWebSignature deserialize(String serialization) throws TrustException {
        JsonWebSignature jws = new JsonWebSignatureImpl(serialization);
        return jws;
    }

}
