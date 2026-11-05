package modelo.entidades;

import java.io.Serializable;

public class DetalleCompra implements Serializable {
    private final Producto producto;
    private final LoteProducto lote;
    private final int cantidad;
    private final double costoUnitario;
    public DetalleCompra(Producto producto, LoteProducto lote, int cantidad, double costoUnitario) {
        if(producto==null || lote==null)
            throw new IllegalArgumentException("Producto y lote son obligatorios");
        if(cantidad<=0 || costoUnitario<0)
            throw new IllegalArgumentException("Cantidad o costo inválido");
        this.producto=producto;
        this.lote=lote;
        this.cantidad=cantidad;
        this.costoUnitario=costoUnitario;
    }
    public Producto getProducto(){
        return producto;
    }
    public LoteProducto getLote(){
        return lote;
    }
    public int getCantidad(){
        return cantidad;
    }
    public double getCostoUnitario(){
        return costoUnitario;
    }
    public double calcularSubtotal(){
        return cantidad*costoUnitario;
    }
}
