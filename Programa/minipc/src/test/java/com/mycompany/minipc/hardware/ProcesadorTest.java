package com.mycompany.minipc.hardware;

import com.mycompany.minipc.excepciones.EjecucionException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.isa.Ensamblador;
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
            Procesador.Resultado resultado = cpu.paso();
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
            cpu.paso();
        }
        assertEquals(BASE + 7, cpu.getPc());
    }

    @Test
    @DisplayName("El IR recibe el texto de la celda y la CPU lo decodifica")
    void elIrGuardaLaInstruccion() throws Exception {
        cargarEjemplo();
        assertNull(cpu.getIr());
        assertEquals("", cpu.getIrTexto());

        cpu.paso();
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
            cpu.paso();
        }
        assertEquals(300, cpu.getAc());
        cpu.paso();
        cpu.paso();
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

        cpu.paso();
        EjecucionException e = assertThrows(EjecucionException.class, cpu::paso);
        assertTrue(e.getMessage().contains("no contiene una instruccion valida"), e.getMessage());
        assertEquals("163", cpu.getIrTexto(), "El IR muestra lo que se trajo de memoria");
    }

    @Test
    @DisplayName("Limpiar deja la CPU sin contexto y con los registros en cero")
    void limpiar() throws Exception {
        cargarEjemplo();
        cpu.paso();
        cpu.paso();
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
        while (cpu.paso() == Procesador.Resultado.CONTINUA) {
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
}
