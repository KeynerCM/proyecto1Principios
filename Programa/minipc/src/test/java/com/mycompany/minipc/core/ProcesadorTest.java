package com.mycompany.minipc.core;

import com.mycompany.minipc.excepciones.MemoriaInsuficienteException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.isa.Ensamblador;
import com.mycompany.minipc.isa.Instruccion;
import com.mycompany.minipc.isa.OpCode;
import com.mycompany.minipc.isa.RegistroID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del ciclo de instruccion.
 *
 * La prueba central recorre el programa de ejemplo del enunciado paso a
 * paso y compara AC, AX y BX contra la tabla de la lamina 6. Si los siete
 * estados coinciden, el nucleo del simulador es correcto.
 */
class ProcesadorTest {

    private Procesador cpu;
    private Ensamblador ensamblador;

    @BeforeEach
    void preparar() {
        cpu = new Procesador();
        ensamblador = new Ensamblador();
    }

    private void cargarEjemplo() throws SintaxisException, MemoriaInsuficienteException {
        cpu.cargar(ensamblador.ensamblar(List.of(
                "MOV AX, 5",
                "MOV BX, 3",
                "LOAD AX",
                "ADD BX",
                "SUB AX",
                "STORE AX",
                "MOV BX, -8")), "file.asm");
    }

    private int ax() {
        return cpu.getRegistros().leer(RegistroID.AX);
    }

    private int bx() {
        return cpu.getRegistros().leer(RegistroID.BX);
    }

    @Test
    @DisplayName("Al cargar, el procesador queda listo en la base del usuario")
    void estadoInicialTrasCargar() throws Exception {
        cargarEjemplo();

        assertEquals(EstadoProceso.LISTO, cpu.getEstado());
        assertEquals(64, cpu.getDireccionBase());
        assertEquals(64, cpu.getPc());
        assertEquals(7, cpu.getLimite());
        assertEquals(0, cpu.getAc());
        assertEquals(0, cpu.getInstruccionesEjecutadas());
        assertTrue(cpu.hayPrograma());
        assertFalse(cpu.haTerminado());
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
            cpu.paso();
            assertEquals(esperados[i][0], cpu.getAc(), "AC tras la instruccion " + (i + 1));
            assertEquals(esperados[i][1], ax(), "AX tras la instruccion " + (i + 1));
            assertEquals(esperados[i][2], bx(), "BX tras la instruccion " + (i + 1));
        }

        assertEquals(EstadoProceso.TERMINADO, cpu.getEstado());
        assertTrue(cpu.haTerminado());
        assertEquals(7, cpu.getInstruccionesEjecutadas());
    }

    @Test
    @DisplayName("El PC avanza una posicion por instruccion")
    void elPcAvanzaDeUnoEnUno() throws Exception {
        cargarEjemplo();
        for (int i = 0; i < 7; i++) {
            assertEquals(64 + i, cpu.getPc(), "Antes de la instruccion " + (i + 1));
            cpu.paso();
        }
        assertEquals(71, cpu.getPc());
    }

    @Test
    @DisplayName("El IR guarda la instruccion en curso")
    void elIrGuardaLaInstruccion() throws Exception {
        cargarEjemplo();
        assertNull(cpu.getIr());
        assertEquals("", cpu.getIrTexto());

        cpu.paso();
        assertEquals(OpCode.MOV, cpu.getIr().getOpcode());
        assertEquals(RegistroID.AX, cpu.getIr().getRegistro(0));
        assertEquals(5, cpu.getIr().getValor(1));
        assertEquals("MOV AX, 5", cpu.getIrTexto());
        assertEquals("MOV AX, 5", cpu.getBcp().getIrTexto());
    }

    @Test
    @DisplayName("Paso devuelve false cuando ya no queda nada por ejecutar")
    void pasoAvisaElFinal() throws Exception {
        cargarEjemplo();
        for (int i = 0; i < 6; i++) {
            assertTrue(cpu.paso(), "Todavia quedaban instrucciones");
        }
        assertFalse(cpu.paso(), "La septima instruccion es la ultima");
        assertFalse(cpu.paso(), "Ya no debe ejecutar nada mas");
        assertEquals(7, cpu.getInstruccionesEjecutadas());
    }

    @Test
    @DisplayName("El BCP refleja el contexto despues de cada instruccion")
    void elBcpSigueAlProcesador() throws Exception {
        cargarEjemplo();
        assertEquals(1, cpu.getBcp().getPid());
        assertEquals("file.asm", cpu.getBcp().getNombrePrograma());
        assertEquals(64, cpu.getBcp().getDireccionBase());
        assertEquals(7, cpu.getBcp().getLimite());

        cpu.paso();
        cpu.paso();
        cpu.paso();

        assertEquals(5, cpu.getBcp().getAc());
        assertEquals(5, cpu.getBcp().getRegistro(RegistroID.AX));
        assertEquals(3, cpu.getBcp().getRegistro(RegistroID.BX));
        assertEquals(0, cpu.getBcp().getRegistro(RegistroID.CX));
        assertEquals(67, cpu.getBcp().getPc());
        assertEquals(3, cpu.getBcp().getInstruccionesEjecutadas());
        assertEquals(EstadoProceso.EJECUCION, cpu.getBcp().getEstado());
    }

    @Test
    @DisplayName("La aritmetica ya no esta limitada a ocho bits")
    void laAritmeticaNoDesborda() throws Exception {
        cpu.cargar(ensamblador.ensamblar(List.of(
                "MOV AX, 200",
                "MOV BX, 100",
                "LOAD AX",
                "ADD BX",
                "SUB AX",
                "SUB AX")), "grande.asm");

        cpu.paso();
        cpu.paso();
        cpu.paso();
        cpu.paso();
        assertEquals(300, cpu.getAc());

        cpu.paso();
        cpu.paso();
        assertEquals(-100, cpu.getAc());
        assertEquals(EstadoProceso.TERMINADO, cpu.getEstado());
    }

    @Test
    @DisplayName("Reiniciar vuelve al inicio sin descargar el programa")
    void reiniciarConservaElPrograma() throws Exception {
        cargarEjemplo();
        cpu.paso();
        cpu.paso();
        cpu.paso();

        cpu.reset();

        assertEquals(64, cpu.getPc());
        assertEquals(0, cpu.getAc());
        assertEquals(0, ax());
        assertEquals(0, cpu.getInstruccionesEjecutadas());
        assertEquals(EstadoProceso.LISTO, cpu.getEstado());
        assertTrue(cpu.hayPrograma(), "El programa sigue en memoria");
        assertEquals(7, cpu.getMemoria().getPosicionesUsadas());
    }

    @Test
    @DisplayName("Limpiar descarga el programa de la memoria")
    void limpiarDescargaElPrograma() throws Exception {
        cargarEjemplo();
        cpu.paso();
        cpu.limpiar();

        assertFalse(cpu.hayPrograma());
        assertEquals(0, cpu.getMemoria().getPosicionesUsadas());
        assertEquals(EstadoProceso.NUEVO, cpu.getEstado());
        assertFalse(cpu.paso(), "Sin programa no hay nada que ejecutar");
    }

    @Test
    @DisplayName("Las estadisticas cuentan las instrucciones por tipo")
    void estadisticasPorOperacion() throws Exception {
        cargarEjemplo();
        while (cpu.paso()) {
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
    @DisplayName("Los observadores reciben las fases fetch y execute")
    void notificaALosObservadores() throws Exception {
        List<Fase> recibidas = new ArrayList<>();
        cpu.agregarObservador((procesador, fase) -> recibidas.add(fase));

        cargarEjemplo();
        assertEquals(List.of(Fase.CARGA), recibidas);

        recibidas.clear();
        cpu.paso();
        assertEquals(List.of(Fase.FETCH, Fase.EXECUTE), recibidas);

        recibidas.clear();
        cpu.reset();
        assertEquals(List.of(Fase.REINICIO), recibidas);
    }

    @Test
    @DisplayName("El indice de la instruccion actual sigue al PC")
    void indiceDeLaInstruccionActual() throws Exception {
        cargarEjemplo();
        assertEquals(0, cpu.getIndiceInstruccionActual());
        cpu.paso();
        assertEquals(1, cpu.getIndiceInstruccionActual());
        while (cpu.paso()) {
            // ejecuta hasta el final
        }
        assertEquals(-1, cpu.getIndiceInstruccionActual(), "Ya no hay instruccion pendiente");
    }

    @Test
    @DisplayName("Un programa que no cabe en memoria no se carga")
    void rechazaProgramaQueNoCabe() throws Exception {
        cpu.configurarMemoria(128, 120);

        List<String> largo = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            largo.add("MOV AX, 1");
        }
        List<Instruccion> programa = ensamblador.ensamblar(largo);

        assertThrows(MemoriaInsuficienteException.class,
                () -> cpu.cargar(programa, "largo.asm"));
        assertFalse(cpu.hayPrograma());
    }

    @Test
    @DisplayName("Cada programa cargado recibe un PID nuevo")
    void pidIncremental() throws Exception {
        cargarEjemplo();
        assertEquals(1, cpu.getBcp().getPid());
        cargarEjemplo();
        assertEquals(2, cpu.getBcp().getPid());
    }
}
