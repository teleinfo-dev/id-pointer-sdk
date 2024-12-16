package cn.teleinfo.idpointer.sdk.client;

import cn.teleinfo.idpointer.sdk.core.ValueReference;

import java.net.InetSocketAddress;
import java.util.Objects;

public class LoginInfoPoolKey {
    private InetSocketAddress address;
    private ValueReference userId;

    public LoginInfoPoolKey(InetSocketAddress address, ValueReference userId) {
        this.address = address;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoginInfoPoolKey loginInfoPoolKey = (LoginInfoPoolKey) o;

        if (!Objects.equals(address, loginInfoPoolKey.address)) return false;
        return Objects.equals(userId, loginInfoPoolKey.userId);
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

    public ValueReference getUserId() {
        return userId;
    }
}
