
package modelo.entidades;

public class Recepcionista extends Personal {

    public Recepcionista(String nombre, String apellido, String documento, String telefono) {
        super(nombre, apellido, documento, telefono);
    }

    @Override
    public String getRol() {
        return "Recepcionista";
    }
    
    
}
