package modelo.servicios;
import modelo.entidades.Producto;

/** Reporte de stock bajo: no repite el filtro, lo consulta a {@link ServicioStock}. */
public class ReporteStockBajo implements Reporte {
    private final ServicioStock servicioStock;
    public ReporteStockBajo(ServicioStock servicioStock){
        if(servicioStock==null)
            throw new IllegalArgumentException("ServicioStock obligatorio");
        this.servicioStock=servicioStock;}
    public String generarReporte(){
        StringBuilder s=new StringBuilder("REPORTE DE STOCK BAJO\n");
        for(Producto p:servicioStock.consultarStockBajo())
            s.append(p.getNombre()).append(" - stock: ").append(p.calcularStock()).append(" - mínimo: ").append(p.getStockMinimo()).append('\n');
        return s.toString();}
}
