package com.mycompany.minipc.gui;

import com.mycompany.minipc.core.BCP;
import com.mycompany.minipc.core.EstadoProceso;
import com.mycompany.minipc.isa.Instruccion;
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

        private File archivoAEntregar;
        private List<Instruccion> instrucciones = Collections.emptyList();
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
        public void mostrarInstrucciones(List<Instruccion> programa) {
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
        public File seleccionarArchivoAsm() {
            return archivoAEntregar;
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

    @Test
    @DisplayName("Cargar un archivo valido llena la tabla y deja el programa listo")
    void cargaUnProgramaValido(@TempDir Path carpeta) throws Exception {
        vista.archivoAEntregar = ejemploDelEnunciado(carpeta);

        controlador.alCargarArchivo();

        assertEquals(7, vista.instrucciones.size());
        assertEquals(7, controlador.getModeloInstrucciones().getRowCount());
        assertEquals("MOV AX, 5", controlador.getModeloInstrucciones().getValueAt(0, 1));
        assertTrue(vista.hayPrograma);
        assertFalse(vista.termino);
        assertEquals("file.asm", vista.archivoEnBarra);
        assertEquals(EstadoProceso.LISTO.name(), vista.estadoEnBarra);
        assertTrue(vista.consolaContiene("Programa cargado en la posicion 64"));
    }

    @Test
    @DisplayName("Si el usuario cancela el dialogo no pasa nada")
    void cancelarNoHaceNada() {
        vista.archivoAEntregar = null;
        controlador.alCargarArchivo();
        assertFalse(vista.hayPrograma);
        assertEquals(0, controlador.getModeloInstrucciones().getRowCount());
    }

    @Test
    @DisplayName("Un archivo con errores de sintaxis los reporta todos y no carga nada")
    void reportaErroresDeSintaxis(@TempDir Path carpeta) throws Exception {
        vista.archivoAEntregar = crearAsm(carpeta, "error-sintaxis.asm",
                "MOV AX, 5\nJUMP 100\nADD EX\nMOV BX, tres\n");

        controlador.alCargarArchivo();

        assertEquals("Errores de sintaxis", vista.tituloError);
        assertEquals(3, vista.errores.size());
        assertTrue(vista.errores.get(0).contains("Linea 2"));
        assertTrue(vista.errores.get(1).contains("Linea 3"));
        assertTrue(vista.errores.get(2).contains("Linea 4"));
        assertFalse(vista.hayPrograma, "No debio cargarse nada");
        assertEquals(0, controlador.getModeloInstrucciones().getRowCount());
    }

    @Test
    @DisplayName("Un archivo que no cabe en memoria se rechaza con su mensaje")
    void reportaMemoriaInsuficiente(@TempDir Path carpeta) throws Exception {
        controlador.alConfigurar(128, 120, 500);

        StringBuilder largo = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            largo.append("MOV AX, 1\n");
        }
        vista.archivoAEntregar = crearAsm(carpeta, "largo.asm", largo.toString());

        controlador.alCargarArchivo();

        assertEquals("Memoria insuficiente", vista.tituloError);
        assertTrue(vista.errores.get(0).contains("requiere 10"), vista.errores.get(0));
        assertTrue(vista.errores.get(0).contains("dispone de 8"), vista.errores.get(0));
        assertFalse(vista.hayPrograma);
    }

    @Test
    @DisplayName("Un archivo que no es .asm se rechaza")
    void rechazaExtensionInvalida(@TempDir Path carpeta) throws Exception {
        vista.archivoAEntregar = crearAsm(carpeta, "programa.txt", "MOV AX, 5\n");

        controlador.alCargarArchivo();

        assertEquals("No se pudo leer el archivo", vista.tituloError);
        assertFalse(vista.hayPrograma);
    }

    @Test
    @DisplayName("Paso a paso avanza una instruccion y mueve el resaltado")
    void pasoAPasoAvanza(@TempDir Path carpeta) throws Exception {
        vista.archivoAEntregar = ejemploDelEnunciado(carpeta);
        controlador.alCargarArchivo();
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
        vista.archivoAEntregar = ejemploDelEnunciado(carpeta);
        controlador.alCargarArchivo();

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
        controlador.alConfigurar(256, 64, 50);
        vista.archivoAEntregar = ejemploDelEnunciado(carpeta);
        controlador.alCargarArchivo();

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
        vista.archivoAEntregar = ejemploDelEnunciado(carpeta);
        controlador.alCargarArchivo();
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
        vista.archivoAEntregar = ejemploDelEnunciado(carpeta);
        controlador.alCargarArchivo();
        controlador.alPasoAPaso();

        controlador.alLimpiar();

        assertFalse(vista.hayPrograma);
        assertEquals(0, controlador.getModeloInstrucciones().getRowCount());
        assertEquals(0, controlador.getProcesador().getMemoria().getPosicionesUsadas());
        assertEquals("(ninguno)", vista.archivoEnBarra);
        assertEquals("SIN PROGRAMA", vista.estadoEnBarra);
    }

    @Test
    @DisplayName("Configurar cambia la memoria y la velocidad")
    void configurarCambiaLaMemoria() {
        controlador.alConfigurar(512, 128, 250);

        assertEquals(512, controlador.getProcesador().getMemoria().getTamano());
        assertEquals(128, controlador.getProcesador().getMemoria().getLimiteKernel());
        assertEquals(250, controlador.getVelocidadMs());
        assertTrue(vista.consolaContiene("512 posiciones"));
    }

    @Test
    @DisplayName("Una configuracion invalida se rechaza sin romper nada")
    void configuracionInvalida() {
        controlador.alConfigurar(128, 200, 500);

        assertEquals("Configuracion invalida", vista.tituloError);
        assertEquals(256, controlador.getProcesador().getMemoria().getTamano(),
                "La memoria no debio cambiar");
    }

    @Test
    @DisplayName("El modelo de memoria refleja el programa cargado")
    void modeloDeMemoriaRefleja(@TempDir Path carpeta) throws Exception {
        vista.archivoAEntregar = ejemploDelEnunciado(carpeta);
        controlador.alCargarArchivo();

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
        vista.archivoAEntregar = ejemploDelEnunciado(carpeta);
        controlador.alCargarArchivo();
        for (int i = 0; i < 7; i++) {
            controlador.alPasoAPaso();
        }

        assertEquals(7, controlador.obtenerEstadisticas().getTotalInstrucciones());
    }
}
