package modelo.entidades;

import java.io.Serializable;

public class LineaFactura implements Serializable {
    private final String descripcion;
    private final int cantidad;
    private final double precioUnitario;

    public LineaFactura(String descripcion,int cantidad,double precioUnitario){
        if(descripcion==null||descripcion.isBlank()||cantidad<=0||precioUnitario<0)
            throw new IllegalArgumentException("Línea inválida");
        this.descripcion=descripcion.trim();
        this.cantidad=cantidad;
        this.precioUnitario=precioUnitario;
    }
    public double calcularSubtotal(){
        return cantidad*precioUnitario;
    }
    public String getDescripcion(){
        return descripcion;
    }
    public int getCantidad(){
        return cantidad;
    }
    public double getPrecioUnitario(){
        return precioUnitario;
    }
}
