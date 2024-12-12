package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.core.HandleValue;

public interface IdConfig extends IdResolver {

    public void loadHandle(String handle, HandleValue[] values,boolean overwrite);

    public void loadAllHandles(String jsonConfig);

}
