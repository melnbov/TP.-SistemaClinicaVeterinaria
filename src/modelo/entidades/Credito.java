package modelo.entidades;

import java.io.Serializable;

public class Credito implements Serializable, MedioPago {
    private final int cantidadCuotas;

    public Credito(int cantidadCuotas){
        if(cantidadCuotas<=0)
            throw new IllegalArgumentException("Las cuotas deben ser positivas");
        this.cantidadCuotas=cantidadCuotas;}

    public double calcularRecargo(double subtotal){return subtotal*0.08;}
    public String getNombre(){return "Crédito";}
    public int getCantidadCuotas(){
        return cantidadCuotas;}
}

