package cl.duoc.skyrimemporium.error;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorApi(
        @Schema(example = "400") int status,
        @Schema(example = "Los datos enviados no son válidos") String mensaje,
        @Schema(example = "{\"correo\":\"El correo no tiene un formato válido\"}") Map<String, String> campos) {
}
