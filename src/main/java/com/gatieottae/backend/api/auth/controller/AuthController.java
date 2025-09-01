package com.gatieottae.backend.api.auth.controller;

import com.gatieottae.backend.api.auth.dto.SignupDto;
import com.gatieottae.backend.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Auth", description = "인증/회원가입/로그인 API")
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "회원가입",
            description = "username/password 기반 회원가입을 수행합니다. email, nickname은 선택입니다."
    )
    @ApiResponse(responseCode = "201", description = "회원가입 성공",
            content = @Content(schema = @Schema(implementation = SignupDto.SignupResponse.class)))
    @ApiResponse(responseCode = "400", description = "요청 바디 검증 실패")
    @ApiResponse(responseCode = "409", description = "username/email 중복")
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SignupDto.SignupResponse> signup(
            @Valid @RequestBody SignupDto.SignupRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        var res = authService.signup(request);
        return ResponseEntity.created(
                uriBuilder.path("/api/members/{id}").buildAndExpand(res.getId()).toUri()
        ).body(res);
    }
}