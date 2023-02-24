package cn.teleinfo.idpointer.sdk.client;

import java.util.Objects;

public class IdUserId {
    private String userIdHandle;
    private int userIdIndex;

    public IdUserId(String userIdHandle, int userIdIndex) {
        this.userIdHandle = userIdHandle;
        this.userIdIndex = userIdIndex;
    }

    public String getUserIdHandle() {
        return userIdHandle;
    }

    public int getUserIdIndex() {
        return userIdIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdUserId idUserId = (IdUserId) o;
        return userIdIndex == idUserId.userIdIndex && userIdHandle.equals(idUserId.userIdHandle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userIdHandle, userIdIndex);
    }
}
