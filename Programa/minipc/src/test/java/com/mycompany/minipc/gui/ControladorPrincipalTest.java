package com.mycompany.minipc.gui;

import com.mycompany.minipc.config.Configuracion;
import com.mycompany.minipc.config.LectorConfiguracion;
import com.mycompany.minipc.core.BCP;
import com.mycompany.minipc.core.EntradaIndice;
import com.mycompany.minipc.core.EstadoProceso;
import com.mycompany.minipc.excepciones.ConfiguracionException;
import com.mycompany.minipc.isa.RegistroID;
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
        private BCP ultimoBcp;
        private final List<String> consola = new ArrayList<>();
        private String tituloError;
        private List<String> errores = Collections.emptyList();
        private boolean hayPrograma;
        private boolean enEjecucion;
        private boolean termino;
        private String archivoEnBarra = "";
        private String estadoEnBarra = "";
        private int usoMemoria = -1;

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
        public void mostrarBCP(BCP bcp) {
            this.ultimoBcp = bcp;
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

    /** Configuracion con el disco por defecto y la memoria y velocidad indicadas. */
    private static Configuracion config(int memoria, int kernel, int msPorSegundo)
            throws ConfiguracionException {
        return new Configuracion(memoria, kernel, 512, 64, msPorSegundo);
    }

    @Test
    @DisplayName("Cargar un archivo valido llena la tabla y deja el programa listo")
    void cargaUnProgramaValido(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));

        controlador.alCargarArchivos();

        assertEquals(7, vista.instrucciones.size());
        assertEquals(7, controlador.getModeloInstrucciones().getRowCount());
        assertEquals("MOV AX, 5", controlador.getModeloInstrucciones().getValueAt(0, 1));
        assertTrue(vista.hayPrograma);
        assertFalse(vista.termino);
        assertEquals("file.asm", vista.archivoEnBarra);
        assertEquals(EstadoProceso.LISTO.name(), vista.estadoEnBarra);
        assertTrue(vista.consolaContiene("Programa file.asm cargado en memoria en la posicion 64"));
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
    @DisplayName("Se pueden cargar varios archivos a la vez")
    void cargaVariosArchivos(@TempDir Path carpeta) throws Exception {
        File a = crearAsm(carpeta, "a.asm", "MOV AX, 1\nMOV BX, 2\n");
        File b = crearAsm(carpeta, "b.asm", "MOV CX, 3\nMOV DX, 4\nADD CX\n");
        vista.entregar(a, b);

        controlador.alCargarArchivos();

        assertEquals(2, controlador.getDisco().getIndice().size());
        assertEquals(22, controlador.getDisco().buscar("b.asm").getDireccionInicio());
        assertEquals("a.asm", vista.archivoEnBarra, "El primero pasa a memoria");
        assertTrue(vista.consolaContiene("1 programa(s) mas quedan guardados en el disco"));
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
        assertEquals("bueno.asm", vista.archivoEnBarra);
    }

    @Test
    @DisplayName("Cargar dos veces el mismo archivo guarda una copia numerada")
    void mismoArchivoDosVeces(@TempDir Path carpeta) throws Exception {
        File archivo = ejemploDelEnunciado(carpeta);
        vista.entregar(archivo, archivo);

        controlador.alCargarArchivos();

        assertNotNull(controlador.getDisco().buscar("file.asm"));
        assertNotNull(controlador.getDisco().buscar("file (2).asm"));
        assertNull(vista.tituloError);
    }

    @Test
    @DisplayName("Si el usuario cancela el dialogo no pasa nada")
    void cancelarNoHaceNada() {
        vista.entregar();
        controlador.alCargarArchivos();
        assertFalse(vista.hayPrograma);
        assertEquals(0, controlador.getModeloInstrucciones().getRowCount());
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
        assertEquals(0, controlador.getModeloInstrucciones().getRowCount());
        assertTrue(controlador.getDisco().getIndice().isEmpty());
    }

    @Test
    @DisplayName("Un programa que no cabe en memoria queda en el disco y se informa")
    void reportaMemoriaInsuficiente(@TempDir Path carpeta) throws Exception {
        controlador.alConfigurar(config(128, 120, 500));

        StringBuilder largo = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            largo.append("MOV AX, 1\n");
        }
        vista.entregar(crearAsm(carpeta, "largo.asm", largo.toString()));

        controlador.alCargarArchivos();

        assertEquals("Errores al cargar archivos", vista.tituloError);
        String error = vista.errores.get(0);
        assertTrue(error.startsWith("largo.asm"), error);
        assertTrue(error.contains("requiere 10"), error);
        assertTrue(error.contains("dispone de 8"), error);
        assertFalse(vista.hayPrograma);
        assertNotNull(controlador.getDisco().buscar("largo.asm"), "Debe quedar en el disco");
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
    @DisplayName("Paso a paso avanza una instruccion y mueve el resaltado")
    void pasoAPasoAvanza(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();
        assertEquals(0, vista.filaResaltada);

        controlador.alPasoAPaso();
        assertEquals(5, ax());
        assertEquals(1, vista.filaResaltada);

        controlador.alPasoAPaso();
        assertEquals(3, bx());
        assertEquals(2, vista.filaResaltada);
    }

    @Test
    @DisplayName("El programa completo deja el resultado del enunciado")
    void ejecucionCompleta(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();

        for (int i = 0; i < 7; i++) {
            controlador.alPasoAPaso();
        }

        assertEquals(3, controlador.getProcesador().getAc());
        assertEquals(3, ax());
        assertEquals(-8, bx());
        assertTrue(vista.termino);
        assertEquals(-1, vista.filaResaltada, "Ya no hay instruccion pendiente");
        assertEquals(EstadoProceso.TERMINADO.name(), vista.estadoEnBarra);
        assertTrue(vista.consolaContiene("7 instrucciones ejecutadas"));
    }

    @Test
    @DisplayName("Al terminar la ejecucion automatica los botones vuelven a habilitarse")
    void ejecucionAutomaticaRehabilitaBotones(@TempDir Path carpeta) throws Exception {
        controlador.alConfigurar(config(256, 64, 50));
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();

        controlador.alEjecutar();
        assertTrue(vista.enEjecucion, "Mientras corre, los botones quedan bloqueados");

        // Siete instrucciones a 50 ms tardan unos 350 ms; se espera con margen.
        long limite = System.currentTimeMillis() + 5000;
        while (vista.enEjecucion && System.currentTimeMillis() < limite) {
            Thread.sleep(20);
        }

        assertFalse(vista.enEjecucion, "El temporizador debio detenerse y refrescar la vista");
        assertTrue(vista.termino);
        assertTrue(vista.hayPrograma, "El boton de estadisticas depende de esto");
        assertEquals(3, controlador.getProcesador().getAc());
        assertEquals(-8, bx());
    }

    @Test
    @DisplayName("Reiniciar vuelve al inicio conservando el programa")
    void reiniciarConservaElPrograma(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();
        controlador.alPasoAPaso();
        controlador.alPasoAPaso();

        controlador.alReiniciar();

        assertEquals(0, ax());
        assertEquals(0, vista.filaResaltada);
        assertTrue(vista.hayPrograma);
        assertEquals(7, controlador.getModeloInstrucciones().getRowCount());
        assertEquals(EstadoProceso.LISTO.name(), vista.estadoEnBarra);
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
        assertTrue(controlador.getDisco().getIndice().isEmpty(), "El disco tambien se vacia");
        assertEquals("(ninguno)", vista.archivoEnBarra);
        assertEquals("SIN PROGRAMA", vista.estadoEnBarra);
    }

    @Test
    @DisplayName("Configurar cambia la memoria, el disco y la velocidad")
    void configurarCambiaLaMemoria(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();

        controlador.alConfigurar(new Configuracion(512, 128, 1024, 128, 250));

        assertEquals(512, controlador.getProcesador().getMemoria().getTamano());
        assertEquals(128, controlador.getProcesador().getMemoria().getLimiteKernel());
        assertEquals(1024, controlador.getDisco().getTamano());
        assertEquals(896, controlador.getDisco().getInicioMemoriaVirtual());
        assertTrue(controlador.getDisco().getIndice().isEmpty(), "Reconfigurar vacia el disco");
        assertEquals(1024, controlador.getModeloDisco().getRowCount());
        assertEquals(250, controlador.getVelocidadMs());
        assertEquals(250, controlador.getConfiguracion().getMsPorSegundo());
        assertTrue(vista.consolaContiene("512 posiciones"));
    }

    @Test
    @DisplayName("Sin archivo de configuracion se usan los valores por defecto")
    void sinArchivoUsaLosValoresPorDefecto() {
        assertEquals(256, controlador.getProcesador().getMemoria().getTamano());
        assertEquals(512, controlador.getDisco().getTamano());
        assertEquals(1000, controlador.getVelocidadMs());
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
        assertTrue(vista.errores.stream().anyMatch(e -> e.contains("al menos 128")));
        assertEquals(256, conArchivo.getProcesador().getMemoria().getTamano());
    }

    @Test
    @DisplayName("Aceptar una configuracion la guarda en el archivo")
    void configurarGuardaElArchivo(@TempDir Path carpeta) throws Exception {
        LectorConfiguracion lector = new LectorConfiguracion(carpeta.resolve("config.properties"));
        ControladorPrincipal conArchivo = new ControladorPrincipal(vista, lector);

        conArchivo.alConfigurar(new Configuracion(384, 96, 768, 32, 400));

        Configuracion guardada = lector.cargar();
        assertEquals(384, guardada.getTamanoMemoria());
        assertEquals(96, guardada.getLimiteKernel());
        assertEquals(768, guardada.getTamanoDisco());
        assertEquals(32, guardada.getTamanoMemoriaVirtual());
        assertEquals(400, guardada.getMsPorSegundo());
    }

    @Test
    @DisplayName("El modelo de memoria refleja el programa cargado")
    void modeloDeMemoriaRefleja(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();

        assertEquals(256, controlador.getModeloMemoria().getRowCount());
        assertEquals("Kernel", controlador.getModeloMemoria().getValueAt(0, 1));
        assertEquals("[reservada]", controlador.getModeloMemoria().getValueAt(0, 2));
        assertEquals("Usuario", controlador.getModeloMemoria().getValueAt(64, 1));
        assertEquals("MOV AX, 5", controlador.getModeloMemoria().getValueAt(64, 2));
        assertEquals("", controlador.getModeloMemoria().getValueAt(200, 2));
    }

    @Test
    @DisplayName("Las estadisticas quedan disponibles al terminar")
    void estadisticasDisponibles(@TempDir Path carpeta) throws Exception {
        vista.entregar(ejemploDelEnunciado(carpeta));
        controlador.alCargarArchivos();
        for (int i = 0; i < 7; i++) {
            controlador.alPasoAPaso();
        }

        assertEquals(7, controlador.obtenerEstadisticas().getTotalInstrucciones());
    }
}
