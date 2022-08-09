/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import cn.teleinfo.idpointer.sdk.core.Encoder;
import cn.teleinfo.idpointer.sdk.core.HandleValue;
import org.apache.commons.codec.binary.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandleValueDigester {
    private static final int VALUE_DIGEST_OFFSET = Encoder.INT_SIZE * 2;

    public DigestedHandleValues digest(List<HandleValue> values, String alg) throws NoSuchAlgorithmException {
        List<DigestedHandleValues.DigestedHandleValue> digests = new ArrayList<>();
        MessageDigest digester = MessageDigest.getInstance(alg);
        for (HandleValue value : values) {
            DigestedHandleValues.DigestedHandleValue digest = digest(value, digester);
            digests.add(digest);
        }
        DigestedHandleValues result = new DigestedHandleValues();
        result.alg = alg;
        result.digests = digests;
        return result;
    }

    private DigestedHandleValues.DigestedHandleValue digest(HandleValue value, MessageDigest digester) {
        byte[] digestBytes = digestHandleValue(value, digester);
        DigestedHandleValues.DigestedHandleValue result = new DigestedHandleValues.DigestedHandleValue();
        result.digest = Base64.encodeBase64String(digestBytes);
        result.index = value.getIndex();
        return result;
    }

    private byte[] digestHandleValue(HandleValue value, MessageDigest digester) {
        digester.reset();
        byte[] encodedHandleValue = Encoder.encodeHandleValue(value);
        digester.update(encodedHandleValue, VALUE_DIGEST_OFFSET, encodedHandleValue.length - VALUE_DIGEST_OFFSET);
        byte[] digestBytes = digester.digest();
        return digestBytes;
    }

    /**
     * Verifies that the given digests correspond to the given values.
     * Note that the function only verifies exact correspondence; see {@link HandleVerifier}
     * for methods that deal separately with undigested, digested-but-missing,
     * bad-digest, and verified values.
     *
     * @param digestedValues the digests to compare.
     * @param values the handle values to compare.
     * @return true if the digests and values correspond, otherwise false.
     * @throws NoSuchAlgorithmException
     */
    public boolean verify(DigestedHandleValues digestedValues, List<HandleValue> values) throws NoSuchAlgorithmException {
        Map<Integer, HandleValue> indexOfValues = new HashMap<>();
        for (HandleValue value : values) {
            indexOfValues.put(value.getIndex(), value);
        }

        if (indexOfValues.size() != digestedValues.digests.size()) {
            return false;
        }
        MessageDigest digester = MessageDigest.getInstance(digestedValues.alg);
        for (DigestedHandleValues.DigestedHandleValue digestedHandleValue : digestedValues.digests) {
            HandleValue value = indexOfValues.get(digestedHandleValue.index);
            if (value == null) {
                return false;
            }
            byte[] digestBytes = digestHandleValue(value, digester);
            String digestAsBase64 = Base64.encodeBase64String(digestBytes);
            if (!digestedHandleValue.digest.equals(digestAsBase64)) {
                return false;
            }
        }
        return true;
    }
}
