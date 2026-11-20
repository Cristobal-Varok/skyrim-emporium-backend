package cl.duoc.skyrimemporium.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @Column(length = 140)
    private String id;

    @Column(nullable = false, length = 40)
    private String gremio;

    @Column(nullable = false, length = 80)
    private String titulo;

    public Categoria() {
    }

    public Categoria(String id, String gremio, String titulo) {
        this.id = id;
        this.gremio = gremio;
        this.titulo = titulo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGremio() { return gremio; }
    public void setGremio(String gremio) { this.gremio = gremio; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
}
