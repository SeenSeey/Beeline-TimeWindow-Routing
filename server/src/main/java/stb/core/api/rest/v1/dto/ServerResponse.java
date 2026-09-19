package stb.core.api.rest.v1.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerResponse<T> {

    private final int statusCode;
    private final String code;
    private final String message;
    private final T payload;

    public static <T> ServerResponse<T> ok(int statusCode, T payload) {
        return new ServerResponse<>(statusCode, null, null, payload);
    }

    public static <T> ServerResponse<T> error(int statusCode, String code, String message) {
        return new ServerResponse<>(statusCode, code, message, null);
    }
}
