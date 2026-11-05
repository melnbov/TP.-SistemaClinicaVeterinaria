# Sistema de Gestión de Clínica Veterinaria

## Descripción del proyecto

El sistema tiene como objetivo informatizar la gestión de una clínica veterinaria, centralizando la información de los dueños, las mascotas, los turnos, las consultas médicas, el stock de insumos y la facturación.

Busca optimizar la organización de la clínica, facilitar el seguimiento de la salud de las mascotas y garantizar el cumplimiento de las reglas de negocio establecidas.

## Funcionalidades principales

- **Gestión de dueños y mascotas:** registro de dueños, mascotas y sus características según la especie.
- **Gestión de turnos:** asignación, reprogramación y cancelación, evitando superposiciones horarias.
- **Historia clínica:** registro de consultas, diagnósticos, tratamientos y vacunas.
- **Control de vacunas:** seguimiento de refuerzos y generación de recordatorios.
- **Control de stock:** administración de medicamentos e insumos, con alertas de reposición y control de vencimientos.
- **Facturación:** cálculo de importes, descuentos y recargos según el medio de pago.
- **Gestión de ausencias y urgencias:** aplicación de las reglas correspondientes para turnos y reprogramaciones.

## Actores del sistema

- **Recepcionista:** gestiona dueños, mascotas y turnos.
- **Veterinario:** registra consultas, diagnósticos, tratamientos y vacunas.
- **Dueño de la mascota:** solicita y gestiona turnos para sus animales.
- **Administrador:** supervisa la gestión general de la clínica y el control de insumos.

## Tecnologías utilizadas

- Java
- Programación Orientada a Objetos (POO)
- UML para el modelado del sistema

## Integrantes del equipo

- Bovarines Melina 
- Manograsso Giovanna
- Torres Manuela

## Requisitos
- Java 17 o superior (`java -version`). No hace falta Maven ni Gradle.

## Cómo ejecutar

Todos los comandos se ejecutan **desde la carpeta raíz del proyecto** (la que contiene `src/` y `datos/`).

**Opción A: IntelliJ IDEA (el proyecto ya tiene su `.iml`)**
1. Abrir la carpeta del proyecto (`File › Open`).
2. Verificar que `src` esté marcada como *Sources Root* (clic derecho sobre `src › Mark Directory as › Sources Root`).
3. Abrir `src/main/Main.java` y ejecutar con el botón ▶ (*Run 'Main.main()'*).
   El directorio de trabajo tiene que ser la raíz del proyecto, para que los datos se guarden en `datos/`.

**Opción B: consola (Linux / macOS)**
```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -Dfile.encoding=UTF-8 -cp out main.Main
```

**Opción B: consola (Windows, PowerShell o CMD)**
```bat
mkdir out
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 -d out @fuentes.txt
chcp 65001
java -Dfile.encoding=UTF-8 -cp out main.Main
```

## Cómo se usa el menú

Al iniciar se elige con qué perfil se ingresa:

```
1. Dueño de mascota
2. Administrador
3. Recepcionista
4. Veterinario
0. Salir
```

| Perfil | Ingreso | Qué puede hacer |
|---|---|---|
| **Dueño de mascota** | Submenú *Ingresar al sistema* (con su documento) o *Registrarme* | Solicitar turno, ver sus turnos, cancelar o reprogramar un turno, ver sus mascotas y agregar una |
| **Recepcionista** | Documento `27555666` (Marta Díaz) | Clientes y mascotas (incluye transferir una mascota a otro dueño), turnos (asignar, urgencias, reprogramar, cancelar, ausencias), cobros y facturas, vacunas y recordatorios |
| **Veterinario** | Documento `30111222` (Ana Gómez) o `28999111` (Luis Pérez) | Ver sus turnos, abrir la consulta de un turno, registrar peso, diagnóstico, tratamiento, vacuna e insumos, confirmar la consulta, notas rectificatorias e historia clínica |
| **Administrador** | Documento `25444333` (Carlos Ruiz) | Stock de medicamentos e insumos, reportes y alta de veterinarios |

Notas de uso:
- Los turnos **no se escriben con fecha**: se elige el día de la lista de días en que atiende el veterinario y después el horario libre. Antes de elegir se muestran los horarios ya reservados de ese día.
- Las fechas que sí se escriben (nacimiento de la mascota, vencimiento de un lote) llevan el formato `aaaa-mm-dd`, por ejemplo `2026-11-20`.
- Si un dueño tiene 3 ausencias seguidas debe pagar una seña, y eso lo gestiona recepción.

## Dónde se guardan los datos
En `datos/clinica.dat` (relativo a la carpeta desde la que se ejecuta). Se guarda **automáticamente después de cada
operación**. Para empezar de cero basta con borrar ese archivo (o la carpeta `datos/`). Se puede elegir otra ruta con
`java -Dclinica.archivo=otra/ruta.dat -cp out main.Main`.

## Datos de prueba
En la primera ejecución se crean: los veterinarios **Ana Gómez** (M-1001, de guardia, lunes a sábado de 9 a 19) y
**Luis Pérez** (M-1002, lunes a viernes de 9 a 19), la recepcionista **Marta Díaz**, el administrador **Carlos Ruiz**,
el catálogo de vacunas (V01–V05) y los productos `MED01` Amoxicilina, `MED02` Meloxicam e `INS01` Gasas estériles con stock.

### Recorrido sugerido para la demo
1. **Recepcionista** (`3`, documento `27555666`) › **1** Clientes y mascotas › **1** registrar dueño (con plan de salud 15 %) › **3** registrar mascota.
2. En el mismo perfil › **2** Turnos › **1** asignar turno: elegir mascota, veterinario, tipo de atención, día y horario.
3. Volver al inicio y entrar como **Veterinario** (`4`, documento `30111222`) › **1** atender el turno › **2** peso › **3** diagnóstico › **4** tratamiento (`MED01`) › **5** vacuna (`V01`) › **6** insumo (`INS01`).
4. **7** confirmar consulta (recién ahí se descuenta el stock y se emite la factura).
5. Volver a **Recepcionista** › **3** Cobros y facturas › **2** cobrar la factura.
6. Entrar como **Dueño de mascota** (`1`) › *Ingresar al sistema* con el documento cargado en el paso 1 › **1** solicitar turno.
7. Salir (`0`), volver a ejecutar y consultar: todo sigue guardado.

## Estructura del proyecto y dónde está cada clase

El código sigue una arquitectura en capas: la **vista** solo muestra y lee datos, el **controlador** coordina,
el **modelo** contiene las reglas de negocio y la **persistencia** guarda todo en un archivo.

```
TP.-SistemaClinicaVeterinaria/
├── src/
│   ├── main/
│   │   └── Main.java
│   ├── vista/
│   │   └── MenuConsola.java
│   ├── controlador/
│   │   └── ControladorClinica.java
│   ├── modelo/
│   │   ├── entidades/
│   │   ├── servicios/
│   │   └── excepciones/
│   │       └── ReglaNegocioException.java
│   └── persistencia/
│       ├── DatosClinica.java
│       └── RepositorioArchivo.java
├── datos/
│   └── clinica.dat
├── docs/
│   ├── Documentacion del Sistema de Clinica Veterinaria.pdf
│   └── uml/
│   └── Informe final

└── README.md
```

### `src/main/`
| Clase | Qué hace |
|---|---|
| `Main` | Punto de entrada: crea el menú y lo inicia. |

### `src/vista/`
| Clase | Qué hace |
|---|---|
| `MenuConsola` | Menú por consola: pantalla de ingreso por perfil, menús de cada perfil, lectura y validación de datos escritos. Solo hace entrada/salida y delega todo al controlador. |

### `src/controlador/`
| Clase | Qué hace |
|---|---|
| `ControladorClinica` | Recibe las acciones del menú, usa los servicios del modelo y **guarda el estado en el archivo** después de cada operación que modifica datos. |

### `src/modelo/entidades/` (los objetos del negocio)
| Grupo | Clases |
|---|---|
| **Personas** | `Personal` (clase base), `Veterinario`, `Recepcionista`, `Administrador`, `Dueno`, `PlanSalud` |
| **Mascotas** | `Mascota` (clase base), `Perro`, `Gato`, `Ave`, `Conejo`, `Especie`, `EstiloVida`, `HistorialDueno` |
| **Turnos y horarios** | `Turno`, `EstadoTurno`, `TipoAtencion`, `HorarioAtencion`, `DiaSemana` |
| **Atención clínica** | `Consulta`, `Tratamiento`, `Vacuna`, `VacunaAplicada`, `NotaRectificatoria`, `InsumoUtilizado` |
| **Stock y compras** | `Producto` (base), `Medicamento`, `Insumo`, `LoteProducto`, `UsoInsumo`, `Proveedor`, `Compra`, `DetalleCompra` |
| **Facturación y pagos** | `Factura`, `LineaFactura`, `EstadoFactura`, `Pago`, `MedioPago` (interfaz), `Efectivo`, `Debito`, `Credito`, `NotaCredito` |

### `src/modelo/servicios/` (reglas de negocio)
| Clase | Qué hace |
|---|---|
| `ServicioTurnos` | Asigna, reprograma y cancela turnos; evita superposiciones, cobra el cargo por cancelación tardía, exige seña tras 3 ausencias y resuelve urgencias. |
| `ServicioStock` | Administra productos y lotes, descuenta stock y detecta stock bajo. |
| `RegistroDuenosMascotas` | Registro de dueños y mascotas; valida duplicados y transferencias de mascotas. |
| `ServicioRecordatorios` | Detecta refuerzos de vacunas próximos o vencidos y envía los avisos. |
| `Notificador` (interfaz), `EmailNotificador`, `SmsNotificador` | Canales para enviar avisos a los dueños. |
| `Reporte` (interfaz), `ReporteTurnosVeterinario`, `ReporteStockBajo`, `ReporteVacunasProximas` | Reportes del administrador. |

### `src/modelo/excepciones/`
| Clase | Qué hace |
|---|---|
| `ReglaNegocioException` | Excepción propia que se lanza cuando se viola una regla de negocio. El menú muestra su mensaje al usuario. |

### `src/persistencia/`
| Clase | Qué hace |
|---|---|
| `DatosClinica` | Contiene todos los datos del sistema (dueños, turnos, stock, facturas, personal) y arma los datos iniciales de la primera ejecución. |
| `RepositorioArchivo` | Guarda y carga `DatosClinica` en `datos/clinica.dat` (serialización de Java). |

### Otras carpetas
- `datos/`: archivo `clinica.dat` con la información guardada.
- `docs/`: documentación del sistema, informe final  y diagramas UML (`docs/uml/`: clases inicial y modificado, casos de uso y secuencia).
- `out/`: clases compiladas por IntelliJ (se regenera al compilar, no se edita).

## Supuestos documentados (el enunciado no los define)
- Precio base por tipo de atención: consulta $15.000, vacunación $8.000, cirugía $120.000 (`TipoAtencion`).
- Precio de venta de un producto = costo del lote que vence primero + 50 %.
- Cada tratamiento indicado consume 1 unidad del medicamento; los insumos se cargan con su cantidad.
- El cargo por cancelación tardía (20 %) se registra en el turno y se informa; no genera una factura aparte.
- Las ausencias seguidas se cuentan por dueño sobre sus últimos turnos ATENDIDO/AUSENTE (los cancelados no cortan la racha).
- La urgencia busca el primer horario libre del veterinario de guardia a partir de «ahora» (pasos de 15 min, hasta 14 días).
