package cl.duoc.skyrimemporium.seguridad;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.skyrimemporium.error.ErrorApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            org.springframework.security.core.AuthenticationException exception) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para acceder a este recurso");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción");
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorApi(estado.value(), mensaje, Map.of()));
    }
}
