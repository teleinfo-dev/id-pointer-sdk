package cn.teleinfo.idpointer.sdk.session;

import java.util.Objects;

public class DefaultUserId {
    private String userIdHandle;
    private int userIdIndex;

    public DefaultUserId(String userIdHandle, int userIdIndex) {
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
        DefaultUserId defaultUserId = (DefaultUserId) o;
        return userIdIndex == defaultUserId.userIdIndex && userIdHandle.equals(defaultUserId.userIdHandle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userIdHandle, userIdIndex);
    }
}
