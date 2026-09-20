package stb.core.api.rest.v1.dto;

public record ServerResponse<T>(int statusCode, String error, String message, T payload) {

    public static <T> ServerResponse<T> ok(int statusCode, T payload) {
        return new ServerResponse<>(statusCode, null, null, payload);
    }

    public static <T> ServerResponse<T> error(int statusCode, String code, String message) {
        return new ServerResponse<>(statusCode, code, message, null);
    }
}
