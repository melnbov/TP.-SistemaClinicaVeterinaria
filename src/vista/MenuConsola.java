package vista;

import controlador.ControladorClinica;
import modelo.entidades.*;
import modelo.excepciones.ReglaNegocioException;
import modelo.servicios.Notificador;
import modelo.servicios.Reporte;
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

    private static final double MONTO_SENA = TipoAtencion.CONSULTA_GENERAL.getPrecio() * 0.20; // SUPUESTO: la propuesta no fija el importe.
    private final Scanner sc = new Scanner(System.in);
    private final ControladorClinica controlador;

    public MenuConsola(Notificador notificador) {
        // El archivo de datos se puede cambiar con -Dclinica.archivo=ruta
        String ruta = System.getProperty("clinica.archivo", "datos/clinica.dat");
        this.controlador = new ControladorClinica(new RepositorioArchivo(Paths.get(ruta)), notificador);
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

    /**
     * El dueño ingresa con su documento. Según el caso de uso, el alta de dueños
     * la realiza recepción; el dueño no puede crear ni modificar sus datos desde aquí.
     */
    private void accesoDueno() {
        ingresoDueno();
    }

    private void ingresoDueno() {
        String documento = leerTexto("Tu documento (vacío para volver): ");
        if (documento.isEmpty()) return;
        Dueno dueno = controlador.buscarDueno(documento);
        if (dueno == null) {
            System.out.println("No encontramos ese documento. Solicitá a recepción que registre tus datos y los de tu mascota.");
            return;
        }
        System.out.println("\n¡Hola, " + dueno.getNombre() + "!");
        menuDueno(dueno);
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
            System.out.println("\n=== Portal del dueño ===");
            System.out.println("1. Solicitar turno\n2. Consultar mis turnos\n3. Cancelar o reprogramar turno\n4. Consultar mis mascotas\n0. Cerrar sesión");
            op = leerOpcionEntre("Opción: ", 0, 4);
            switch (op) {
                case 1: ejecutar(() -> solicitarTurno(dueno)); break;
                case 2: mostrar(turnosDe(dueno, false)); break;
                case 3: ejecutar(() -> modificarTurno(dueno)); break;
                case 4: mostrar(controlador.listarMascotasDe(dueno)); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    // ------------------------------------------------------------------ acciones del dueño

    private void solicitarTurno(Dueno dueno) throws ReglaNegocioException {
        Mascota m = elegir("Tu mascota", controlador.listarMascotasDe(dueno));
        if (m == null) return;
        Veterinario v = elegir("Veterinario", controlador.listarVeterinarios());
        if (v == null) return;
        System.out.println("Tipo: 1. Consulta general (30')  2. Vacunación (15')  3. Cirugía (120')");
        int t = leerOpcionEntre("Opción: ", 1, 3);
        if (controlador.requiereSena(dueno)) {
            System.out.printf("Tenés tres ausencias consecutivas. Para solicitar un nuevo turno, primero debés abonar una seña de $%.2f en recepción. Una vez confirmada, recepción podrá asignarte el turno.%n", MONTO_SENA);
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
        int op = leerOpcionEntre("Opción: ", 1, 2);
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
            System.out.println("1. Asignar turno\n2. Asignar urgencia\n3. Reprogramar turno\n4. Cancelar turno\n5. Registrar ausencia\n6. Listar todos los turnos\n7. Consultar turnos por veterinario\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ejecutar(this::asignarTurno); break;
                case 2: ejecutar(this::asignarUrgencia); break;
                case 3: ejecutar(this::reprogramarTurno); break;
                case 4: ejecutar(this::cancelarTurno); break;
                case 5: ejecutar(this::registrarAusencia); break;
                case 6: mostrar(controlador.listarTurnos()); break;
                case 7: consultarTurnosVeterinario(); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void consultarTurnosVeterinario() {
        Veterinario veterinario = elegir("Veterinario", controlador.listarVeterinarios());
        if (veterinario == null) return;
        List<Turno> turnos = controlador.listarTurnosDe(veterinario);
        if (turnos.isEmpty()) {
            System.out.println("Este veterinario todavía no tiene turnos registrados.");
            return;
        }
        System.out.println("\nTurnos de " + veterinario.getNombreCompleto() + ":");
        mostrar(turnos);
    }

    private void menuAtencion(Veterinario vet) {
        int op;
        do {
            System.out.println("\n--- Atención clínica ---");
            System.out.println("1. Atender turno (abrir consulta)\n2. Registrar peso del día\n3. Registrar diagnóstico\n4. Indicar tratamiento\n5. Aplicar vacuna\n6. Registrar insumo utilizado\n7. Confirmar consulta (descuenta stock y factura)\n8. Nota rectificatoria\n9. Ver historia clínica de una mascota\n0. Cerrar sesión");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ejecutar(() -> atenderTurno(vet)); break;
                case 2: ejecutar(() -> { Consulta c = elegirConsultaAbierta(vet); if (c != null) controlador.registrarPeso(c, leerDecimalPositivo("Peso del día (kg): ")); }); break;
                case 3: ejecutar(() -> { Consulta c = elegirConsultaAbierta(vet); if (c != null) controlador.registrarDiagnostico(c, leerTextoObligatorio("Diagnóstico: "), leerTexto("Observaciones (opcional): ")); }); break;
                case 4: ejecutar(() -> indicarTratamiento(vet)); break;
                case 5: ejecutar(() -> aplicarVacuna(vet)); break;
                case 6: ejecutar(() -> agregarInsumo(vet)); break;
                case 7: ejecutar(() -> confirmarConsulta(vet)); break;
                case 8: ejecutar(() -> notaRectificatoria(vet)); break;
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
                case 2: ejecutar(() -> { controlador.registrarMedicamento(leerTexto("Código: "), leerTexto("Nombre: "), leerEnteroNoNegativo("Stock mínimo: "), leerTexto("Principio activo: "), leerTexto("Presentación: ")); System.out.println("Medicamento registrado."); }); break;
                case 3: ejecutar(() -> { controlador.registrarInsumo(leerTexto("Código: "), leerTexto("Nombre: "), leerEnteroNoNegativo("Stock mínimo: "), leerTexto("Unidad de medida: ")); System.out.println("Insumo registrado."); }); break;
                case 4: ejecutar(() -> { controlador.ingresarLote(leerTexto("Código del producto: "), leerTexto("N° de lote: "), leerFecha("Vencimiento (aaaa-mm-dd): "), leerEnteroPositivo("Cantidad: "), leerDecimalNoNegativo("Costo unitario: ")); System.out.println("Lote ingresado."); }); break;
                case 5: emitir(controlador.reporteStockBajo()); break;
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
                case 3: emitir(controlador.reporteVacunasProximas()); break;
                case 0: break;
                default: System.out.println("Opción incorrecta.");
            }
        } while (op != 0);
    }

    private void emitir(Reporte reporte) {
        System.out.print(controlador.emitirReporte(reporte));
    }

    private void menuReportes() {
        int op;
        do {
            System.out.println("\n--- Reportes del administrador ---");
            System.out.println("1. Turnos de un veterinario\n2. Todos los turnos\n3. Productos con stock bajo\n4. Vacunas próximas a vencer\n0. Volver");
            op = leerEntero("Opción: ");
            switch (op) {
                case 1: ejecutar(() -> { Veterinario v = elegir("Veterinario", controlador.listarVeterinarios()); if (v != null) emitir(controlador.reporteTurnos(v)); }); break;
                case 2: emitir(controlador.reporteTurnos(null)); break;
                case 3: emitir(controlador.reporteStockBajo()); break;
                case 4: emitir(controlador.reporteVacunasProximas()); break;
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
        altaDueno();
    }

    /** El alta de dueños corresponde a recepción, como indica el caso de uso. */
    private Dueno altaDueno() throws ReglaNegocioException {
        String documento;
        while (true) {
            documento = leerTextoObligatorio("Documento: ");
            if (controlador.buscarDueno(documento) == null) break;
            System.out.println("Ya existe un dueño con ese documento. Revisalo e ingresá otro.");
        }

        String nombre = leerTextoObligatorio("Nombre: ");
        String apellido = leerTextoObligatorio("Apellido: ");
        String telefono = leerTelefono();
        String email = leerEmail();
        PlanSalud plan = null;
        if (leerSiNo("¿Tiene plan de salud animal?")) {
            plan = new PlanSalud("Plan de salud animal", 15.0);
            System.out.println("Plan registrado. El descuento se asigna automáticamente: 15 %.");
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
        int esp = leerOpcionEntre("Opción: ", 1, 4);
        String nombre = leerTextoObligatorio("Nombre: ");
        String raza = leerTextoObligatorio("Raza: ");
        String sexo;
        while (true) {
            sexo = leerTexto("Sexo (macho/hembra): ").toLowerCase();
            if (sexo.equals("macho") || sexo.equals("hembra")) break;
            System.out.println("Sexo inválido. Ingresá únicamente 'macho' o 'hembra'.");
        }
        LocalDate nac = leerFecha("Fecha de nacimiento (aaaa-mm-dd): ");
        while (nac.isAfter(LocalDate.now())) {
            System.out.println("La fecha de nacimiento no puede ser futura.");
            nac = leerFecha("Volvé a ingresar la fecha (aaaa-mm-dd): ");
        }
        double peso = leerDecimalPositivo("Peso actual (kg): ");
        Mascota m;
        switch (esp) {
            case 1: m = controlador.nuevoPerro(nombre, raza, sexo, nac, peso, dueno, leerTextoObligatorio("N° de chip: "), leerDecimalPositivo("Tamaño (cm): ")); break;
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
        String nombre = leerTextoObligatorio("Nombre: ");
        String apellido = leerTextoObligatorio("Apellido: ");
        String documento;
        while (true) {
            documento = leerTextoObligatorio("Documento: ");
            if (controlador.buscarPersonal(documento) == null) break;
            System.out.println("Ya existe un integrante del personal con ese documento. Ingresá otro.");
        }
        String telefono = leerTelefono();
        String matricula;
        while (true) {
            matricula = leerTextoObligatorio("Matrícula: ");
            boolean existe = false;
            for (Veterinario actual : controlador.listarVeterinarios()) {
                if (actual.getMatricula().equalsIgnoreCase(matricula)) existe = true;
            }
            if (!existe) break;
            System.out.println("Ya existe un veterinario con esa matrícula. Ingresá otra.");
        }
        String especialidad = leerTextoObligatorio("Especialidad: ");
        boolean guardia = leerSiNo("¿Está de guardia?");
        List<HorarioAtencion> horarios = new java.util.ArrayList<>();
        while (horarios.isEmpty()) {
            System.out.println("Cargá el horario de cada día en el que atiende el veterinario.");
            for (DiaSemana dia : DiaSemana.values()) {
                if (!leerSiNo("¿Atiende los " + dia.name().toLowerCase() + "?")) continue;
                LocalTime desde;
                LocalTime hasta;
                while (true) {
                    desde = leerHora("  Hora de inicio (hh:mm): ");
                    hasta = leerHora("  Hora de fin (hh:mm): ");
                    if (hasta.isAfter(desde)) break;
                    System.out.println("  La hora de fin debe ser posterior a la hora de inicio. Volvé a ingresar ambas horas.");
                }
                horarios.add(new HorarioAtencion(dia, desde, hasta));
            }
            if (horarios.isEmpty()) System.out.println("Debés seleccionar al menos un día de atención. Volvé a intentarlo.");
        }
        Veterinario v = controlador.registrarVeterinario(nombre, apellido, documento, telefono,
                matricula, especialidad, guardia, horarios);
        System.out.println("Veterinario registrado: " + v);
    }

    private void asignarTurno() throws ReglaNegocioException {
        Mascota m = elegir("Mascota", controlador.listarMascotas());
        if (m == null) return;
        Veterinario v = elegir("Veterinario", controlador.listarVeterinarios());
        if (v == null) return;
        System.out.println("Tipo: 1. Consulta general (30')  2. Vacunación (15')  3. Cirugía (120')");
        int t = leerOpcionEntre("Opción: ", 1, 3);
        boolean sena = false;
        if (controlador.requiereSena(m.getDuenoActual())) {
            System.out.printf("El dueño tiene 3 ausencias seguidas: debe abonar una seña de $%.2f.%n", MONTO_SENA);
            sena = leerSiNo("¿Se confirmó el pago de la seña en recepción?");
            if (!sena) {
                System.out.println("No se puede asignar el turno sin abonar la seña.");
                return;
            }
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
        List<Turno> pendientes = new java.util.ArrayList<>();
        for (Turno t : controlador.listarTurnos()) {
            if (t.getEstado() == EstadoTurno.PROGRAMADO && !t.getFin().isAfter(LocalDateTime.now())) {
                pendientes.add(t);
            }
        }
        if (pendientes.isEmpty()) {
            System.out.println("No hay turnos programados cuyo horario completo haya finalizado. No se puede registrar una ausencia antes de que termine el turno.");
            return;
        }
        Turno t = elegir("Turno para registrar como ausente", pendientes);
        if (t == null) return;
        controlador.registrarAusencia(t.getNumero());
        System.out.println("Ausencia registrada para el turno N° " + t.getNumero() + ".");
        if (controlador.requiereSena(t.getMascota().getDuenoActual())) {
            System.out.printf("ATENCIÓN: el dueño alcanzó tres ausencias consecutivas. Para solicitar nuevos turnos deberá abonar una seña de $%.2f en recepción.%n", MONTO_SENA);
        }
    }

    private void atenderTurno(Veterinario vet) throws ReglaNegocioException {
        List<Turno> atendibles = new java.util.ArrayList<>();
        for (Turno t : turnosProgramadosDe(vet)) {
            if (!t.getInicio().isAfter(LocalDateTime.now())) atendibles.add(t);
        }
        if (atendibles.isEmpty()) {
            System.out.println("No tenés turnos cuya hora de atención haya llegado.");
            return;
        }
        Turno t = elegir("Turnos listos para atender", atendibles);
        if (t == null) return;
        String motivo = leerTextoObligatorio("Motivo de la consulta: ");
        Consulta c = controlador.atenderTurno(t.getNumero(), motivo);
        System.out.println("Consulta abierta: " + c + " (" + c.getMascota().getNombre() + ")");
    }

    private void indicarTratamiento(Veterinario vet) throws ReglaNegocioException {
        Consulta c = elegirConsultaAbierta(vet);
        if (c == null) return;
        mostrar(controlador.listarProductos());
        controlador.indicarTratamiento(c, leerTextoObligatorio("Código del medicamento: "), leerTextoObligatorio("Dosis: "),
                leerTextoObligatorio("Frecuencia: "), leerTextoObligatorio("Duración: "));
        System.out.println("Tratamiento indicado (el stock se descuenta al confirmar la consulta).");
    }

    private void aplicarVacuna(Veterinario vet) throws ReglaNegocioException {
        Consulta c = elegirConsultaAbierta(vet);
        if (c == null) return;
        mostrar(controlador.listarCatalogoVacunas());
        VacunaAplicada va = controlador.aplicarVacuna(c, leerTextoObligatorio("Código de la vacuna: "), leerTextoObligatorio("Lote de la vacuna: "));
        System.out.println("Vacuna aplicada: " + va);
    }

    private void agregarInsumo(Veterinario vet) throws ReglaNegocioException {
        Consulta c = elegirConsultaAbierta(vet);
        if (c == null) return;
        mostrar(controlador.listarProductos());
        controlador.agregarInsumo(c, leerTextoObligatorio("Código del producto: "), leerEnteroPositivo("Cantidad: "));
        System.out.println("Insumo registrado en la consulta.");
    }

    private void confirmarConsulta(Veterinario vet) throws ReglaNegocioException {
        Consulta c = elegirConsultaAbierta(vet);
        if (c == null) return;
        Factura f = controlador.confirmarConsulta(c);
        System.out.println("Consulta confirmada y cerrada.");
        mostrarFactura(f);
        List<Producto> bajos = controlador.productosStockBajo();
        for (Producto p : bajos) System.out.println("ALERTA DE REPOSICIÓN: " + p.getNombre() + " (stock " + p.calcularStock() + ", mínimo " + p.getStockMinimo() + ")");
    }

    private void notaRectificatoria(Veterinario vet) throws ReglaNegocioException {
        List<Consulta> consultas = new java.util.ArrayList<>();
        for (Mascota mascota : controlador.listarMascotas()) {
            for (Consulta consulta : mascota.getHistoriaClinica()) {
                if (consulta.getVeterinario().equals(vet)) consultas.add(consulta);
            }
        }
        Consulta c = elegir("Consultas atendidas por " + vet.getNombreCompleto(), consultas);
        if (c == null) return;
        controlador.agregarNotaRectificatoria(c, leerTextoObligatorio("Texto de la nota rectificatoria: "), vet);
        System.out.println("Nota rectificatoria firmada por " + vet.getNombreCompleto() + ". La historia clínica original no se modificó.");
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
        List<Factura> pendientes = new java.util.ArrayList<>();
        for (Factura factura : controlador.listarFacturas()) {
            if (factura.getEstado() == EstadoFactura.EMITIDA) pendientes.add(factura);
        }
        if (pendientes.isEmpty()) {
            System.out.println("No hay facturas pendientes de pago. Las facturas pagadas o anuladas no se pueden cobrar.");
            return;
        }
        Factura f = elegir("Facturas pendientes de pago", pendientes);
        if (f == null) return;
        mostrarFactura(f);
        System.out.println("Medio de pago: 1. Efectivo (10% desc.)  2. Débito  3. Crédito en cuotas (8% recargo)");
        int op = leerOpcionEntre("Opción: ", 1, 3);
        MedioPago medio;
        switch (op) {
            case 1: medio = new Efectivo(); break;
            case 2: medio = new Debito(); break;
            default: medio = new Credito(leerEnteroPositivo("Cantidad de cuotas: ")); break;
        }
        System.out.printf("Total a pagar: $%.2f%n", controlador.calcularTotal(f, medio));
        if (leerSiNo("¿Confirmar el cobro?")) {
            controlador.pagarFactura(f.getNumero(), medio);
            System.out.println("Pago registrado.");
        } else {
            System.out.println("Cobro cancelado. La factura continúa pendiente.");
        }
    }

    private void mostrarFactura(Factura f) {
        System.out.println("Factura N° " + f.getNumero() + " (" + f.getFecha() + ") - " + f.getEstado());
        for (LineaFactura l : f.getLineas()) System.out.printf("   %-28s x%d  $%.2f%n", l.getDescripcion(), l.getCantidad(), l.calcularSubtotal());
        System.out.printf("   Subtotal: $%.2f%n", f.calcularSubtotal());
        PlanSalud plan = controlador.planDe(f);
        if (plan != null) System.out.println("   Plan de salud: " + plan.getNombre() + " (" + plan.getPorcentajeDescuento() + "% de descuento)");
        if (f.getPago() != null) {
            System.out.printf("   Medio de pago: %s (ajuste $%.2f)  -  Total pagado: $%.2f%n",
                    f.getPago().getMedioPago().getNombre(), f.calcularAjusteMedioPago(plan), f.getPago().getImporte());
        }
    }

    // ------------------------------------------------------------------ utilidades de interfaz

    private static final String[] DIAS = {"lun", "mar", "mié", "jue", "vie", "sáb", "dom"};
    private static final java.time.format.DateTimeFormatter FORMATO_HORA = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    /** Turnos futuros que todavía pueden cancelarse o reprogramarse. */
    private List<Turno> turnosProgramados() {
        List<Turno> r = new java.util.ArrayList<>();
        for (Turno t : controlador.listarTurnos()) {
            if (t.getEstado() == EstadoTurno.PROGRAMADO && t.getInicio().isAfter(LocalDateTime.now())) r.add(t);
        }
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
            while (true) {
                int h = leerEntero("Elegí el horario (0 para elegir otro día): ");
                if (h == 0) break;
                if (h >= 1 && h <= libres.size()) return libres.get(h - 1);
                System.out.println("Horario inválido. Seleccioná una de las opciones disponibles.");
            }
        }
    }

    private Consulta elegirConsultaAbierta(Veterinario vet) throws ReglaNegocioException {
        List<Consulta> abiertasDelVeterinario = new java.util.ArrayList<>();
        for (Consulta consulta : controlador.consultasAbiertas()) {
            if (consulta.getVeterinario().equals(vet)) abiertasDelVeterinario.add(consulta);
        }
        if (abiertasDelVeterinario.isEmpty()) {
            System.out.println("No tenés consultas abiertas. Primero atendé uno de tus turnos.");
            return null;
        }
        return elegir("Tus consultas abiertas", abiertasDelVeterinario);
    }

    /** Muestra una lista numerada y devuelve el elemento elegido (o null si no hay o se cancela). */
    private <T> T elegir(String titulo, List<T> lista) {
        if (lista.isEmpty()) { System.out.println("No hay elementos para elegir (" + titulo + ")."); return null; }
        System.out.println(titulo + ":");
        for (int i = 0; i < lista.size(); i++) System.out.println("  " + (i + 1) + ". " + lista.get(i));
        while (true) {
            int n = leerEntero("Número (0 para cancelar): ");
            if (n == 0) return null;
            if (n >= 1 && n <= lista.size()) return lista.get(n - 1);
            System.out.println("Opción inválida. Elegí un número de la lista.");
        }
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

    private String leerTextoObligatorio(String mensaje) {
        while (true) {
            String valor = leerTexto(mensaje);
            if (!valor.isBlank()) return valor;
            System.out.println("Este campo es obligatorio. Volvé a ingresarlo.");
        }
    }

    private int leerOpcionEntre(String mensaje, int minimo, int maximo) {
        while (true) {
            int valor = leerEntero(mensaje);
            if (valor >= minimo && valor <= maximo) return valor;
            System.out.println("Opción inválida. Ingresá un número entre " + minimo + " y " + maximo + ".");
        }
    }

    private int leerEnteroPositivo(String mensaje) {
        while (true) {
            int valor = leerEntero(mensaje);
            if (valor > 0) return valor;
            System.out.println("El valor debe ser mayor que cero.");
        }
    }

    private int leerEnteroNoNegativo(String mensaje) {
        while (true) {
            int valor = leerEntero(mensaje);
            if (valor >= 0) return valor;
            System.out.println("El valor no puede ser negativo.");
        }
    }

    private double leerDecimalNoNegativo(String mensaje) {
        while (true) {
            double valor = leerDecimal(mensaje);
            if (Double.isFinite(valor) && valor >= 0) return valor;
            System.out.println("Ingresá un número igual o mayor que cero.");
        }
    }

    private double leerDecimalPositivo(String mensaje) {
        while (true) {
            double valor = leerDecimal(mensaje);
            if (Double.isFinite(valor) && valor > 0) return valor;
            System.out.println("Ingresá un número mayor que cero.");
        }
    }

    private int leerEntero(String mensaje) {
        while (true) {
            try { return Integer.parseInt(leerTexto(mensaje)); }
            catch (NumberFormatException e) { System.out.println("Ingresá un número entero válido."); }
        }
    }

    private double leerDecimal(String mensaje) {
        while (true) {
            try {
                double valor = Double.parseDouble(leerTexto(mensaje).replace(',', '.'));
                if (Double.isFinite(valor)) return valor;
                System.out.println("Ingresá un número finito válido.");
            } catch (NumberFormatException e) {
                System.out.println("Ingresá un número válido.");
            }
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

    private String leerTelefono() {
        while (true) {
            String telefono = leerTextoObligatorio("Teléfono: ");
            String digitos = telefono.replaceAll("\\D", "");
            if (digitos.length() >= 7) return telefono;
            System.out.println("El teléfono debe contener al menos 7 dígitos. Volvé a ingresarlo.");
        }
    }

    private String leerEmail() {
        while (true) {
            String email = leerTextoObligatorio("Email: ");
            if (email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) return email;
            System.out.println("El email no tiene un formato válido. Ejemplo: nombre@dominio.com");
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

