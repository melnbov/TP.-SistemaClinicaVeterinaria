
package modelo.entidades;

public class Administrador extends Personal{
    
    //constructor
    public Administrador(String nombre, String apellido, String documento, String telefono) {
        super(nombre, apellido, documento, telefono);
    }

    @Override
    public String getRol() {
        return "Administrador";
    }
    
    
}
