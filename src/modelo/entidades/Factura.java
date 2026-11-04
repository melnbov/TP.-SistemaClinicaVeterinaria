package modelo.entidades;
import java.io.Serializable;
import java.time.LocalDate; import java.util.*;
public class Factura implements Serializable {
    private final int numero;
    private final LocalDate fecha;
    private EstadoFactura estado;
    private final List<LineaFactura> lineas=new ArrayList<>();
    private Pago pago;
    private NotaCredito notaCredito;
    private final Consulta consulta;

    public Factura (int numero, LocalDate fecha){this(numero,fecha,null);}
    public Factura(int numero,LocalDate fecha,Consulta consulta){
        if(numero<=0||fecha==null)
            throw new IllegalArgumentException("Factura inválida");
        this.numero=numero;
        this.fecha=fecha;
        this.consulta=consulta;
        this.estado=EstadoFactura.EMITIDA;
    }
    private void validarEditable(){
        if(estado!=EstadoFactura.EMITIDA||pago!=null)
            throw new IllegalStateException("La factura ya no puede modificarse");
    }
    public void agregarLinea(LineaFactura linea){
        validarEditable();
        if(linea==null)
            throw new IllegalArgumentException("Línea obligatoria");
        lineas.add(linea);
    }
    public double calcularSubtotal(){
        double t=0;
        for(LineaFactura l:lineas)t+=l.calcularSubtotal();
        return t;
    }
    public double calcularDescuento(PlanSalud plan){
        if (plan == null){
            return 0;
        }
        return calcularSubtotal()* plan.getPorcentajeDescuento()/100.0;
    }
    public double calcularTotal(PlanSalud plan){
        return calcularSubtotal()- calcularDescuento(plan);}

    /** Total a pagar con el medio indicado: la factura delega la regla en el propio medio (sin instanceof). */
    public double calcularTotal(PlanSalud plan,MedioPago medio){
        if (medio == null) {
            throw new IllegalArgumentException("El medio de pago es obligatorio");
        }
        double porcentajePlan = plan == null ? 0 : plan.getPorcentajeDescuento();
        return medio.calcularTotal(calcularSubtotal(), porcentajePlan);
    }

    /**
     * Diferencia que produjo el medio de pago ya registrado en esta factura respecto del total con
     * plan de salud (positiva = recargo, negativa = descuento adicional). 0 si todavía no se pagó.
     */
    public double calcularAjusteMedioPago(PlanSalud plan){
        if (pago == null) {
            return 0;
        }
        return calcularTotal(plan, pago.getMedioPago()) - calcularTotal(plan);
    }
    public void registrarPago(Pago pago){
        validarEditable();
        if(pago==null)
            throw new IllegalArgumentException("Pago obligatorio");
        this.pago=pago;
        this.estado=EstadoFactura.PAGADA;
    }
    public NotaCredito anular(
            int numeroNota,String motivo){
        if(estado==EstadoFactura.ANULADA)
            throw new IllegalStateException("Factura ya anulada");
        double importe = pago == null ? calcularSubtotal() : pago.getImporte();
        notaCredito=new NotaCredito(numeroNota,LocalDate.now(),motivo,importe);
        estado=EstadoFactura.ANULADA;
        return notaCredito;
    }
    public int getNumero(){return numero;}

    public Consulta getConsulta(){return consulta;}

    public LocalDate getFecha(){
        return fecha;
    }
    public EstadoFactura getEstado(){
        return estado;}
    public List<LineaFactura> getLineas(){
        return Collections.unmodifiableList(lineas);
    }
    public Pago getPago(){
        return pago;
    }
    public NotaCredito getNotaCredito(){
        return notaCredito;}
}
