package com.shottrack.backend.common.web;

/**
 * Envelope padrão de resposta da API.
 * Sucesso: { "data": ... }
 * Erro:    { "error": { "code": ..., "message": ... } }
 */
public record ApiResponse<T>(T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(null, new ApiError(code, message));
    }

    public record ApiError(String code, String message) {
    }
}
