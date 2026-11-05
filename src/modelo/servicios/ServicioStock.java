package modelo.servicios;

import modelo.entidades.*;

import java.io.Serializable;
import java.time.LocalDate; import java.util.*;
public class ServicioStock implements Serializable {
    private final List<Producto> productos=new ArrayList<>();
    public void registrarProducto(Producto p){
        if(p==null)
            throw new IllegalArgumentException("Producto obligatorio");
        if(buscarProducto(p.getCodigo())!=null)
            throw new IllegalArgumentException("Ya existe el código");
        productos.add(p);
    }
    public Producto buscarProducto(String codigo){
        for(Producto p:productos)
            if(p.getCodigo().equalsIgnoreCase(codigo))
                return p;
        return null;
    }
    public void ingresarLote(String codigo,LoteProducto lote){
        Producto p=buscarProducto(codigo);
        if(p==null)
            throw new IllegalArgumentException("Producto inexistente");
        p.agregarLote(lote);
    }
    public List<UsoInsumo> descontarPorVencimiento(String codigo,int cantidad){
        Producto p=buscarProducto(codigo);
        if(p==null)
            throw new IllegalArgumentException("Producto inexistente");
        if(cantidad<=0)
            throw new IllegalArgumentException("Cantidad inválida");
        List<LoteProducto> lotes=new ArrayList<>(p.getLotes());
        lotes.sort(Comparator.comparing(LoteProducto::getFechaVencimiento));
        int disponible=p.calcularStock();
        if(disponible<cantidad)
            throw new IllegalStateException("Stock insuficiente");
        List<UsoInsumo> usos=new ArrayList<>();
        int restante=cantidad;
        for(LoteProducto l:lotes){if(restante==0)break;
            if(l.estaVencido()||l.getCantidadDisponible()==0)continue;
            int tomar=Math.min(restante,l.getCantidadDisponible());
            usos.add(new UsoInsumo(l,tomar));
            restante-=tomar;
        }
        return usos;
    }
    /** @return true si hay al menos {@code cantidad} unidades vigentes (no vencidas) del producto */
    public boolean hayStock(String codigo,int cantidad){
        Producto p=buscarProducto(codigo);
        return p!=null&&cantidad>0&&p.calcularStock()>=cantidad;
    }
    public List<Producto> consultarStockBajo(){
        List<Producto> r=new ArrayList<>();
        for(Producto p:productos)
            if(p.tieneStockBajo())
                r.add(p);
        return Collections.unmodifiableList(r);
    }
    public List<Producto> getProductos(){
        return Collections.unmodifiableList(productos);
    }
}