package modelo.entidades;

import java.io.Serializable;

public class Debito implements Serializable, MedioPago {
    public double calcularRecargo(double subtotal){
        return 0;
    }
    public String getNombre(){
        return "Débito";
    }
}
