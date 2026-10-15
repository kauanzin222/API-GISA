package com.fatec.gisa.exceptions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fatec.gisa.dtos.terapia.response.ConflitoHorarioDTO;
import com.fatec.gisa.dtos.terapia.response.TerapiaForaDaJornadaDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.", request, Map.of());
    }

    @ExceptionHandler(NaoAutorizadoException.class)
    public ResponseEntity<ApiErrorResponse> handleNaoAutorizado(
            NaoAutorizadoException exception,
            HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(EnvioEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleEnvioEmail(
            EnvioEmailException exception,
            HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request, Map.of());
    }

    // Usuário autenticado, mas sem a permissão exigida pela rota (@PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN,
                "Você não tem permissão para realizar esta ação.", request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return response(HttpStatus.BAD_REQUEST, "Erro de validação dos dados.", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fields.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));

        return response(HttpStatus.BAD_REQUEST, "Erro de validação dos dados.", request, fields);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST,
                "O parâmetro '" + exception.getParameterName() + "' é obrigatório.", request, Map.of());
    }

    // Ex.: ?status=XYZ em um parâmetro enum, ou texto em um ID numérico da URL
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST,
                "Valor inválido para '" + exception.getName() + "': " + exception.getValue(), request, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Rota não encontrada.", request, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED,
                "O método " + exception.getMethod() + " não é suportado nesta rota.", request, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.", request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    // Agendamento de terapia com conflito de horário ou fora da jornada: a lista vai junto
    @ExceptionHandler(ConflitoHorarioException.class)
    public ResponseEntity<ApiConflitoResponse> handleConflitoHorario(
            ConflitoHorarioException exception,
            HttpServletRequest request) {
        return conflito(exception, request, exception.getConflitos(), null);
    }

    // Nova jornada do especialista que deixaria terapias futuras fora do horário
    @ExceptionHandler(TerapiasForaDaJornadaException.class)
    public ResponseEntity<ApiConflitoResponse> handleTerapiasForaDaJornada(
            TerapiasForaDaJornadaException exception,
            HttpServletRequest request) {
        return conflito(exception, request, null, exception.getTerapias());
    }

    private ResponseEntity<ApiConflitoResponse> conflito(ConflitoException exception, HttpServletRequest request,
            List<ConflitoHorarioDTO> conflitos, List<TerapiaForaDaJornadaDTO> terapias) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new ApiConflitoResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of(),
                conflitos,
                terapias));
    }

    // Só conflitos lançados de propósito pela API; uma IllegalStateException qualquer
    // (vinda do framework, por exemplo) é erro inesperado e cai no 500
    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiErrorResponse> handleConflito(
            ConflitoException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT,
                "Os dados informados violam uma regra de integridade do banco.",
                request,
                Map.of());
    }

    // Último recurso: nenhum erro sai sem o corpo padrão. Exceções do próprio Spring
    // que já carregam um status (ex.: 415 tipo de conteúdo não suportado) mantêm esse status;
    // as demais viram 500 sem expor detalhes internos ao cliente.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            String detalhe = errorResponse.getBody().getDetail();
            return response(status, detalhe != null ? detalhe : status.getReasonPhrase(), request, Map.of());
        }

        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor. Tente novamente mais tarde.", request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fields) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fields);
        return ResponseEntity.status(status).body(body);
    }
}
