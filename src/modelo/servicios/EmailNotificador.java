package modelo.servicios;
import modelo.entidades.Dueno;

public class EmailNotificador implements Notificador {
    public void enviarAviso(Dueno dueno,String mensaje){
        if(dueno==null||mensaje==null||mensaje.isBlank())
            throw new IllegalArgumentException("Dueño y mensaje son obligatorios");
        System.out.println("Email enviado a "+ dueno.getEmail()+": "+ mensaje);}
}
