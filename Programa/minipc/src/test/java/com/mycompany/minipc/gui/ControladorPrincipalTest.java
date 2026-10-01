package com.mycompany.minipc.gui;

import com.mycompany.minipc.config.Configuracion;
import com.mycompany.minipc.config.LectorConfiguracion;
import com.mycompany.minipc.hardware.EntradaIndice;
import com.mycompany.minipc.excepciones.ConfiguracionException;
import com.mycompany.minipc.isa.RegistroID;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.trabajos.Trabajo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del controlador con una vista falsa.
 *
 * Sirven para comprobar la cadena completa (elegir archivo, leerlo,
 * ensamblarlo, cargarlo y ejecutarlo) sin abrir ninguna ventana. Esa es
 * la ventaja de que el controlador programe contra la interfaz
 * VistaPrincipal y no contra la clase concreta.
 */
class ControladorPrincipalTest {

    /**
     * Vista de mentira: en vez de dibujar, anota lo que le piden.
     */
    private static class VistaFalsa implements VistaPrincipal {

        private List<File> archivosAEntregar = Collections.emptyList();
        private int refrescosDisco;
        private List<String> instrucciones = Collections.emptyList();
        private int filaResaltada = -1;
        private Proceso ultimoProceso;
        private final List<String> consola = new ArrayList<>();
        private String tituloError;
        private List<String> errores = Collections.emptyList();
        private boolean hayPrograma;
        private boolean enEjecucion;
        private boolean termino;
        private String archivoEnBarra = "";
        private String estadoEnBarra = "";
        private int usoMemoria = -1;
        private List<String> pantalla = Collections.emptyList();
        private boolean tecladoHabilitado;
        private String reloj = "";
        private List<Proceso> colas = Collections.emptyList();
        private int admitidos = -1;

        @Override
        public void mostrarInstrucciones(List<String> programa) {
            this.instrucciones = programa;
        }

        @Override
        public void resaltarInstruccion(int indiceFila) {
            this.filaResaltada = indiceFila;
        }

        @Override
        public void refrescarMemoria() {
            // La vista falsa no dibuja nada.
        }

        @Override
        public void mostrarBCP(Proceso proceso) {
            this.ultimoProceso = proceso;
        }

        @Override
        public void escribirEnConsola(String mensaje) {
            consola.add(mensaje);
        }

        @Override
        public void limpiarConsola() {
            consola.clear();
        }

        @Override
        public void mostrarErrores(String titulo, List<String> mensajes) {
            this.tituloError = titulo;
            this.errores = mensajes;
        }

        @Override
        public void actualizarBotones(boolean hayPrograma, boolean enEjecucion,
                boolean termino) {
            this.hayPrograma = hayPrograma;
            this.enEjecucion = enEjecucion;
            this.termino = termino;
        }

        @Override
        public void actualizarBarraContexto(String nombreArchivo, String estado) {
            this.archivoEnBarra = nombreArchivo;
            this.estadoEnBarra = estado;
        }

        @Override
        public void actualizarUsoMemoria(int porcentaje) {
            this.usoMemoria = porcentaje;
        }

        @Override
        public void refrescarDisco() {
            refrescosDisco++;
        }

        @Override
        public List<File> seleccionarArchivosAsm() {
            return archivosAEntregar;
        }

        @Override
        public void mostrarPantalla(List<String> lineas) {
            this.pantalla = new ArrayList<>(lineas);
        }

        @Override
        public void habilitarTeclado(boolean habilitado) {
            this.tecladoHabilitado = habilitado;
        }

        @Override
        public void mostrarReloj(String reloj) {
            this.reloj = reloj;
        }

        @Override
        public void mostrarColas(List<Proceso> procesos) {
            this.colas = procesos;
        }

        @Override
        public void mostrarResumen(int usoDisco, int admitidos) {
            this.admitidos = admitidos;
        }

        /** Fija los archivos que "elegira" el usuario; sin argumentos, cancela. */
        void entregar(File... archivos) {
            this.archivosAEntregar = List.of(archivos);
        }

        boolean consolaContiene(String fragmento) {
            return consola.stream().anyMatch(linea -> linea.contains(fragmento));
        }
    }

    private VistaFalsa vista;
    private ControladorPrincipal controlador;

    @BeforeEach
    void preparar() {
        vista = new VistaFalsa();
        controlador = new ControladorPrincipal(vista);
    }

    private File crearAsm(Path carpeta, String nombre, String contenido) throws IOException {
        Path archivo = carpeta.resolve(nombre);
        Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
        return archivo.toFile();
    }

    private File ejemploDelEnunciado(Path carpeta) throws IOException {
        return crearAsm(carpeta, "file.asm",
                "MOV AX, 5\nMOV BX, 3\nLOAD AX\nADD BX\nSUB AX\nSTORE AX\nMOV BX, -8\n");
    }

    private int ax() {
        return controlador.getProcesador().getRegistros().leer(RegistroID.AX);
    }

    private int bx() {
        return controlador.getProcesador().getRegistros().leer(RegistroID.BX);
    }

    /** Configuracion con el disco por defecto, FCFS, y la memoria y velocidad indicadas. */
    private static Configuracion config(int memoria, int msPorSegundo)
            throws ConfiguracionException {
        return new Configuracion(memoria, 512, 64, msPorSegundo, "FCFS");
    }

    private List<Trabajo> trabajos() {
        return controlador.getSistemaOperativo().getListaTrabajos().getTrabajos();
    }

    private Proceso enEjecucion() {
        return controlador.getSistemaOperativo().getEnEjecucion();
    }


    @Test
    @DisplayName("Cargar un archivo valido lo deja en la lista de trabajos y admitido")
    void cargaUnProgramaValido(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));

        controlador.alCargarArchivos();

        assertEquals(1, trabajos().size());
        assertEquals(EstadoProceso.PREPARADO, trabajos().get(0).getEstado());
        assertEquals("P1", controlador.getModeloTrabajos().getValueAt(0, 0));
        assertEquals("PREPARADO", controlador.getModeloTrabajos().getValueAt(0, 2));
        assertTrue(vista.hayPrograma);
        assertFalse(vista.termino);
        assertEquals("(CPU libre)", vista.archivoEnBarra, "Todavia no se despacho");
        assertEquals("CPU LIBRE", vista.estadoEnBarra);
        assertTrue(vista.consolaContiene("Planificador de trabajos: admite P1 (file.asm)"));
    }

    @Test
    @DisplayName("El programa cargado queda guardado en el disco y en su indice")
    void elProgramaQuedaEnElDisco(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));

        controlador.alCargarArchivos();

        EntradaIndice entrada = controlador.getDisco().buscar("file.asm");
        assertNotNull(entrada);
        assertEquals(20, entrada.getDireccionInicio());
        assertEquals(7, entrada.getTamano());
        assertEquals("file.asm|20|7", controlador.getModeloDisco().getValueAt(0, 2),
                "La tabla muestra la celda del indice tal como esta guardada");
        assertEquals("Indice", controlador.getModeloDisco().getValueAt(0, 1));
        assertEquals("MOV AX, 5", controlador.getModeloDisco().getValueAt(20, 2));
        assertEquals("Virtual", controlador.getModeloDisco().getValueAt(448, 1));
        assertTrue(vista.refrescosDisco > 0, "La tabla del disco debio refrescarse");
        assertTrue(vista.consolaContiene("Guardado en disco: file.asm, posiciones 20 a 26"));
    }

    @Test
    @DisplayName("Se pueden cargar varios archivos a la vez y todos entran a la lista")
    void cargaVariosArchivos(@TempDir Path carpeta) throws Exception {
        File a = crearAsm(carpeta, "a.asm", "MOV AX, 1\nMOV BX, 2\n");
        File b = crearAsm(carpeta, "b.asm", "MOV CX, 3\nMOV DX, 4\nADD CX\n");
        vista.entregar(a, b);

        controlador.alCargarArchivos();

        assertEquals(2, controlador.getDisco().getIndice().size());
        assertEquals(22, controlador.getDisco().buscar("b.asm").getDireccionInicio());
        assertEquals(2, trabajos().size());
        assertEquals(2, controlador.getSistemaOperativo().getTablaBCP().getProcesosAdmitidos());
        assertEquals(2, vista.colas.size(), "Las colas se arman con la lista de procesos en memoria");
        assertEquals(2, vista.admitidos);
        assertNull(vista.tituloError, "No hubo errores");
    }

    @Test
    @DisplayName("Un archivo invalido no impide guardar los validos y se reporta con su nombre")
    void unArchivoInvalidoNoFrenaALosDemas(@TempDir Path carpeta) throws Exception {
        File malo = crearAsm(carpeta, "malo.asm", "MOV AX, 5\nJUMP 100\n");
        File bueno = crearAsm(carpeta, "bueno.asm", "MOV AX, 5\n");
        vista.entregar(malo, bueno);

        controlador.alCargarArchivos();

        assertEquals("Errores al cargar archivos", vista.tituloError);
        assertEquals("malo.asm: 1 error(es) de sintaxis", vista.errores.get(0));
        assertTrue(vista.errores.get(1).contains("Linea 2"), vista.errores.get(1));
        assertNull(controlador.getDisco().buscar("malo.asm"));
        assertNotNull(controlador.getDisco().buscar("bueno.asm"));
        assertEquals(1, trabajos().size());
        assertEquals("bueno.asm", trabajos().get(0).getPrograma());
    }

    @Test
    @DisplayName("Cargar dos veces el mismo archivo guarda una copia numerada")
    void mismoArchivoDosVeces(@TempDir Path carpeta) throws Exception {
        File archivo = ejemploDelEnunciado(carpeta);
        vista.entregar(archivo, archivo);

        controlador.alCargarArchivos();

        assertNotNull(controlador.getDisco().buscar("file.asm"));
        assertNotNull(controlador.getDisco().buscar("file (2).asm"));
        assertEquals(2, trabajos().size());
        assertNull(vista.tituloError);
    }

    @Test
    @DisplayName("Si el usuario cancela el dialogo no pasa nada")
    void cancelarNoHaceNada() {
        vista.entregar();
        controlador.alCargarArchivos();
        assertFalse(vista.hayPrograma);
        assertTrue(trabajos().isEmpty());
        assertTrue(controlador.getDisco().getIndice().isEmpty());
    }

    @Test
    @DisplayName("Un archivo con errores de sintaxis los reporta todos y no guarda nada")
    void reportaErroresDeSintaxis(@TempDir Path carpeta) throws Exception {
        vista.entregar(crearAsm(carpeta, "error-sintaxis.asm",
                "MOV AX, 5\nJUMP 100\nADD EX\nMOV BX, tres\n"));

        controlador.alCargarArchivos();

        assertEquals("Errores al cargar archivos", vista.tituloError);
        assertEquals(4, vista.errores.size(), "Una linea con el archivo y tres errores");
        assertEquals("error-sintaxis.asm: 3 error(es) de sintaxis", vista.errores.get(0));
        assertTrue(vista.errores.get(1).contains("Linea 2"));
        assertTrue(vista.errores.get(2).contains("Linea 3"));
        assertTrue(vista.errores.get(3).contains("Linea 4"));
        assertFalse(vista.hayPrograma, "No debio cargarse nada");
        assertTrue(controlador.getDisco().getIndice().isEmpty());
    }

    @Test
    @DisplayName("Un programa mas grande que la memoria de usuario se rechaza al cargarlo")
    void reportaMemoriaInsuficiente(@TempDir Path carpeta) throws Exception {
        controlador.alConfigurar(config(160, 500));

        StringBuilder largo = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            largo.append("MOV AX, 1\n");
        }
        vista.entregar(crearAsm(carpeta, "largo.asm", largo.toString()));

        controlador.alCargarArchivos();

        assertEquals("Errores al cargar archivos", vista.tituloError);
        String error = vista.errores.get(0);
        assertTrue(error.startsWith("largo.asm"), error);
        assertTrue(error.contains("40 instrucciones"), error);
        assertTrue(error.contains("32 posiciones"), error);
        assertFalse(vista.hayPrograma);
        assertNull(controlador.getDisco().buscar("largo.asm"), "No se guarda algo que no correra");
    }

    @Test
    @DisplayName("Un archivo que no es .asm se rechaza")
    void rechazaExtensionInvalida(@TempDir Path carpeta) throws Exception {
        vista.entregar(crearAsm(carpeta, "programa.txt", "MOV AX, 5\n"));

        controlador.alCargarArchivos();

        assertEquals("Errores al cargar archivos", vista.tituloError);
        assertTrue(vista.errores.get(0).startsWith("programa.txt: "), vista.errores.get(0));
        assertTrue(vista.errores.get(0).contains(".asm"), vista.errores.get(0));
        assertFalse(vista.hayPrograma);
        assertTrue(controlador.getDisco().getIndice().isEmpty());
    }

    @Test
    @DisplayName("Siguiente despacha el proceso, ejecuta un segundo y mueve el resaltado")
    void pasoAPasoAvanza(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();
        assertEquals(-1, vista.filaResaltada, "Todavia no hay proceso en la CPU");

        controlador.alPasoAPaso();
        assertEquals(5, ax());
        assertEquals(1, vista.filaResaltada);
        assertEquals(7, vista.instrucciones.size(), "Se muestra el programa en ejecucion");
        assertEquals("file.asm", vista.archivoEnBarra);
        assertEquals(EstadoProceso.EJECUCION.name(), vista.estadoEnBarra);
        assertNotNull(vista.ultimoProceso);
        assertEquals(5, vista.ultimoProceso.getRegistro(RegistroID.AX),
                "El panel del BCP lee de la memoria");
        assertTrue(vista.consolaContiene("Despachador: P1 pasa a EJECUCION"));

        controlador.alPasoAPaso();
        assertEquals(3, bx());
        assertEquals(2, vista.filaResaltada);
    }

    @Test
    @DisplayName("El programa completo deja el resultado del enunciado en su BCP")
    void ejecucionCompleta(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();

        // Pesos: MOV 1, MOV 1, LOAD 2, ADD 3, SUB 3, STORE 2 = 12 s; MOV BX, -8 = 1 s.
        for (int i = 0; i < 12; i++) {
            controlador.alPasoAPaso();
        }
        Proceso p = enEjecucion();
        assertEquals(3, p.getAc());
        assertEquals(3, p.getRegistro(RegistroID.AX));

        controlador.alPasoAPaso();
        assertTrue(vista.termino);
        assertEquals(-1, vista.filaResaltada, "Ya no hay instruccion pendiente");
        assertEquals("FINALIZADO", vista.estadoEnBarra);
        assertEquals(EstadoProceso.FINALIZADO, trabajos().get(0).getEstado());
        assertEquals(13, trabajos().get(0).getTiempoCpu(), "Suma de los pesos");
        assertTrue(vista.consolaContiene("Todos los trabajos finalizaron"));
    }

    @Test
    @DisplayName("Con dos programas, FCFS ejecuta el primero completo y luego el segundo")
    void dosProgramasConFcfs(@TempDir Path carpeta) throws Exception {
        File a = crearAsm(carpeta, "a.asm", "MOV AX, 1\nINT 20H\n");
        File b = crearAsm(carpeta, "b.asm", "MOV AX, 2\nINT 20H\n");
        vista.entregar(a, b);
        controlador.alCargarArchivos();

        controlador.alPasoAPaso();
        assertEquals("a.asm", vista.archivoEnBarra);
        assertEquals("PREPARADO", controlador.getModeloTrabajos().getValueAt(1, 2));
        controlador.alPasoAPaso();
        controlador.alPasoAPaso();
        assertEquals("FINALIZADO", controlador.getModeloTrabajos().getValueAt(0, 2),
                "MOV (1 s) + INT 20H (2 s)");

        controlador.alPasoAPaso();
        assertEquals("b.asm", vista.archivoEnBarra, "Cambio de contexto al segundo");
        assertEquals(2, ax());
    }

    @Test
    @DisplayName("Al terminar la ejecucion automatica los botones vuelven a habilitarse")
    void ejecucionAutomaticaRehabilitaBotones(@TempDir Path carpeta) throws Exception {
        controlador.alConfigurar(config(256, 50));
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();

        controlador.alEjecutar();
        assertTrue(vista.enEjecucion, "Mientras corre, los botones quedan bloqueados");

        // Siete segundos de CPU a 50 ms tardan unos 350 ms; se espera con margen.
        long limite = System.currentTimeMillis() + 5000;
        while (vista.enEjecucion && System.currentTimeMillis() < limite) {
            Thread.sleep(20);
        }

        assertFalse(vista.enEjecucion, "El temporizador debio detenerse y refrescar la vista");
        assertTrue(vista.termino);
        assertTrue(vista.hayPrograma, "El boton de estadisticas depende de esto");
        assertEquals(EstadoProceso.FINALIZADO, trabajos().get(0).getEstado());
    }

    @Test
    @DisplayName("Un error de ejecucion se muestra y el proceso finaliza")
    void errorDeEjecucion(@TempDir Path carpeta) throws Exception {
        vista.entregar(crearAsm(carpeta, "vacia.asm", "POP AX\nINT 20H\n"));
        controlador.alCargarArchivos();

        controlador.alPasoAPaso();

        assertEquals("Error de ejecucion", vista.tituloError);
        assertTrue(vista.errores.get(0).contains("Pila vacia"), vista.errores.get(0));
        assertEquals("FINALIZADO (error)", controlador.getModeloTrabajos().getValueAt(0, 2));
    }

    @Test
    @DisplayName("Reiniciar devuelve los trabajos a PREPARADO y el reloj a cero")
    void reiniciarConservaElPrograma(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();
        controlador.alPasoAPaso();
        controlador.alPasoAPaso();

        controlador.alReiniciar();

        assertEquals(0, controlador.getSistemaOperativo().getReloj());
        assertEquals(EstadoProceso.PREPARADO, trabajos().get(0).getEstado());
        assertEquals(-1, vista.filaResaltada);
        assertTrue(vista.hayPrograma);
        assertEquals("CPU LIBRE", vista.estadoEnBarra);
        assertNotNull(controlador.getDisco().buscar("file.asm"), "El disco se conserva");
    }

    @Test
    @DisplayName("Limpiar descarga todo y vacia las tablas")
    void limpiarDescargaTodo(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();
        controlador.alPasoAPaso();

        controlador.alLimpiar();

        assertFalse(vista.hayPrograma);
        assertEquals(0, controlador.getModeloInstrucciones().getRowCount());
        assertEquals(0, controlador.getProcesador().getMemoria().getPosicionesUsadas());
        assertTrue(trabajos().isEmpty());
        assertTrue(controlador.getDisco().getIndice().isEmpty(), "El disco tambien se vacia");
        assertEquals("(CPU libre)", vista.archivoEnBarra);
        assertEquals("SIN PROGRAMA", vista.estadoEnBarra);
    }

    @Test
    @DisplayName("Configurar cambia la memoria, el disco y la velocidad")
    void configurarCambiaLaMemoria(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();

        controlador.alConfigurar(new Configuracion(512, 1024, 128, 250, "FCFS"));

        assertEquals(512, controlador.getProcesador().getMemoria().getTamano());
        assertEquals(128, controlador.getProcesador().getMemoria().getLimiteKernel(),
                "El kernel sigue siendo el calculado");
        assertEquals(1024, controlador.getDisco().getTamano());
        assertEquals(896, controlador.getDisco().getInicioMemoriaVirtual());
        assertTrue(controlador.getDisco().getIndice().isEmpty(), "Reconfigurar vacia el disco");
        assertTrue(trabajos().isEmpty());
        assertEquals(1024, controlador.getModeloDisco().getRowCount());
        assertEquals(250, controlador.getVelocidadMs());
        assertEquals(250, controlador.getConfiguracion().getMsPorSegundo());
        assertTrue(vista.consolaContiene("Memoria de 512 posiciones"));
        assertTrue(vista.consolaContiene("Planificacion: FCFS"));
    }

    @Test
    @DisplayName("Sin archivo de configuracion se usan los valores por defecto")
    void sinArchivoUsaLosValoresPorDefecto() {
        assertEquals(256, controlador.getProcesador().getMemoria().getTamano());
        assertEquals(128, controlador.getProcesador().getMemoria().getLimiteKernel());
        assertEquals(512, controlador.getDisco().getTamano());
        assertEquals(1000, controlador.getVelocidadMs());
        assertEquals("FCFS", controlador.getSistemaOperativo().getAlgoritmo().getNombre());
    }

    @Test
    @DisplayName("Al arrancar se lee el archivo de configuracion")
    void leeLaConfiguracionAlArrancar(@TempDir Path carpeta) throws Exception {
        Path archivo = carpeta.resolve("config.properties");
        Files.writeString(archivo, "memoria.tamano=300\ndisco.tamano=600\n",
                StandardCharsets.UTF_8);

        ControladorPrincipal conArchivo = new ControladorPrincipal(vista,
                new LectorConfiguracion(archivo));
        conArchivo.inicializarVista();

        assertEquals(300, conArchivo.getProcesador().getMemoria().getTamano());
        assertEquals(600, conArchivo.getDisco().getTamano());
        assertNull(vista.tituloError);
        assertTrue(vista.consolaContiene("Configuracion leida de"));
    }

    @Test
    @DisplayName("Un archivo de configuracion invalido se informa y se usan los valores por defecto")
    void configuracionInvalidaAlArrancar(@TempDir Path carpeta) throws Exception {
        Path archivo = carpeta.resolve("config.properties");
        Files.writeString(archivo, "memoria.tamano=10\n", StandardCharsets.UTF_8);

        ControladorPrincipal conArchivo = new ControladorPrincipal(vista,
                new LectorConfiguracion(archivo));
        conArchivo.inicializarVista();

        assertEquals("Configuracion invalida", vista.tituloError);
        assertTrue(vista.errores.stream().anyMatch(e -> e.contains("al menos 160")));
        assertEquals(256, conArchivo.getProcesador().getMemoria().getTamano());
    }

    @Test
    @DisplayName("Aceptar una configuracion la guarda en el archivo")
    void configurarGuardaElArchivo(@TempDir Path carpeta) throws Exception {
        LectorConfiguracion lector = new LectorConfiguracion(carpeta.resolve("config.properties"));
        ControladorPrincipal conArchivo = new ControladorPrincipal(vista, lector);

        conArchivo.alConfigurar(new Configuracion(384, 768, 32, 400, "FCFS"));

        Configuracion guardada = lector.cargar();
        assertEquals(384, guardada.getTamanoMemoria());
        assertEquals(768, guardada.getTamanoDisco());
        assertEquals(32, guardada.getTamanoMemoriaVirtual());
        assertEquals(400, guardada.getMsPorSegundo());
        assertEquals("FCFS", guardada.getAlgoritmo());
    }

    @Test
    @DisplayName("La tabla de memoria nombra la cabecera, los campos del BCP y el programa")
    void modeloDeMemoriaRefleja(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();

        assertEquals(256, controlador.getModeloMemoria().getRowCount());
        assertEquals("SO.En ejecucion", controlador.getModeloMemoria().getValueAt(0, 1));
        assertEquals("SO.Admitidos", controlador.getModeloMemoria().getValueAt(2, 1));
        assertEquals("1", controlador.getModeloMemoria().getValueAt(2, 2));
        assertEquals("P1.PID", controlador.getModeloMemoria().getValueAt(3, 1));
        assertEquals("1", controlador.getModeloMemoria().getValueAt(3, 2));
        assertEquals("P1.Estado", controlador.getModeloMemoria().getValueAt(5, 1));
        assertEquals("PREPARADO", controlador.getModeloMemoria().getValueAt(5, 2));
        assertEquals("BCP libre", controlador.getModeloMemoria().getValueAt(28, 1));
        assertEquals("P1", controlador.getModeloMemoria().getValueAt(128, 1));
        assertEquals("MOV AX, 5", controlador.getModeloMemoria().getValueAt(128, 2));
        assertEquals("", controlador.getModeloMemoria().getValueAt(200, 2));
    }

    @Test
    @DisplayName("Las estadisticas quedan disponibles al terminar")
    void estadisticasDisponibles(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();
        for (int i = 0; i < 13; i++) {
            controlador.alPasoAPaso();
        }

        assertEquals(7, controlador.obtenerEstadisticas().getTotalInstrucciones());
        Trabajo t = trabajos().get(0);
        assertEquals(0, t.getInicio());
        assertEquals(13, t.getFin(), "El reloj avanza segun los pesos");
        assertEquals("00:00:13", vista.reloj);
    }

    @Test
    @DisplayName("INT 09H habilita el teclado; el valor llega al proceso y se ve en la pantalla")
    void tecladoYPantalla(@TempDir Path carpeta) throws Exception {
        vista.entregar(crearAsm(carpeta, "teclado.asm", "INT 09H\nINT 10H\nINT 20H\n"));
        controlador.alCargarArchivos();
        assertFalse(vista.tecladoHabilitado);

        controlador.alPasoAPaso();
        assertTrue(vista.tecladoHabilitado, "El proceso espera el teclado");
        assertEquals(List.of(">> Ingresar valor:"), vista.pantalla);
        assertEquals("EN_ESPERA", controlador.getModeloTrabajos().getValueAt(0, 2));

        controlador.alEnviarTeclado("300");
        assertEquals("Teclado", vista.tituloError);
        assertTrue(vista.tecladoHabilitado, "Un valor invalido no se acepta");

        controlador.alEnviarTeclado("42");
        assertFalse(vista.tecladoHabilitado);
        assertEquals("PREPARADO", controlador.getModeloTrabajos().getValueAt(0, 2));

        controlador.alPasoAPaso();
        controlador.alPasoAPaso();
        assertEquals(List.of(">> Ingresar valor:", "42", "42"), vista.pantalla,
                "Eco del teclado y luego INT 10H imprime DX");
    }

    @Test
    @DisplayName("Pausar detiene la ejecucion automatica y se puede seguir con Siguiente")
    void pausarLaEjecucionAutomatica(@TempDir Path carpeta) throws Exception {
        controlador.alConfigurar(config(256, 2000));
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();

        controlador.alEjecutar();
        assertTrue(vista.enEjecucion);
        controlador.alPausar();
        assertFalse(vista.enEjecucion, "El temporizador se detuvo");
        assertTrue(vista.consolaContiene("en pausa"));

        controlador.alPasoAPaso();
        assertEquals(EstadoProceso.EJECUCION, trabajos().get(0).getEstado());
    }
}
