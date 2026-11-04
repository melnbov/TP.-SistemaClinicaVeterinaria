package modelo.servicios;
import modelo.entidades.Producto;
import java.util.List;
public class ReporteStockBajo implements Reporte {
    private final List<Producto> productos;
    public ReporteStockBajo(List<Producto> productos){
        this.productos=productos;}
    public String generarReporte(){
        StringBuilder s=new StringBuilder("REPORTE DE STOCK BAJO\n");
        for(Producto p:productos)
            if(p.tieneStockBajo())s.append(p.getNombre()).append(" - stock: ").append(p.calcularStock()).append(" - mínimo: ").append(p.getStockMinimo()).append('\n');
        return s.toString();}
}
