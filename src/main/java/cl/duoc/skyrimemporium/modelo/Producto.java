package cl.duoc.skyrimemporium.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @Schema(example = "alquimia::curar-enfermedad")
    @NotBlank(message = "El identificador es obligatorio")
    private String id;

    @Column(unique = true, length = 80)
    @Schema(example = "ALQ-POC-001")
    @Size(max = 80, message = "El código admite hasta 80 caracteres")
    private String codigo;

    @Schema(example = "Pocion para curar enfermedades")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    private String name;

    @Schema(example = "150")
    @NotNull(message = "El precio es obligatorio")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    private Integer price;

    @Schema(example = "Una pocion que cura enfermedades comunes.")
    @Size(max = 500, message = "La descripción admite hasta 500 caracteres")
    private String description;

    @Lob
    @Schema(example = "assets/img/Alquimia/curar-enfermedad.png")
    @Size(max = 5_000_000, message = "La imagen admite hasta 5 MB")
    private String image;

    @Schema(example = "25")
    @NotNull(message = "El stock es obligatorio")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer stock;

    @Schema(example = "alquimia")
    @NotBlank(message = "El gremio es obligatorio")
    private String gremio;

    @Schema(example = "Pociones")
    @NotBlank(message = "La categoría es obligatoria")
    private String categoriaSeccion;

    @Schema(example = "5")
    @NotNull(message = "El stock crítico es obligatorio")
    @PositiveOrZero(message = "El stock crítico no puede ser negativo")
    private Integer stockCritico;

    @Version
    @Schema(example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    private Long version;

    public Producto() {
    }

    public Producto(String id, String name, Integer price, String description, String image,
                    Integer stock, String gremio, String categoriaSeccion, Integer stockCritico) {
        this.id = id;
        this.codigo = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.image = image;
        this.stock = stock;
        this.gremio = gremio;
        this.categoriaSeccion = categoriaSeccion;
        this.stockCritico = stockCritico;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigo() { return codigo == null ? id : codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getGremio() { return gremio; }
    public void setGremio(String gremio) { this.gremio = gremio; }

    public String getCategoriaSeccion() { return categoriaSeccion; }
    public void setCategoriaSeccion(String categoriaSeccion) { this.categoriaSeccion = categoriaSeccion; }
    public String getCategoria() { return categoriaSeccion; }
    public void setCategoria(String categoria) { this.categoriaSeccion = categoria; }

    public Integer getStockCritico() { return stockCritico; }
    public void setStockCritico(Integer stockCritico) { this.stockCritico = stockCritico; }

    public Long getVersion() { return version; }
}
