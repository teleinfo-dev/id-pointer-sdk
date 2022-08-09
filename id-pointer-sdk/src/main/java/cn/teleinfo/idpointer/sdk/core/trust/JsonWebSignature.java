/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import java.security.PublicKey;

public interface JsonWebSignature {
    public String getPayloadAsString();

    public byte[] getPayloadAsBytes();

    public boolean validates(PublicKey publicKey) throws TrustException;

    public String serialize();

    public String serializeToJson();
}
