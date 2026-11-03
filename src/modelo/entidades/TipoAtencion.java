
package modelo.entidades;

public enum TipoAtencion {
    CONSULTA_GENERAL(30),
    VACUNACION(15),
    CIRUGIA(120);
    
    private final int duracionMinutos;
    
    TipoAtencion(int duracionMinutos){
        this.duracionMinutos=duracionMinutos;
    }
    
    public int getDuracionMinutos(){
        return duracionMinutos;
    }
}
