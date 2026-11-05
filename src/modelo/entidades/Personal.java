
package modelo.entidades;

public abstract class Personal implements java.io.Serializable {
    //atributos
    protected String nombre;
    protected String apellido;
    protected String documento;
    protected String telefono;
    
    //constructor

    public Personal(String nombre, String apellido, String documento, String telefono) {
        if (nombre == null || nombre.isBlank() || apellido == null || apellido.isBlank()){
            throw new IllegalArgumentException("Nombre y apellido son obligatorios");
        }
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.telefono = telefono;
    }
    
    //getters

    public String getNombreCompleto(){
        return nombre + " " + apellido;
    }
    public String getDocumento() {
        return documento;
    }

    public String getTelefono() {
        return telefono;
    }
    
    public abstract String getRol();
    
}
