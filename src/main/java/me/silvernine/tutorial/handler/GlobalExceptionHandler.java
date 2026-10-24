package me.silvernine.tutorial.handler;

import me.silvernine.tutorial.exception.DuplicateMemberException;
import me.silvernine.tutorial.exception.InvalidRefreshTokenException;
import me.silvernine.tutorial.exception.NotFoundMemberException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * 모든 오류를 RFC 9457 Problem Details(application/problem+json) 형식으로 돌려줍니다.
 * <p>
 * 강의 영상의 ErrorDto 대신 Spring 이 기본 제공하는 {@link ProblemDetail} 을 씁니다.
 * 응답에는 type, title, status, detail, instance 가 들어가고, 입력값 오류에는 fieldErrors 를 덧붙입니다.
 * 필터 단계의 인증 오류(401)와 권한 오류(403)도 JwtAuthenticationEntryPoint, JwtAccessDeniedHandler 를 거쳐 여기로 옵니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateMemberException.class)
    public ProblemDetail duplicateMember(DuplicateMemberException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NotFoundMemberException.class)
    public ProblemDetail notFoundMember(NotFoundMemberException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail invalidRefreshToken(InvalidRefreshTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail authentication(AuthenticationException ex) {
        String detail = switch (ex) {
            case BadCredentialsException e -> "아이디 또는 비밀번호가 올바르지 않습니다.";
            case DisabledException e -> "비활성화된 계정입니다.";
            default -> "인증이 필요합니다.";
        };
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception ex) {
        log.error("처리되지 않은 예외가 발생했습니다.", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
    }

    /** @Valid 검증 실패(400)에 어떤 필드가 왜 틀렸는지 덧붙입니다. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();

        ProblemDetail body = ex.getBody();
        body.setDetail("입력값이 올바르지 않습니다.");
        body.setProperty("fieldErrors", fieldErrors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }
}
