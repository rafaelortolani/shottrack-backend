package com.shottrack.backend.support;

import com.shottrack.backend.common.web.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-0021: ainda não existe rota admin de produção, então este controller (só em
 * src/test) exercita a regra de `/api/admin/**`.
 */
@RestController
@RequestMapping("/api/admin/test")
public class AdminTestController {

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.ok("pong"));
    }
}
