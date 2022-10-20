package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.transport.ResponsePromise;

import java.io.Closeable;

public interface IDClient extends IDResolver, Closeable {

    //int login(AuthenticationInfo authenticationInfo) throws IDException, HandleException, UnsupportedEncodingException;

    public void addHandleValues(String handle, HandleValue[] values) throws IDException;

    public ResponsePromise addHandleValuesAsync(String handle, HandleValue[] values) throws IDException;

    public void addHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public void createHandle(String handle, HandleValue[] values) throws IDException;

    ResponsePromise addHandleValuesAsync(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public ResponsePromise createHandleAsync(String handle, HandleValue[] values) throws IDException;

    void createHandle(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public void deleteHandle(String handle) throws IDException;

    ResponsePromise createHandleAsync(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public ResponsePromise deleteHandleAsync(String handle) throws IDException;

    public void deleteHandleValues(String handle, HandleValue[] values) throws IDException;

    public ResponsePromise deleteHandleValuesAsync(String handle, HandleValue[] values) throws IDException;

    public void deleteHandleValues(String handle, int[] indexes) throws IDException;

    public ResponsePromise deleteHandleValuesAsync(String handle, int[] indexes) throws IDException;

    public ResponsePromise resolveHandleAsync(String handle, String[] types, int[] indexes, boolean auth) throws IDException;

    public ResponsePromise resolveHandleAsync(String handle, String[] types, int[] indexes) throws IDException;

    public void updateHandleValues(String handle, HandleValue[] values) throws IDException;

    public ResponsePromise updateHandleValuesAsync(String handle, HandleValue[] values) throws IDException;

    void updateHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public void homeNa(String na) throws IDException;

    ResponsePromise updateHandleValuesAsync(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public ResponsePromise homeNaAsync(String na) throws IDException;

    public void unhomeNa(String na) throws IDException;

    public ResponsePromise unhomeNaAsync(String na) throws IDException;

    void setMinorVersion(byte minorVersion);
}
