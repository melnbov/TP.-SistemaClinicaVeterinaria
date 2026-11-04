package modelo.servicios;
import modelo.entidades.Dueno;
public interface Notificador {
    void enviarAviso(Dueno dueno,String mensaje);
}

