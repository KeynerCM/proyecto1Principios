package com.mycompany.minipc.hardware;

import com.mycompany.minipc.excepciones.DesbordamientoException;
import com.mycompany.minipc.excepciones.EjecucionException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.isa.Ensamblador;
import com.mycompany.minipc.isa.RegistroID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la ejecucion de las instrucciones que no dependen de los
 * dispositivos de entrada y salida: incrementos, intercambio, comparacion,
 * saltos, pila y fin de programa.
 */
class EjecucionInstruccionesTest {

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

    /** Comprueba con el ensamblador que las lineas son validas y las devuelve. */
    private List<String> validas(List<String> lineas) throws SintaxisException {
        ensamblador.ensamblar(lineas);
        return lineas;
    }

    /** Escribe el programa en memoria desde BASE y le da el contexto a la CPU. */
    private void cargar(List<String> lineas) {
        for (int i = 0; i < lineas.size(); i++) {
            memoria.escribir(BASE + i, lineas.get(i));
        }
        cpu.cargarLimites(BASE, lineas.size());
        cpu.setPc(BASE);
    }

    /** Carga el programa y lo ejecuta hasta el final, con un tope de pasos. */
    private void ejecutar(String... lineas) throws Exception {
        cargar(validas(List.of(lineas)));
        boolean termino = false;
        for (int i = 0; i < 1000 && !termino; i++) {
            termino = cpu.paso() == Procesador.Resultado.TERMINO;
        }
        assertTrue(termino, "El programa debio terminar");
    }

    private int ejecutadas() {
        return cpu.getEstadisticas().getTotalInstrucciones();
    }

    private int reg(RegistroID id) {
        return cpu.getRegistros().leer(id);
    }

    @Test
    @DisplayName("MOV entre registros copia el valor")
    void movEntreRegistros() throws Exception {
        ejecutar("MOV AX, 9", "MOV BX, AX", "INT 20H");
        assertEquals(9, reg(RegistroID.BX));
        assertEquals(9, reg(RegistroID.AX));
    }

    @Test
    @DisplayName("INC y DEC sin registro actuan sobre el AC")
    void incDecSobreElAcumulador() throws Exception {
        ejecutar("MOV AX, 5", "LOAD AX", "INC", "INC", "DEC", "INT 20H");
        assertEquals(6, cpu.getAc());
        assertEquals(5, reg(RegistroID.AX), "El registro no cambia");
    }

    @Test
    @DisplayName("INC y DEC con registro actuan sobre ese registro")
    void incDecSobreUnRegistro() throws Exception {
        ejecutar("MOV CX, 10", "INC CX", "INC CX", "DEC DX", "INT 20H");
        assertEquals(12, reg(RegistroID.CX));
        assertEquals(-1, reg(RegistroID.DX));
        assertEquals(0, cpu.getAc(), "El AC no cambia");
    }

    @Test
    @DisplayName("SWAP intercambia los valores")
    void swap() throws Exception {
        ejecutar("MOV AX, 1", "MOV BX, 2", "SWAP AX, BX", "INT 20H");
        assertEquals(2, reg(RegistroID.AX));
        assertEquals(1, reg(RegistroID.BX));
    }

    @Test
    @DisplayName("CMP deja la bandera de cero segun la igualdad")
    void cmp() throws Exception {
        cargar(validas(List.of(
                "MOV AX, 3", "MOV BX, 3", "CMP AX, BX", "MOV BX, 4", "CMP AX, BX",
                "INT 20H")));
        for (int i = 0; i < 3; i++) {
            cpu.paso();
        }
        assertTrue(cpu.getZf());
        cpu.paso();
        cpu.paso();
        assertFalse(cpu.getZf());
    }

    @Test
    @DisplayName("JMP salta hacia adelante y se saltea instrucciones")
    void jmpAdelante() throws Exception {
        ejecutar("MOV AX, 1", "JMP +1", "MOV AX, 99", "INT 20H");
        assertEquals(1, reg(RegistroID.AX), "MOV AX, 99 no debio ejecutarse");
    }

    @Test
    @DisplayName("JE salta solo si la comparacion dio igual")
    void je() throws Exception {
        ejecutar("MOV AX, 2", "MOV BX, 2", "CMP AX, BX", "JE +1", "MOV CX, 7", "INT 20H");
        assertEquals(0, reg(RegistroID.CX), "Eran iguales: debio saltar");

        preparar();
        ejecutar("MOV AX, 2", "MOV BX, 3", "CMP AX, BX", "JE +1", "MOV CX, 7", "INT 20H");
        assertEquals(7, reg(RegistroID.CX), "Eran distintos: no debio saltar");
    }

    @Test
    @DisplayName("JNE hacia atras arma un bucle que cuenta hasta 5")
    void bucleConJne() throws Exception {
        ejecutar(
                "MOV CX, 0",
                "MOV DX, 5",
                "INC CX",
                "CMP CX, DX",
                "JNE -3",
                "INT 20H");
        assertEquals(5, reg(RegistroID.CX));
        // 2 MOV + 5 vueltas de (INC, CMP, JNE) + INT 20H
        assertEquals(18, ejecutadas());
    }

    @Test
    @DisplayName("Un salto fuera del programa se detiene como desbordamiento")
    void saltoFueraDelProgramaEnEjecucion() throws Exception {
        // El ensamblador ya rechaza este salto; se carga sin validar para
        // probar que el procesador tambien protege la region del proceso.
        cargar(List.of("JMP +10"));

        DesbordamientoException e = assertThrows(DesbordamientoException.class, cpu::paso);
        assertTrue(e.getMessage().contains("fuera del programa"), e.getMessage());
    }

    @Test
    @DisplayName("PUSH y POP guardan y recuperan en orden inverso")
    void pushPop() throws Exception {
        ejecutar("MOV AX, 1", "MOV BX, 2", "PUSH AX", "PUSH BX", "POP CX", "POP DX", "INT 20H");
        assertEquals(2, reg(RegistroID.CX), "El ultimo en entrar sale primero");
        assertEquals(1, reg(RegistroID.DX));
        assertEquals(0, cpu.getPila().getTamano());
    }

    @Test
    @DisplayName("PARAM apila los valores y POP los devuelve del ultimo al primero")
    void param() throws Exception {
        ejecutar("PARAM 10, 20, 30", "POP AX", "POP BX", "POP CX", "INT 20H");
        assertEquals(30, reg(RegistroID.AX));
        assertEquals(20, reg(RegistroID.BX));
        assertEquals(10, reg(RegistroID.CX));
    }

    @Test
    @DisplayName("Un sexto valor desborda la pila de tamano 5")
    void desbordamientoDePila() throws Exception {
        cargar(validas(List.of(
                "PUSH AX", "PUSH AX", "PUSH AX", "PUSH AX", "PUSH AX", "PUSH AX",
                "INT 20H")));
        for (int i = 0; i < 5; i++) {
            cpu.paso();
        }
        DesbordamientoException e = assertThrows(DesbordamientoException.class, cpu::paso);
        assertTrue(e.getMessage().contains("Desbordamiento de pila"), e.getMessage());
        assertEquals(Pila.CAPACIDAD, cpu.getPila().getTamano());
    }

    @Test
    @DisplayName("PARAM que no cabe no deja la pila a medio llenar")
    void paramQueNoCabe() throws Exception {
        cargar(validas(List.of(
                "PARAM 1, 2, 3", "PARAM 4, 5, 6", "INT 20H")));
        cpu.paso();
        assertThrows(DesbordamientoException.class, cpu::paso);
        assertEquals(3, cpu.getPila().getTamano(), "El segundo PARAM no apilo nada");
    }

    @Test
    @DisplayName("POP con la pila vacia es un error")
    void popConPilaVacia() throws Exception {
        cargar(validas(List.of("POP AX", "INT 20H")));
        DesbordamientoException e = assertThrows(DesbordamientoException.class, cpu::paso);
        assertTrue(e.getMessage().contains("Pila vacia"), e.getMessage());
    }

    @Test
    @DisplayName("INT 20H termina el programa aunque queden instrucciones")
    void int20Termina() throws Exception {
        ejecutar("MOV AX, 1", "INT 20H", "MOV AX, 99");
        assertEquals(1, reg(RegistroID.AX));
        assertEquals(2, ejecutadas());
    }

    @Test
    @DisplayName("Las interrupciones de entrada y salida todavia no se ejecutan")
    void interrupcionesPendientes() throws Exception {
        cargar(validas(List.of("INT 10H", "INT 20H")));
        EjecucionException e = assertThrows(EjecucionException.class, cpu::paso);
        assertTrue(e.getMessage().contains("todavia no esta disponible"), e.getMessage());
    }
}
