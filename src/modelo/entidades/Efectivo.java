package modelo.entidades;

import java.io.Serializable;

public class Efectivo implements Serializable,MedioPago {
    public double calcularRecargo(double subtotal){
        return -subtotal*0.10;
    }
    public String getNombre(){
        return "Efectivo";
    }
}

