/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/**
 * Same format as ResolutionResponse, but allows values to be empty.  Used for RC_PREFIX_REFERRAL and RC_SERVICE_REFERRAL.
 */
public class ServiceReferralIdResponse extends AbstractIdResponse {

    public byte handle[];
    public byte values[][];

    public ServiceReferralIdResponse(int responseCode, byte handle[], byte values[][]) {
        super(OC_RESOLUTION, responseCode);
        this.handle = handle;
        this.values = values;
    }

    public ServiceReferralIdResponse(AbstractIdRequest req, int responseCode, byte handle[], byte clumps[][]) throws HandleException {
        super(req, responseCode);
        this.handle = handle;
        this.values = clumps;
    }

    public HandleValue[] getHandleValues() throws HandleException {
        if (values == null) return null;
        HandleValue retValues[] = new HandleValue[values.length];
        for (int i = 0; i < retValues.length; i++) {
            retValues[i] = new HandleValue();
            Encoder.decodeHandleValue(values[i], 0, retValues[i]);
        }
        return retValues;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append(' ');
        if (handle == null) sb.append(String.valueOf(handle));
        else sb.append(Util.decodeString(handle));
        sb.append("\n");

        if (values != null) {
            try {
                HandleValue vals[] = getHandleValues();
                for (HandleValue val : vals) {
                    sb.append("   ");
                    sb.append(String.valueOf(val));
                    sb.append('\n');
                }
            } catch (HandleException e) {
            }
        }
        return sb.toString();
    }

    @Override
    public boolean shouldEncrypt() {
        if (!hasEqualOrGreaterVersion(2, 8)) return false;
        if (values == null) return false;
        try {
            for (HandleValue value : getHandleValues()) {
                if (!value.publicRead) return true;
            }
        } catch (HandleException e) {
            return true;
        }
        return false;
    }
}
