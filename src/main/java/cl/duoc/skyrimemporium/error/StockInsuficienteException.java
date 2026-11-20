package cl.duoc.skyrimemporium.error;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
