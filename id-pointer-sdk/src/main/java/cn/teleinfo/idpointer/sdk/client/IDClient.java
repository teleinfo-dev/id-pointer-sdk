package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.AuthenticationInfo;
import cn.teleinfo.idpointer.sdk.core.HandleException;
import cn.teleinfo.idpointer.sdk.core.HandleValue;
import cn.teleinfo.idpointer.sdk.exception.IDException;
import cn.teleinfo.idpointer.sdk.session.IdUser;
import cn.teleinfo.idpointer.sdk.transport.ResponsePromise;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;

public interface IDClient extends IDResolver, Closeable {

    int login(AuthenticationInfo authenticationInfo) throws IDException, HandleException, UnsupportedEncodingException;

    /**
     * Adds new handle records. </br> <b> Note: </b>
     * <li>The administrative priveleges have to be valid for this method to
     * perform without any exception</li>
     *
     * @param handle The handle into which new values are to be added
     * @param values The array of handle values to deposit
     */
    public void addHandleValues(String handle, HandleValue[] values) throws IDException;

    public ResponsePromise addHandleValuesAsync(String handle, HandleValue[] values) throws IDException;

    public void addHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    /**
     * Creates a new handle. If the handle already exists, the method will throw
     * an exception. The proper course of action is then to delete the handle
     * and call the method again.
     *
     * @param handle The handle to create
     * @param values An array of handle values to add to the handle. <b>Note:</b>
     *               <b> Note: </b>
     *               <li>It is important to add admin handle value in order to
     *               administer this handle at a later point.</li>
     *               <li>The administrative priveleges have to be valid for this
     *               method to perform without any exception</li>
     * @throws IDException Describes
     *                     the error that occured in the process of creating the
     *                     handle.
     */
    public void createHandle(String handle, HandleValue[] values) throws IDException;

    ResponsePromise addHandleValuesAsync(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public ResponsePromise createHandleAsync(String handle, HandleValue[] values) throws IDException;

    void createHandle(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    /**
     * Deletes an existing Handle from the handle server. </br> <b> Note: </b>
     * <li>The administrative priveleges have to be valid for this method to
     * perform without any exception</li>
     *
     * @param handle The handle to delete.
     */
    public void deleteHandle(String handle) throws IDException;

    ResponsePromise createHandleAsync(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public ResponsePromise deleteHandleAsync(String handle) throws IDException;

    /**
     * Deletes a specific set of handle values in a Handle. </br> <b> Note: </b>
     * <li>The administrative priveleges have to be valid for this method to
     * perform without any exception</li>
     *
     * @param handle The Handle that we want to delete values from
     * @param values An array of handle values to delete.
     * @throws IDException Describes
     *                     the error that occured while executing the method.
     */
    public void deleteHandleValues(String handle, HandleValue[] values) throws IDException;

    public ResponsePromise deleteHandleValuesAsync(String handle, HandleValue[] values) throws IDException;

    public void deleteHandleValues(String handle, int[] indexes) throws IDException;

    public ResponsePromise deleteHandleValuesAsync(String handle, int[] indexes) throws IDException;


    public ResponsePromise resolveHandleAsync(String handle, String[] types, int[] indexes, boolean auth) throws IDException;

    public ResponsePromise resolveHandleAsync(String handle, String[] types, int[] indexes) throws IDException;


    /**
     * Set how long to wait for responses to TCP and HTTP requests.
     * @param newTcpTimeout Milliseconds to use for timeout.
     */
    // public void setTcpTimeout(int newTcpTimeout);

    /**
     * Get how long to wait for responses to TCP and HTTP requests.
     */
    // public int getTcpTimeout();

    /**
     * Adds and prioritizes the UDP for communication with the Handle server.
     * @param useUDP
     *              The boolean flag that specifies the use of UDP.
     */
    // public void setUseUDP(boolean useUDP);

    /**
     * Updates the specified data handle values. </br> <b> Note: </b>
     * <li>Make sure that the index value is specified in the array of handle
     * values or else this method will not work well.</li>
     * <li>The administrative priveleges have to be valid for this method to
     * perform without any exception</li>
     *
     * @param handle
     * @param values
     */
    public void updateHandleValues(String handle, HandleValue[] values) throws IDException;

    public ResponsePromise updateHandleValuesAsync(String handle, HandleValue[] values) throws IDException;

    void updateHandleValues(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public void homeNa(String na) throws IDException;

    ResponsePromise updateHandleValuesAsync(String handle, HandleValue[] values, boolean overwrite) throws IDException;

    public ResponsePromise homeNaAsync(String na) throws IDException;

    public void unhomeNa(String na) throws IDException;

    public ResponsePromise unhomeNaAsync(String na) throws IDException;

    int getSessionId();

    IdUser getIdUser();

    void setMinorVersion(byte minorVersion);
}
