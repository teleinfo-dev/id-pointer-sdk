package cn.teleinfo.idpointer.sdk.session;

import java.util.Objects;

public class IdUser {
    private String userIdHandle;
    private int userIdIndex;

    public IdUser(String userIdHandle, int userIdIndex) {
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
        IdUser idUser = (IdUser) o;
        return userIdIndex == idUser.userIdIndex && userIdHandle.equals(idUser.userIdHandle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userIdHandle, userIdIndex);
    }
}
