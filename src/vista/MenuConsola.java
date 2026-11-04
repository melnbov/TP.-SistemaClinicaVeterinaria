package vista;

import controlador.ControladorClinica;
import modelo.entidades.*;
import modelo.excepciones.ReglaNegocioException;
import modelo.servicios.ServicioTurnos;
import persistencia.RepositorioArchivo;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/** Interfaz por consola con menú: presenta opciones y delega las operaciones al controlador. */
public class MenuConsola {
    /** Se lanza cuando se termina la entrada estándar, para salir sin quedar en un bucle. */
    private static class FinDeEntrada extends RuntimeException { }

    private interface Accion { void ejecutar() throws ReglaNegocioException; }

    private final Scanner sc = new Scanner(System.in);
    private final ControladorClinica controlador;

    public MenuConsola() {
        // El archivo de datos se puede cambiar con -Dclinica.archivo=ruta
        String ruta = System.getProperty("clinica.archivo", "datos/clinica.dat");
        this.controlador = new ControladorClinica(new RepositorioArchivo(Paths.get(ruta)));
    }

    public void iniciar() {
        System.out.println(controlador.getMensajeInicio());
        try {
            int opcion;
            do {
                System.out.println("\nCLÍNICA VETERINARIA PATITAS");
                System.out.println("Cuidamos a los que más querés");
                System.out.println("\n  ¿Cómo querés ingresar?\n");
                System.out.println("  1. Dueño de mascota");
                System.out.println("  2. Administrador");
                System.out.println("  3. Recepcionista");
                System.out.println("  4. Veterinario");
                System.out.println("  0. Salir");
                opcion = leerEntero("\n  Opción: ");
                switch (opcion) {
                    case 1: accesoDueno(); break;
                    case 2: ingresoPersonal(Administrador.class, "administrador"); break;
                    case 3: ingresoPersonal(Recepcionista.class, "recepcionista"); break;
                    case 4: ingresoPersonal(Veterinario.class, "veterinario"); break;
                    case 0: System.out.println("Datos guardados. ¡Hasta luego!"); break;
                    default: System.out.println("Opción incorrecta.");
                }
            } while (opcion != 0);
        } catch (FinDeEntrada ex) {
            System.out.println("\nFin de la entrada. Los datos ya están guardados.");
        }
    }

    // ------------------------------------------------------------------ ingreso por perfil

    /** Acceso de dueños: pueden ingresar con su documento o registrarse por primera vez. */
    private void accesoDueno() {
        int op;
        do {
            System.out.println("\n--- Dueños de mascotas ---");
            System.out.println("1. Ingresar al sistema\n2. Registrarme\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ingresoDueno(); break;
                case 2: registroDueno(); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void ingresoDueno() {
        String documento = leerTexto("Tu documento (vacío para volver): ");
        if (documento.isEmpty()) return;
        Dueno dueno = controlador.buscarDueno(documento);
        if (dueno == null) {
            System.out.println("No encontramos ese documento. Si es tu primera vez, elegí «Registrarme».");
            return;
        }
        System.out.println("\n¡Hola, " + dueno.getNombre() + "!");
        menuDueno(dueno);
    }

    /** Registro de un dueño nuevo; al terminar entra directamente a su espacio. */
    private void registroDueno() {
        Dueno[] nuevo = new Dueno[1];
        ejecutar(() -> nuevo[0] = altaDueno(true));
        if (nuevo[0] != null) {
            System.out.println("\n¡Bienvenido/a, " + nuevo[0].getNombre() + "!");
            menuDueno(nuevo[0]);
        }
    }

    /** Ingreso del personal: el documento debe pertenecer a alguien con el perfil elegido. */
    private void ingresoPersonal(Class<? extends Personal> perfil, String nombrePerfil) {
        System.out.println("\n--- Ingreso de " + nombrePerfil + " ---");
        String documento = leerTexto("Documento (vacío para volver): ");
        if (documento.isEmpty()) return;
        Personal p = controlador.buscarPersonal(documento);
        if (p == null || !perfil.isInstance(p)) {
            System.out.println("Ese documento no corresponde a un " + nombrePerfil + " de la clínica.");
            return;
        }
        System.out.println("\nSesión iniciada: " + p.getNombreCompleto() + " (" + p.getRol() + ")");
        if (p instanceof Recepcionista) menuRecepcion();
        else if (p instanceof Veterinario) menuVeterinario((Veterinario) p);
        else menuAdministracion();
    }

    // ------------------------------------------------------------------ menús por perfil

    private void menuRecepcion() {
        int op;
        do {
            System.out.println("\n=== Recepción ===");
            System.out.println("1. Clientes y mascotas\n2. Turnos\n3. Cobros y facturas\n4. Vacunas y recordatorios\n0. Cerrar sesión");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: menuDuenosMascotas(); break;
                case 2: menuTurnos(); break;
                case 3: menuFacturacion(); break;
                case 4: menuVacunas(); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void menuVeterinario(Veterinario vet) {
        System.out.println("\n=== Consultorio de " + vet.getNombreCompleto() + " ===");
        mostrar(turnosProgramadosDe(vet));
        menuAtencion(vet);
    }

    private void menuAdministracion() {
        int op;
        do {
            System.out.println("\n=== Administración ===");
            System.out.println("1. Stock de medicamentos e insumos\n2. Reportes\n3. Veterinarios\n0. Cerrar sesión");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: menuStock(); break;
                case 2: menuReportes(); break;
                case 3: menuVeterinarios(); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void menuDueno(Dueno dueno) {
        int op;
        do {
            System.out.println("\n=== Mi espacio ===");
            System.out.println("1. Solicitar turno\n2. Mis turnos\n3. Cancelar o reprogramar un turno\n4. Mis mascotas\n5. Agregar una mascota\n0. Volver al inicio");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ejecutar(() -> solicitarTurno(dueno)); break;
                case 2: mostrar(turnosDe(dueno, false)); break;
                case 3: ejecutar(() -> modificarTurno(dueno)); break;
                case 4: mostrar(dueno.getMascotas()); break;
                case 5: ejecutar(() -> registrarMascotaDe(dueno)); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    // ------------------------------------------------------------------ acciones del dueño

    private void solicitarTurno(Dueno dueno) throws ReglaNegocioException {
        Mascota m = elegir("Tu mascota", dueno.getMascotas());
        if (m == null) return;
        Veterinario v = elegir("Veterinario", controlador.listarVeterinarios());
        if (v == null) return;
        System.out.println("Tipo: 1. Consulta general (30')  2. Vacunación (15')  3. Cirugía (120')");
        int t = leerEntero("Opción: ");
        if (t < 1 || t > 3) { System.out.println("Tipo incorrecto."); return; }
        if (controlador.requiereSena(dueno)) {
            System.out.println("Tenés 3 ausencias seguidas: para sacar un turno primero tenés que abonar una seña en recepción.");
            return;
        }
        LocalDateTime fecha = elegirFechaHora(v, TipoAtencion.values()[t - 1], null);
        if (fecha == null) return;
        Turno turno = controlador.asignarTurno(m, v, fecha, TipoAtencion.values()[t - 1], false);
        System.out.println("¡Turno confirmado!\n  " + turno);
    }

    private void modificarTurno(Dueno dueno) throws ReglaNegocioException {
        Turno t = elegir("Tu turno", turnosDe(dueno, true));
        if (t == null) return;
        System.out.println("1. Cancelar  2. Reprogramar");
        int op = leerEntero("Opción: ");
        if (op == 1) {
            double cargo = controlador.cancelarTurno(t.getNumero());
            if (cargo > 0) System.out.printf("Turno cancelado con menos de 2 h de anticipación: cargo del 20 %% = $%.2f%n", cargo);
            else System.out.println("Turno cancelado sin cargo.");
        } else if (op == 2) {
            LocalDateTime nueva = elegirFechaHora(t.getVeterinario(), t.getAtencion(), t);
            if (nueva == null) return;
            controlador.reprogramarTurno(t.getNumero(), nueva);
            System.out.println("Turno reprogramado.");
        }
    }

    /** Turnos de las mascotas del dueño; con soloProgramados se omiten los ya atendidos o cancelados. */
    private List<Turno> turnosDe(Dueno dueno, boolean soloProgramados) {
        List<Turno> r = new java.util.ArrayList<>();
        for (Turno t : controlador.listarTurnos()) {
            if (!dueno.equals(t.getMascota().getDuenoActual())) continue;
            if (soloProgramados && t.getEstado() != EstadoTurno.PROGRAMADO) continue;
            r.add(t);
        }
        return r;
    }

    // ------------------------------------------------------------------ submenús

    private void menuDuenosMascotas() {
        int op;
        do {
            System.out.println("\n--- Dueños y mascotas ---");
            System.out.println("1. Registrar dueño\n2. Listar dueños\n3. Registrar mascota\n4. Listar mascotas (por nombre)\n5. Transferir mascota a otro dueño\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ejecutar(this::registrarDueno); break;
                case 2: mostrar(controlador.listarDuenos()); break;
                case 3: ejecutar(this::registrarMascota); break;
                case 4: mostrar(controlador.listarMascotas()); break;
                case 5: ejecutar(this::transferirMascota); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void menuTurnos() {
        int op;
        do {
            System.out.println("\n--- Turnos ---");
            System.out.println("1. Asignar turno\n2. Atender una urgencia\n3. Reprogramar turno\n4. Cancelar turno\n5. Registrar ausencia\n6. Listar turnos\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ejecutar(this::asignarTurno); break;
                case 2: ejecutar(this::asignarUrgencia); break;
                case 3: ejecutar(this::reprogramarTurno); break;
                case 4: ejecutar(this::cancelarTurno); break;
                case 5: ejecutar(this::registrarAusencia); break;
                case 6: mostrar(controlador.listarTurnos()); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void menuAtencion(Veterinario vet) {
        int op;
        do {
            System.out.println("\n--- Atención clínica ---");
            System.out.println("1. Atender turno (abrir consulta)\n2. Registrar peso del día\n3. Registrar diagnóstico\n4. Indicar tratamiento\n5. Aplicar vacuna\n6. Registrar insumo utilizado\n7. Confirmar consulta (descuenta stock y factura)\n8. Nota rectificatoria\n9. Ver historia clínica de una mascota\n0. Cerrar sesión");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ejecutar(() -> atenderTurno(vet)); break;
                case 2: ejecutar(() -> { Consulta c = elegirConsultaAbierta(); if (c != null) controlador.registrarPeso(c, leerDecimal("Peso del día (kg): ")); }); break;
                case 3: ejecutar(() -> { Consulta c = elegirConsultaAbierta(); if (c != null) controlador.registrarDiagnostico(c, leerTexto("Diagnóstico: "), leerTexto("Observaciones (opcional): ")); }); break;
                case 4: ejecutar(this::indicarTratamiento); break;
                case 5: ejecutar(this::aplicarVacuna); break;
                case 6: ejecutar(this::agregarInsumo); break;
                case 7: ejecutar(this::confirmarConsulta); break;
                case 8: ejecutar(this::notaRectificatoria); break;
                case 9: ejecutar(this::verHistoria); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void menuStock() {
        int op;
        do {
            System.out.println("\n--- Stock ---");
            System.out.println("1. Listar productos\n2. Alta de medicamento\n3. Alta de insumo\n4. Ingresar lote\n5. Alertas de stock bajo\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: mostrar(controlador.listarProductos()); break;
                case 2: ejecutar(() -> { controlador.registrarMedicamento(leerTexto("Código: "), leerTexto("Nombre: "), leerEntero("Stock mínimo: "), leerTexto("Principio activo: "), leerTexto("Presentación: ")); System.out.println("Medicamento registrado."); }); break;
                case 3: ejecutar(() -> { controlador.registrarInsumo(leerTexto("Código: "), leerTexto("Nombre: "), leerEntero("Stock mínimo: "), leerTexto("Unidad de medida: ")); System.out.println("Insumo registrado."); }); break;
                case 4: ejecutar(() -> { controlador.ingresarLote(leerTexto("Código del producto: "), leerTexto("N° de lote: "), leerFecha("Vencimiento (aaaa-mm-dd): "), leerEntero("Cantidad: "), leerDecimal("Costo unitario: ")); System.out.println("Lote ingresado."); }); break;
                case 5: System.out.print(controlador.reporteStockBajo()); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void menuFacturacion() {
        int op;
        do {
            System.out.println("\n--- Facturación ---");
            System.out.println("1. Listar facturas\n2. Cobrar factura\n3. Anular factura (nota de crédito)\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: listarFacturas(); break;
                case 2: ejecutar(this::cobrarFactura); break;
                case 3: ejecutar(() -> { NotaCredito nc = controlador.anularFactura(leerEntero("N° de factura: "), leerTexto("Motivo: ")); System.out.printf("Factura anulada. Nota de crédito N° %d por $%.2f%n", nc.getNumero(), nc.getImporte()); }); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void menuVacunas() {
        int op;
        do {
            System.out.println("\n--- Vacunas y recordatorios ---");
            System.out.println("1. Ver catálogo de vacunas\n2. Enviar recordatorios (refuerzos a 7 días o vencidos)\n3. Vacunas próximas a vencer (30 días)\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: mostrar(controlador.listarCatalogoVacunas()); break;
                case 2: controlador.enviarRecordatoriosVacunas(); System.out.println("Recordatorios procesados."); break;
                case 3: System.out.print(controlador.reporteVacunasProximas()); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void menuReportes() {
        int op;
        do {
            System.out.println("\n--- Reportes del administrador ---");
            System.out.println("1. Turnos de un veterinario\n2. Todos los turnos\n3. Productos con stock bajo\n4. Vacunas próximas a vencer\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ejecutar(() -> { Veterinario v = elegir("Veterinario", controlador.listarVeterinarios()); if (v != null) System.out.print(controlador.reporteTurnos(v)); }); break;
                case 2: System.out.print(controlador.reporteTurnos(null)); break;
                case 3: System.out.print(controlador.reporteStockBajo()); break;
                case 4: System.out.print(controlador.reporteVacunasProximas()); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void menuVeterinarios() {
        int op;
        do {
            System.out.println("\n--- Veterinarios ---");
            System.out.println("1. Registrar veterinario\n2. Listar veterinarios\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ejecutar(this::registrarVeterinario); break;
                case 2: mostrar(controlador.listarVeterinarios()); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    // ------------------------------------------------------------------ acciones

    private void registrarDueno() throws ReglaNegocioException {
        altaDueno(false);
    }

    /** Pide los datos de un dueño nuevo. El documento se pide primero y se valida en el acto. */
    private Dueno altaDueno(boolean autoregistro) throws ReglaNegocioException {
        String documento = leerTexto("Documento: ");
        if (documento.isEmpty()) throw new ReglaNegocioException("El documento es obligatorio");
        if (controlador.buscarDueno(documento) != null) {
            throw new ReglaNegocioException("Ya existe un dueño con ese documento" + (autoregistro ? ": ingresá con «Ingresar al sistema»." : "."));
        }
        String nombre = leerTexto("Nombre: ");
        String apellido = leerTexto("Apellido: ");
        String telefono = leerTexto("Teléfono: ");
        String email = leerTexto("Email: ");
        PlanSalud plan = null;
        if (leerSiNo("¿Tiene plan de salud animal?")) {
            plan = new PlanSalud(leerTexto("Nombre del plan: "), leerDecimal("% de descuento (el enunciado indica 15): "));
        }
        Dueno d = controlador.registrarDueno(nombre, apellido, documento, telefono, email, plan);
        System.out.println("Dueño registrado: " + d);
        return d;
    }

    private void registrarMascota() throws ReglaNegocioException {
        Dueno dueno = elegir("Dueño", controlador.listarDuenos());
        if (dueno == null) return;
        registrarMascotaDe(dueno);
    }

    private void registrarMascotaDe(Dueno dueno) throws ReglaNegocioException {
        System.out.println("Especie: 1. Perro  2. Gato  3. Ave  4. Conejo");
        int esp = leerEntero("Opción: ");
        String nombre = leerTexto("Nombre: ");
        String raza = leerTexto("Raza: ");
        String sexo = leerTexto("Sexo (M/H): ");
        LocalDate nac = leerFecha("Fecha de nacimiento (aaaa-mm-dd): ");
        double peso = leerDecimal("Peso actual (kg): ");
        Mascota m;
        switch (esp) {
            case 1: m = controlador.nuevoPerro(nombre, raza, sexo, nac, peso, dueno, leerTexto("N° de chip: "), leerDecimal("Tamaño (cm): ")); break;
            case 2: m = controlador.nuevoGato(nombre, raza, sexo, nac, peso, dueno, leerSiNo("¿Vive en interior?") ? EstiloVida.INTERIOR : EstiloVida.EXTERIOR); break;
            case 3: m = controlador.nuevaAve(nombre, raza, sexo, nac, peso, dueno, leerSiNo("¿Puede volar?")); break;
            case 4: m = controlador.nuevoConejo(nombre, raza, sexo, nac, peso, dueno); break;
            default: System.out.println("Especie incorrecta."); return;
        }
        controlador.registrarMascota(m);
        System.out.println("Mascota registrada: " + m);
    }

    private void transferirMascota() throws ReglaNegocioException {
        Mascota m = elegir("Mascota", controlador.listarMascotas());
        if (m == null) return;
        controlador.transferirMascota(m, leerTexto("Documento del nuevo dueño: "));
        System.out.println("Mascota transferida. Conserva su historia clínica.");
    }

    private void registrarVeterinario() throws ReglaNegocioException {
        Veterinario v = controlador.registrarVeterinario(leerTexto("Nombre: "), leerTexto("Apellido: "),
                leerTexto("Documento: "), leerTexto("Teléfono: "), leerTexto("Matrícula: "), leerTexto("Especialidad: "),
                leerSiNo("¿Está de guardia?"), leerEntero("Días de atención desde el lunes (6 = lunes a sábado): "),
                leerHora("Hora de inicio (hh:mm): "), leerHora("Hora de fin (hh:mm): "));
        System.out.println("Veterinario registrado: " + v);
    }

    private void asignarTurno() throws ReglaNegocioException {
        Mascota m = elegir("Mascota", controlador.listarMascotas());
        if (m == null) return;
        Veterinario v = elegir("Veterinario", controlador.listarVeterinarios());
        if (v == null) return;
        System.out.println("Tipo: 1. Consulta general (30')  2. Vacunación (15')  3. Cirugía (120')");
        int t = leerEntero("Opción: ");
        if (t < 1 || t > 3) { System.out.println("Tipo incorrecto."); return; }
        boolean sena = false;
        if (controlador.requiereSena(m.getDuenoActual())) {
            System.out.println("El dueño tiene 3 ausencias seguidas: debe pagar una seña.");
            sena = leerSiNo("¿Abonó la seña?");
        }
        LocalDateTime fecha = elegirFechaHora(v, TipoAtencion.values()[t - 1], null);
        if (fecha == null) return;
        Turno turno = controlador.asignarTurno(m, v, fecha, TipoAtencion.values()[t - 1], sena);
        System.out.println("Turno asignado: " + turno);
    }

    private void asignarUrgencia() throws ReglaNegocioException {
        Mascota m = elegir("Mascota", controlador.listarMascotas());
        if (m == null) return;
        List<Veterinario> guardia = new java.util.ArrayList<>();
        for (Veterinario v : controlador.listarVeterinarios()) if (v.isDeGuardia()) guardia.add(v);
        Veterinario v = elegir("Veterinario de guardia", guardia);
        if (v == null) return;
        ServicioTurnos.ResultadoUrgencia r = controlador.asignarUrgencia(m, v);
        System.out.println("Urgencia asignada: " + r.getUrgente());
        for (Turno d : r.getDesplazados()) System.out.println("  Turno desplazado y notificado: " + d);
    }

    private void reprogramarTurno() throws ReglaNegocioException {
        Turno t = elegir("Turno a reprogramar", turnosProgramados());
        if (t == null) return;
        LocalDateTime nueva = elegirFechaHora(t.getVeterinario(), t.getAtencion(), t);
        if (nueva == null) return;
        controlador.reprogramarTurno(t.getNumero(), nueva);
        System.out.println("Turno reprogramado.");
    }

    private void cancelarTurno() throws ReglaNegocioException {
        Turno t = elegir("Turno a cancelar", turnosProgramados());
        if (t == null) return;
        double cargo = controlador.cancelarTurno(t.getNumero());
        if (cargo > 0) System.out.printf("Turno cancelado con menos de 2 h de anticipación: cargo del 20 %% = $%.2f%n", cargo);
        else System.out.println("Turno cancelado sin cargo.");
    }

    private void registrarAusencia() throws ReglaNegocioException {
        Turno t = elegir("Turno ausente", turnosProgramados());
        if (t == null) return;
        controlador.registrarAusencia(t.getNumero());
        System.out.println("Ausencia registrada.");
    }

    private void atenderTurno(Veterinario vet) throws ReglaNegocioException {
        Turno t = elegir("Tus turnos programados", turnosProgramadosDe(vet));
        if (t == null) return;
        Consulta c = controlador.atenderTurno(t.getNumero(), leerTexto("Motivo de la consulta: "));
        System.out.println("Consulta abierta: " + c + " (" + c.getMascota().getNombre() + ")");
    }

    private void indicarTratamiento() throws ReglaNegocioException {
        Consulta c = elegirConsultaAbierta();
        if (c == null) return;
        mostrar(controlador.listarProductos());
        controlador.indicarTratamiento(c, leerTexto("Código del medicamento: "), leerTexto("Dosis: "),
                leerTexto("Frecuencia: "), leerTexto("Duración: "));
        System.out.println("Tratamiento indicado (el stock se descuenta al confirmar la consulta).");
    }

    private void aplicarVacuna() throws ReglaNegocioException {
        Consulta c = elegirConsultaAbierta();
        if (c == null) return;
        mostrar(controlador.listarCatalogoVacunas());
        VacunaAplicada va = controlador.aplicarVacuna(c, leerTexto("Código de la vacuna: "), leerTexto("Lote de la vacuna: "));
        System.out.println("Vacuna aplicada: " + va);
    }

    private void agregarInsumo() throws ReglaNegocioException {
        Consulta c = elegirConsultaAbierta();
        if (c == null) return;
        mostrar(controlador.listarProductos());
        controlador.agregarInsumo(c, leerTexto("Código del producto: "), leerEntero("Cantidad: "));
        System.out.println("Insumo registrado en la consulta.");
    }

    private void confirmarConsulta() throws ReglaNegocioException {
        Consulta c = elegirConsultaAbierta();
        if (c == null) return;
        Factura f = controlador.confirmarConsulta(c);
        System.out.println("Consulta confirmada y cerrada.");
        mostrarFactura(f);
        List<Producto> bajos = controlador.productosStockBajo();
        for (Producto p : bajos) System.out.println("ALERTA DE REPOSICIÓN: " + p.getNombre() + " (stock " + p.calcularStock() + ", mínimo " + p.getStockMinimo() + ")");
    }

    private void notaRectificatoria() throws ReglaNegocioException {
        Mascota m = elegir("Mascota", controlador.listarMascotas());
        if (m == null) return;
        Consulta c = elegir("Consulta", m.getHistoriaClinica());
        if (c == null) return;
        Veterinario firmante = elegir("Veterinario firmante", controlador.listarVeterinarios());
        if (firmante == null) return;
        controlador.agregarNotaRectificatoria(c, leerTexto("Texto de la nota: "), firmante);
        System.out.println("Nota rectificatoria agregada (la historia clínica no se modifica).");
    }

    private void verHistoria() throws ReglaNegocioException {
        Mascota m = elegir("Mascota", controlador.listarMascotas());
        if (m == null) return;
        System.out.println("Historia clínica de " + m.getNombre() + " (" + m.getEspecie() + ")");
        System.out.println("Dueños: " + m.getHistorialDuenos());
        if (m.getHistoriaClinica().isEmpty()) System.out.println("(sin consultas)");
        for (Consulta c : m.getHistoriaClinica()) {
            System.out.println("- " + c + (c.estaCerrada() ? " [cerrada]" : " [abierta]"));
            if (c.getDiagnostico() != null) System.out.println("    Diagnóstico: " + c.getDiagnostico() + " | Peso: " + c.getPesoDelDia() + " kg");
            for (Tratamiento t : c.getTratamientos()) System.out.println("    Tratamiento: " + t);
            for (VacunaAplicada v : c.getVacunasAplicadas()) System.out.println("    Vacuna: " + v);
            for (NotaRectificatoria n : c.getNotasRectificatorias()) System.out.println("    " + n);
        }
    }

    private void listarFacturas() {
        if (controlador.listarFacturas().isEmpty()) System.out.println("(no hay facturas)");
        for (Factura f : controlador.listarFacturas()) {
            System.out.printf("Factura N° %d | %s | %s | subtotal $%.2f%n", f.getNumero(), f.getFecha(), f.getEstado(), f.calcularSubtotal());
        }
    }

    private void cobrarFactura() throws ReglaNegocioException {
        Factura f = controlador.buscarFactura(leerEntero("N° de factura: "));
        mostrarFactura(f);
        System.out.println("Medio de pago: 1. Efectivo (10% desc.)  2. Débito  3. Crédito en cuotas (8% recargo)");
        int op = leerEntero("Opción: ");
        MedioPago medio;
        switch (op) {
            case 1: medio = new Efectivo(); break;
            case 2: medio = new Debito(); break;
            case 3: medio = new Credito(leerEntero("Cantidad de cuotas: ")); break;
            default: System.out.println("Medio incorrecto."); return;
        }
        System.out.printf("Total a pagar: $%.2f%n", controlador.calcularTotal(f, medio));
        controlador.pagarFactura(f.getNumero(), medio);
        System.out.println("Pago registrado.");
    }

    private void mostrarFactura(Factura f) {
        System.out.println("Factura N° " + f.getNumero() + " (" + f.getFecha() + ") - " + f.getEstado());
        for (LineaFactura l : f.getLineas()) System.out.printf("   %-28s x%d  $%.2f%n", l.getDescripcion(), l.getCantidad(), l.calcularSubtotal());
        System.out.printf("   Subtotal: $%.2f%n", f.calcularSubtotal());
        PlanSalud plan = controlador.planDe(f);
        if (plan != null) System.out.println("   Plan de salud: " + plan.getNombre() + " (" + plan.getPorcentajeDescuento() + "% de descuento)");
    }

    // ------------------------------------------------------------------ utilidades de interfaz

    private static final String[] DIAS = {"lun", "mar", "mié", "jue", "vie", "sáb", "dom"};
    private static final java.time.format.DateTimeFormatter FORMATO_HORA = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    private List<Turno> turnosProgramados() {
        List<Turno> r = new java.util.ArrayList<>();
        for (Turno t : controlador.listarTurnos()) if (t.getEstado() == EstadoTurno.PROGRAMADO) r.add(t);
        return r;
    }

    private List<Turno> turnosProgramadosDe(Veterinario vet) {
        List<Turno> r = new java.util.ArrayList<>();
        for (Turno t : controlador.listarTurnosDe(vet)) if (t.getEstado() == EstadoTurno.PROGRAMADO) r.add(t);
        return r;
    }

    /**
     * Elige día y horario sin escribir fechas: primero los días en que atiende el veterinario (próximos 14 días),
     * después los horarios libres de ese día, mostrando también los ya reservados.
     * @param excluir turno que se está reprogramando (su horario actual cuenta como libre); puede ser null
     * @return el horario elegido o null si se cancela
     */
    private LocalDateTime elegirFechaHora(Veterinario v, TipoAtencion tipo, Turno excluir) {
        while (true) {
            List<LocalDate> dias = controlador.diasDeAtencion(v, 14);
            if (dias.isEmpty()) { System.out.println("El veterinario no tiene días de atención cargados."); return null; }
            System.out.println("\nDías de atención de " + v.getNombreCompleto() + ":");
            for (int i = 0; i < dias.size(); i++) {
                LocalDate d = dias.get(i);
                System.out.printf("  %2d. %s %02d/%02d%n", i + 1, DIAS[d.getDayOfWeek().getValue() - 1], d.getDayOfMonth(), d.getMonthValue());
            }
            int n = leerEntero("Elegí el día (0 para cancelar): ");
            if (n == 0) return null;
            if (n < 1 || n > dias.size()) { System.out.println("Opción incorrecta."); continue; }
            LocalDate dia = dias.get(n - 1);

            List<Turno> reservados = controlador.turnosReservados(v, dia);
            System.out.println("\nHorarios reservados ese día:");
            boolean hay = false;
            for (Turno t : reservados) {
                if (t == excluir) continue;
                System.out.println("  " + t.getInicio().format(FORMATO_HORA) + " a " + t.getFin().format(FORMATO_HORA));
                hay = true;
            }
            if (!hay) System.out.println("  (ninguno)");
            List<LocalDateTime> libres = controlador.horariosLibres(v, dia, tipo, excluir);
            if (libres.isEmpty()) { System.out.println("No quedan horarios libres ese día. Probá con otro."); continue; }
            System.out.println("Horarios disponibles (" + tipo.getDuracionMinutos() + " min):");
            for (int i = 0; i < libres.size(); i++) System.out.printf("  %2d. %s%n", i + 1, libres.get(i).format(FORMATO_HORA));
            int h = leerEntero("Elegí el horario (0 para elegir otro día): ");
            if (h >= 1 && h <= libres.size()) return libres.get(h - 1);
        }
    }

    private Consulta elegirConsultaAbierta() throws ReglaNegocioException {
        List<Consulta> abiertas = controlador.consultasAbiertas();
        if (abiertas.isEmpty()) { System.out.println("No hay consultas abiertas. Primero atendé un turno."); return null; }
        return elegir("Consulta abierta", abiertas);
    }

    /** Muestra una lista numerada y devuelve el elemento elegido (o null si no hay o se cancela). */
    private <T> T elegir(String titulo, List<T> lista) {
        if (lista.isEmpty()) { System.out.println("No hay elementos para elegir (" + titulo + ")."); return null; }
        System.out.println(titulo + ":");
        for (int i = 0; i < lista.size(); i++) System.out.println("  " + (i + 1) + ". " + lista.get(i));
        int n = leerEntero("Número (0 para cancelar): ");
        if (n < 1 || n > lista.size()) return null;
        return lista.get(n - 1);
    }

    private void mostrar(List<?> lista) {
        if (lista.isEmpty()) System.out.println("(sin resultados)");
        lista.forEach(System.out::println);
    }

    /** Ejecuta una acción y muestra el motivo si una regla de negocio o una validación la rechaza. */
    private void ejecutar(Accion accion) {
        try {
            accion.ejecutar();
        } catch (FinDeEntrada ex) {
            throw ex;
        } catch (ReglaNegocioException | IllegalArgumentException | IllegalStateException ex) {
            System.out.println("No se pudo completar: " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.out.println("Error inesperado (" + ex + "). Los datos guardados no se modificaron.");
        }
    }

    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        if (!sc.hasNextLine()) throw new FinDeEntrada();
        return sc.nextLine().trim();
    }

    private int leerEntero(String mensaje) {
        while (true) {
            try { return Integer.parseInt(leerTexto(mensaje)); }
            catch (NumberFormatException e) { System.out.println("Ingresá un número entero válido."); }
        }
    }

    private double leerDecimal(String mensaje) {
        while (true) {
            try { return Double.parseDouble(leerTexto(mensaje).replace(',', '.')); }
            catch (NumberFormatException e) { System.out.println("Ingresá un número válido."); }
        }
    }

    private boolean leerSiNo(String mensaje) {
        while (true) {
            String r = leerTexto(mensaje + " (s/n): ").toLowerCase();
            if (r.equals("s") || r.equals("si") || r.equals("sí")) return true;
            if (r.equals("n") || r.equals("no")) return false;
            System.out.println("Respondé s o n.");
        }
    }

    private LocalDate leerFecha(String mensaje) {
        while (true) {
            try { return LocalDate.parse(leerTexto(mensaje)); }
            catch (DateTimeParseException e) { System.out.println("Formato inválido. Ejemplo: 2026-11-20"); }
        }
    }

    private LocalDateTime leerFechaHora(String mensaje) {
        while (true) {
            try { return LocalDateTime.parse(leerTexto(mensaje).replace(' ', 'T')); }
            catch (DateTimeParseException e) { System.out.println("Formato inválido. Ejemplo: 2026-11-20 10:30"); }
        }
    }

    private LocalTime leerHora(String mensaje) {
        while (true) {
            try { return LocalTime.parse(leerTexto(mensaje)); }
            catch (DateTimeParseException e) { System.out.println("Formato inválido. Ejemplo: 09:00"); }
        }
    }
}
