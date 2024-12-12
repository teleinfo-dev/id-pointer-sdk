/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

public class GetSiteInfoIdResponse extends AbstractIdResponse {
    public SiteInfo siteInfo;

    /**************************************************************
     * Constructor used on the client side.
     **************************************************************/
    public GetSiteInfoIdResponse(SiteInfo siteInfo) {
        super(AbstractMessage.OC_GET_SITE_INFO, AbstractMessage.RC_SUCCESS);
        this.siteInfo = siteInfo;
    }

    /**************************************************************
     * Constructor used on the server side.
     **************************************************************/
    public GetSiteInfoIdResponse(AbstractIdRequest req, SiteInfo siteInfo) throws HandleException {
        super(req, AbstractMessage.RC_SUCCESS);
        this.siteInfo = siteInfo;
    }

    @Override
    public String toString() {
        return super.toString() + "; siteInfo:" + String.valueOf(siteInfo);
    }
}
