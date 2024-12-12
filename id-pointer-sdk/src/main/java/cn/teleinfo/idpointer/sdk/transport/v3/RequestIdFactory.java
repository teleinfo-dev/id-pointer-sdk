package cn.teleinfo.idpointer.sdk.transport.v3;

/**
 * Interface for generating request IDs.
 */
public interface RequestIdFactory {

    int getNextInteger();

}
