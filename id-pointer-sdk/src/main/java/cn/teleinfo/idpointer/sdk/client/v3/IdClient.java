package cn.teleinfo.idpointer.sdk.client.v3;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;

public interface IdClient extends IdResolver {

    public void createHandle(String handle, HandleValue[] values) throws IDException;

    public void createHandle(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public void deleteHandle(String handle) throws IDException;

    public void addHandleValues(String handle, HandleValue[] values) throws IDException;

    public void addHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public void deleteHandleValues(String handle, HandleValue[] values) throws IDException;

    public void deleteHandleValues(String handle, int[] indexes) throws IDException;

    public void updateHandleValues(String handle, HandleValue[] values) throws IDException;

    public void updateHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public void homeNa(String na) throws IDException;

    public void unhomeNa(String na) throws IDException;

    public IdEngine getIdEngine();

}
