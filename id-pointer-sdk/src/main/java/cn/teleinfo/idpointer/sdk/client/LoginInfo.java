package cn.teleinfo.idpointer.sdk.client;

import java.net.InetSocketAddress;
import java.util.Objects;

public class LoginInfo {
    private InetSocketAddress address;
    private DefaultUserId userId;

    public LoginInfo(InetSocketAddress address, DefaultUserId userId) {
        this.address = address;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoginInfo loginInfo = (LoginInfo) o;

        if (!Objects.equals(address, loginInfo.address)) return false;
        return Objects.equals(userId, loginInfo.userId);
    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        return result;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public DefaultUserId getUserId() {
        return userId;
    }
}
