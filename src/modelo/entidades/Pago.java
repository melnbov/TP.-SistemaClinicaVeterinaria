package modelo.entidades;
import java.io.Serializable;
import java.time.LocalDate;
public class Pago implements Serializable {
    private final LocalDate fecha;
    private final double importe;
    private final MedioPago medioPago;

    public Pago(LocalDate fecha,double importe,MedioPago medioPago){
        if(fecha==null||importe<0||medioPago==null)
            throw new IllegalArgumentException("Pago inválido");
        this.fecha=fecha;
        this.importe=importe;
        this.medioPago=medioPago;
    }
    public LocalDate getFecha(){
        return fecha;
    }
    public double getImporte(){
        return importe;
    }
    public MedioPago getMedioPago(){
        return medioPago;
    }
}
