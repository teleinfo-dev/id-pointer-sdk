package cn.teleinfo.idpointer.sdk.transport;

/**
 * Interface for generating request IDs.
 */
public interface RequestIdFactory {

    int getNextInteger();

}
