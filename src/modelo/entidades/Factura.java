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
    public double calcularRecargo(){
        return 0;
    }
    public double calcularTotal(PlanSalud plan){
        return calcularSubtotal()- calcularDescuento(plan);}

    public double calcularTotal(PlanSalud plan,MedioPago medio){
        if (medio == null) {
            throw new IllegalArgumentException("El medio de pago es obligatorio");
        }
        double subtotal = calcularSubtotal();
        double porcentajePlan = plan == null
                ? 0 : plan.getPorcentajeDescuento();

        // El descuento del plan y el de efectivo no se acumulan.
        double porcentajeDescuento = porcentajePlan;
        if (medio instanceof Efectivo) {
            porcentajeDescuento = Math.max(porcentajePlan, 10);
        }
        double total = subtotal - (subtotal * porcentajeDescuento / 100.0);

        // Para debito el recargo es 0 y para credito es 8%.
        // Efectivo ya fue contemplado en el descuento mayor, por eso no se acumula.
        if (!(medio instanceof Efectivo)) {
            total += medio.calcularRecargo(total);
        }

        return Math.max(0, total);
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
