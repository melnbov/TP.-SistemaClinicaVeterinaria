package modelo.entidades;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Producto implements Serializable {
    private final String codigo;
    private final String nombre;
    private final int stockMinimo;
    private final List<LoteProducto> lotes = new ArrayList<>();

    protected Producto(String codigo, String nombre, int stockMinimo) {
        if (codigo == null || codigo.isBlank())
            throw new IllegalArgumentException("El código es obligatorio");
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (stockMinimo < 0)
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        this.codigo = codigo.trim();
        this.nombre = nombre.trim();
        this.stockMinimo = stockMinimo;
    }
    public int calcularStock() {
        int total = 0;
        for (LoteProducto lote : lotes)
            if (!lote.estaVencido())
                total += lote.getCantidadDisponible();
        return total;
    }
    public void agregarLote(LoteProducto lote) {
        if (lote == null)
            throw new IllegalArgumentException("El lote es obligatorio");
        if (lotes.contains(lote))
            throw new IllegalArgumentException("El lote ya está asociado al producto");
        lotes.add(lote);
    }
    public void quitarLote(LoteProducto lote) {
        lotes.remove(lote);
    }
    public boolean tieneStockBajo() {
        return calcularStock() < stockMinimo;
    }
    public String getCodigo() {
        return codigo;
    }
    public String getNombre() {
        return nombre;
    }
    public int getStockMinimo() {
        return stockMinimo;
    }
    public List<LoteProducto> getLotes() {
        return Collections.unmodifiableList(lotes);
    }
    @Override
    public String toString() {
        return codigo + " - " + nombre + " (stock: " + calcularStock() + ")";
    }
}

