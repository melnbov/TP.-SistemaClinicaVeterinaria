package persistencia;

import java.io.*;
import java.nio.file.*;

/** Lee y escribe el estado de la clínica en un archivo, para que sobreviva entre ejecuciones. */
public class RepositorioArchivo {
    private final Path archivo;

    public RepositorioArchivo(Path archivo) {
        if (archivo == null) throw new IllegalArgumentException("El archivo es obligatorio");
        this.archivo = archivo;
    }

    public Path getArchivo() { return archivo; }

    public boolean existe() { return Files.exists(archivo); }

    /** @return los datos guardados, o null si todavía no existe el archivo */
    public DatosClinica cargar() throws IOException {
        if (!existe()) return null;
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(archivo)))) {
            return (DatosClinica) in.readObject();
        } catch (ClassNotFoundException | ClassCastException ex) {
            throw new IOException("El archivo de datos no tiene un formato válido: " + ex.getMessage(), ex);
        }
    }

    /**
     * Guarda escribiendo primero un archivo temporal y reemplazando después el definitivo,
     * así un corte a mitad de escritura no deja el archivo de datos corrupto.
     */
    public void guardar(DatosClinica datos) throws IOException {
        if (datos == null) throw new IllegalArgumentException("Los datos son obligatorios");
        Path carpeta = archivo.toAbsolutePath().getParent();
        if (carpeta != null) Files.createDirectories(carpeta);
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temporal)))) {
                out.writeObject(datos);
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal); // si todo salió bien ya no existe
        }
    }
}
