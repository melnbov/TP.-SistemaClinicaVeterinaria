package modelo.entidades;

import java.io.Serializable;

public class PlanSalud implements Serializable {
    private final String nombre;
    private final double porcentajeDescuento;

    public PlanSalud(String nombre,double porcentajeDescuento){
        if(nombre==null||nombre.isBlank()||porcentajeDescuento<0||porcentajeDescuento>100)
            throw new IllegalArgumentException("Plan inválido");
        this.nombre=nombre.trim();
        this.porcentajeDescuento=porcentajeDescuento;
    }
    public String getNombre(){
        return nombre;
    }
    public double getPorcentajeDescuento(){
        return porcentajeDescuento;
    }
}

