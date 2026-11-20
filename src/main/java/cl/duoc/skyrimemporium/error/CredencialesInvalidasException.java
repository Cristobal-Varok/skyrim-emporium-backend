package cl.duoc.skyrimemporium.error;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}
