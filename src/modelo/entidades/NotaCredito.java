package modelo.entidades;
import java.io.Serializable;
import java.time.LocalDate;
public class NotaCredito implements Serializable {
    private final int numero;
    private final LocalDate fecha;
    private final String motivo;
    private final double importe;

    public NotaCredito(int numero,LocalDate fecha,String motivo,double importe){
        if(numero<=0||fecha==null||motivo==null||motivo.isBlank()||importe<0)
            throw new IllegalArgumentException("Nota de crédito inválida");
        this.numero=numero;
        this.fecha=fecha;
        this.motivo=motivo.trim();
        this.importe=importe;
    }
    public int getNumero(){
        return numero;
    }
    public LocalDate getFecha(){
        return fecha;
    }
    public String getMotivo(){
        return motivo;
    }
    public double getImporte(){
        return importe;
    }
}

