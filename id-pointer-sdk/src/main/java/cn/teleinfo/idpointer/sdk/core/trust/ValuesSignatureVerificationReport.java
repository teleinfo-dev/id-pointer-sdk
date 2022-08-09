/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core.trust;

import java.util.ArrayList;
import java.util.List;

public class ValuesSignatureVerificationReport extends SignatureVerificationReport {

    public boolean correctHandle;
    public List<Integer> verifiedValues;
    public List<Integer> missingValues; //present in signature digests missing in values
    public List<Integer> unsignedValues; //present in values missing in signature digests
    public List<Integer> badDigestValues;

    public ValuesSignatureVerificationReport() {
        this.verifiedValues = new ArrayList<>();
        this.missingValues = new ArrayList<>();
        this.unsignedValues = new ArrayList<>();
        this.badDigestValues = new ArrayList<>();
        this.exceptions = new ArrayList<>();
    }
}
