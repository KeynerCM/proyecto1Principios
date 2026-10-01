package com.mycompany.minipc.hardware;

import com.mycompany.minipc.excepciones.EjecucionException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.isa.Ensamblador;
import com.mycompany.minipc.isa.Interrupcion;
import com.mycompany.minipc.isa.OpCode;
import com.mycompany.minipc.isa.RegistroID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del ciclo de instruccion de la CPU como hardware: se le carga un
 * programa en memoria y sus registros base y alcance, sin sistema operativo.
 *
 * La prueba central recorre el programa de ejemplo del enunciado paso a
 * paso y compara AC, AX y BX contra la tabla de la lamina 6.
 */
class ProcesadorTest {

    private static final int BASE = 128;

    private Memoria memoria;
    private Procesador cpu;
    private Ensamblador ensamblador;

    @BeforeEach
    void preparar() {
        memoria = new Memoria(256, 128);
        cpu = new Procesador(memoria);
        ensamblador = new Ensamblador();
    }

    /** Escribe el programa en memoria desde BASE y le da el contexto a la CPU. */
    private void cargar(List<String> lineas) throws SintaxisException {
        ensamblador.ensamblar(lineas);
        for (int i = 0; i < lineas.size(); i++) {
            memoria.escribir(BASE + i, lineas.get(i));
        }
        cpu.cargarLimites(BASE, lineas.size());
        cpu.setPc(BASE);
    }

    private void cargarEjemplo() throws SintaxisException {
        cargar(List.of(
                "MOV AX, 5",
                "MOV BX, 3",
                "LOAD AX",
                "ADD BX",
                "SUB AX",
                "STORE AX",
                "MOV BX, -8"));
    }

    /** Avanza segundo a segundo hasta que la instruccion en curso cumple su peso. */
    private Procesador.Resultado instruccion() {
        Procesador.Resultado resultado = cpu.paso();
        while (resultado == Procesador.Resultado.EN_CURSO) {
            resultado = cpu.paso();
        }
        return resultado;
    }

    private int ax() {
        return cpu.getRegistros().leer(RegistroID.AX);
    }

    private int bx() {
        return cpu.getRegistros().leer(RegistroID.BX);
    }

    @Test
    @DisplayName("Sin contexto cargado la CPU esta libre y no ejecuta")
    void sinContexto() {
        assertFalse(cpu.tieneContexto());
        assertThrows(IllegalStateException.class, cpu::paso);
    }

    @Test
    @DisplayName("El programa del enunciado reproduce la tabla AC, AX, BX paso a paso")
    void reproduceLaTablaDelEnunciado() throws Exception {
        cargarEjemplo();

        // Estados esperados despues de cada instruccion: {AC, AX, BX}
        int[][] esperados = {
            {0, 5, 0},    // MOV AX, 5
            {0, 5, 3},    // MOV BX, 3
            {5, 5, 3},    // LOAD AX
            {8, 5, 3},    // ADD BX
            {3, 5, 3},    // SUB AX
            {3, 3, 3},    // STORE AX
            {3, 3, -8}    // MOV BX, -8
        };

        for (int i = 0; i < esperados.length; i++) {
            Procesador.Resultado resultado = instruccion();
            assertEquals(esperados[i][0], cpu.getAc(), "AC tras la instruccion " + (i + 1));
            assertEquals(esperados[i][1], ax(), "AX tras la instruccion " + (i + 1));
            assertEquals(esperados[i][2], bx(), "BX tras la instruccion " + (i + 1));
            assertEquals(i < 6 ? Procesador.Resultado.CONTINUA : Procesador.Resultado.TERMINO,
                    resultado, "Resultado de la instruccion " + (i + 1));
        }
    }

    @Test
    @DisplayName("El PC avanza una posicion por instruccion")
    void elPcAvanzaDeUnoEnUno() throws Exception {
        cargarEjemplo();
        for (int i = 0; i < 7; i++) {
            assertEquals(BASE + i, cpu.getPc(), "Antes de la instruccion " + (i + 1));
            instruccion();
        }
        assertEquals(BASE + 7, cpu.getPc());
    }

    @Test
    @DisplayName("El IR recibe el texto de la celda y la CPU lo decodifica")
    void elIrGuardaLaInstruccion() throws Exception {
        cargarEjemplo();
        assertNull(cpu.getIr());
        assertEquals("", cpu.getIrTexto());

        instruccion();
        assertEquals("MOV AX, 5", cpu.getIrTexto());
        assertEquals(OpCode.MOV, cpu.getIr().getOpcode());
        assertEquals(RegistroID.AX, cpu.getIr().getRegistro(0));
        assertEquals(5, cpu.getIr().getValor(1));
    }

    @Test
    @DisplayName("La aritmetica no esta limitada a ocho bits")
    void laAritmeticaNoDesborda() throws Exception {
        cargar(List.of("MOV AX, 200", "MOV BX, 100", "LOAD AX", "ADD BX", "SUB AX",
                "SUB AX"));
        for (int i = 0; i < 4; i++) {
            instruccion();
        }
        assertEquals(300, cpu.getAc());
        instruccion();
        instruccion();
        assertEquals(-100, cpu.getAc());
    }

    @Test
    @DisplayName("La CPU no puede traer instrucciones de la zona del kernel")
    void noEjecutaElKernel() {
        memoria.escribir(10, "INC");
        cpu.cargarLimites(10, 1);
        cpu.setPc(10);
        assertThrows(IllegalArgumentException.class, cpu::paso);
    }

    @Test
    @DisplayName("Si el PC llega a una celda que no es una instruccion, es un error")
    void celdaQueNoEsInstruccion() throws Exception {
        cargar(List.of("MOV AX, 1", "INT 20H"));
        memoria.escribir(BASE + 1, "163");

        instruccion();
        EjecucionException e = assertThrows(EjecucionException.class, cpu::paso);
        assertTrue(e.getMessage().contains("no contiene una instruccion valida"), e.getMessage());
        assertEquals("163", cpu.getIrTexto(), "El IR muestra lo que se trajo de memoria");
    }

    @Test
    @DisplayName("Limpiar deja la CPU sin contexto y con los registros en cero")
    void limpiar() throws Exception {
        cargarEjemplo();
        instruccion();
        instruccion();
        cpu.limpiar();

        assertFalse(cpu.tieneContexto());
        assertEquals(0, cpu.getPc());
        assertEquals(0, ax());
        assertEquals("", cpu.getIrTexto());
        assertEquals(0, cpu.getPila().getTamano());
    }

    @Test
    @DisplayName("Las estadisticas cuentan las instrucciones por tipo")
    void estadisticasPorOperacion() throws Exception {
        cargarEjemplo();
        while (instruccion() == Procesador.Resultado.CONTINUA) {
            // ejecuta hasta el final
        }

        Estadisticas e = cpu.getEstadisticas();
        assertEquals(7, e.getTotalInstrucciones());
        assertEquals(3, e.getConteo(OpCode.MOV));
        assertEquals(1, e.getConteo(OpCode.LOAD));
        assertEquals(1, e.getConteo(OpCode.STORE));
        assertEquals(1, e.getConteo(OpCode.ADD));
        assertEquals(1, e.getConteo(OpCode.SUB));
        assertEquals(7, e.getAccesosLectura(), "Una lectura de memoria por instruccion");
        assertEquals(4, e.getAccesosEscritura(), "Tres MOV mas un STORE");
    }

    @Test
    @DisplayName("Cada instruccion dura tantos segundos como su peso y su efecto va al final")
    void pesos() throws Exception {
        cargar(List.of("MOV AX, 5", "ADD AX", "INT 20H"));

        assertEquals(Procesador.Resultado.CONTINUA, cpu.paso(), "MOV pesa 1");
        assertEquals(5, ax());

        assertEquals(Procesador.Resultado.EN_CURSO, cpu.paso(), "ADD pesa 3: segundo 1");
        assertEquals("ADD AX", cpu.getIrTexto(), "El fetch ocurre en el primer segundo");
        assertEquals(1, cpu.getSegundosCumplidos());
        assertEquals(3, cpu.getPesoActual());
        assertEquals(0, cpu.getAc(), "El efecto todavia no se aplica");
        assertEquals(Procesador.Resultado.EN_CURSO, cpu.paso(), "segundo 2");
        assertEquals(Procesador.Resultado.CONTINUA, cpu.paso(), "segundo 3");
        assertEquals(5, cpu.getAc());

        assertEquals(Procesador.Resultado.EN_CURSO, cpu.paso(), "INT 20H pesa 2");
        assertEquals(Procesador.Resultado.LLAMADA_SISTEMA, cpu.paso());
        assertEquals(Interrupcion.FIN_PROGRAMA,
                cpu.getInterrupcionPendiente(), "La CPU deja la llamada al sistema operativo");
    }

    @Test
    @DisplayName("INT 09H tiene peso variable: la llamada dura un segundo")
    void int09DuraUnSegundo() throws Exception {
        cargar(List.of("INT 09H", "INT 20H"));
        assertEquals(Procesador.Resultado.LLAMADA_SISTEMA, cpu.paso());
        assertEquals(Interrupcion.TECLADO,
                cpu.getInterrupcionPendiente());
    }

    @Test
    @DisplayName("AH y AL son la parte alta y la parte baja de AX")
    void ahYAlSonMitadesDeAx() throws Exception {
        cargar(List.of("MOV AX, 772", "MOV AH, 3Ch", "MOV AL, 65", "MOV BX, AH", "INT 20H"));
        instruccion();
        assertEquals(3, cpu.getRegistros().leer(RegistroID.AH), "772 = 3 x 256 + 4");
        assertEquals(4, cpu.getRegistros().leer(RegistroID.AL));

        instruccion();
        assertEquals(0x3C * 256 + 4, ax(), "Escribir AH conserva AL");
        instruccion();
        assertEquals(0x3C * 256 + 65, ax(), "Escribir AL conserva AH");
        instruccion();
        assertEquals(60, bx(), "3Ch en hexadecimal es 60");
    }

    @Test
    @DisplayName("MOV DX con un texto guarda en DX la direccion donde esta el texto")
    void movConTexto() throws Exception {
        cargar(List.of("MOV AX, 1", "MOV DX, \"datos.txt\"", "INT 20H"));
        instruccion();
        instruccion();
        assertEquals(BASE + 1, cpu.getRegistros().leer(RegistroID.DX));
        assertEquals("MOV DX, \"datos.txt\"", memoria.leer(BASE + 1),
                "El nombre queda guardado en esa celda de memoria");
    }
}
