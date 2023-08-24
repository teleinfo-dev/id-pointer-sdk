/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

/**
 * Request used to resolve a handle.  Holds the handle and parameters
 * used in resolution.
 */
public class ResolutionRequest extends AbstractRequest {

    public byte requestedTypes[][] = null;
    public int requestedIndexes[] = null;
    /**
     * 递归传输身份使用的认证信息
     */
    public byte[] authBytes= null;

    public ResolutionRequest(byte handle[], byte reqTypes[][], int reqIndexes[], AuthenticationInfo authInfo) {
        super(handle, AbstractMessage.OC_RESOLUTION, authInfo);
        this.requestedIndexes = reqIndexes;
        this.requestedTypes = reqTypes;
        this.authInfo = authInfo;
    }

    /**
     * 添加构造方式，用于递归传输身份
     */
    public ResolutionRequest(byte handle[], byte reqTypes[][], int reqIndexes[], AuthenticationInfo authInfo,String authString) {
        this(handle, reqTypes, reqIndexes, authInfo);
        this.authBytes = Util.encodeString(authString);
    }

    private String getTypesString() {
        if (requestedTypes == null || requestedTypes.length <= 0) return "[ ]";
        StringBuffer sb = new StringBuffer("[");
        for (byte[] requestedType : requestedTypes) {
            sb.append(Util.decodeString(requestedType));
            sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private String getIndexesString() {
        if (requestedIndexes == null || requestedIndexes.length <= 0) return "[ ]";
        StringBuffer sb = new StringBuffer("[");
        for (int requestedIndexe : requestedIndexes) {
            sb.append(requestedIndexe);
            sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return super.toString() + ' ' + getTypesString() + ' ' + getIndexesString();
    }

}
