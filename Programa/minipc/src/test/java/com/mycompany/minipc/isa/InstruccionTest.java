package com.mycompany.minipc.isa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la instruccion, de sus pesos y de las busquedas de operaciones,
 * registros e interrupciones.
 */
class InstruccionTest {

    private static Instruccion movNumero(RegistroID reg, int valor) {
        return new Instruccion(OpCode.MOV, Forma.REGISTRO_NUMERO,
                List.of(Operando.registro(reg), Operando.numero(valor)), null, 1);
    }

    @Test
    @DisplayName("La instruccion conserva operacion, forma y operandos")
    void conservaLosCampos() {
        Instruccion i = movNumero(RegistroID.AX, 5);
        assertEquals(OpCode.MOV, i.getOpcode());
        assertEquals(Forma.REGISTRO_NUMERO, i.getForma());
        assertEquals(RegistroID.AX, i.getRegistro(0));
        assertEquals(5, i.getValor(1));
        assertEquals("MOV AX, 5", i.toString(), "Sin texto fuente se reconstruye");
    }

    @Test
    @DisplayName("La instruccion conserva el texto y la linea de origen")
    void conservaElOrigen() {
        Instruccion i = new Instruccion(OpCode.ADD, Forma.REGISTRO,
                List.of(Operando.registro(RegistroID.CX)), "add cx", 7);
        assertEquals("add cx", i.getTextoFuente());
        assertEquals(7, i.getNumeroLinea());
        assertEquals("add cx", i.toString());
    }

    @Test
    @DisplayName("Se rechaza una forma que la operacion no admite")
    void rechazaFormaAjena() {
        assertThrows(IllegalArgumentException.class, () -> new Instruccion(OpCode.ADD,
                Forma.REGISTRO_NUMERO,
                List.of(Operando.registro(RegistroID.BX), Operando.numero(5)), "ADD BX, 5", 1));
    }

    @Test
    @DisplayName("Se rechaza una cantidad o un tipo de operandos equivocado")
    void rechazaOperandosEquivocados() {
        assertThrows(IllegalArgumentException.class, () -> new Instruccion(OpCode.MOV,
                Forma.REGISTRO_NUMERO, List.of(Operando.registro(RegistroID.AX)), "MOV AX", 1));
        assertThrows(IllegalArgumentException.class, () -> new Instruccion(OpCode.MOV,
                Forma.REGISTRO_NUMERO,
                List.of(Operando.numero(1), Operando.numero(2)), "MOV 1, 2", 1));
        assertThrows(IllegalArgumentException.class, () -> new Instruccion(OpCode.PARAM,
                Forma.PARAMETROS, List.of(Operando.numero(1), Operando.numero(2),
                        Operando.numero(3), Operando.numero(4)), "PARAM 1, 2, 3, 4", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Instruccion(null, Forma.REGISTRO, List.of(), "?", 1));
    }

    @Test
    @DisplayName("Los pesos son los del enunciado")
    void pesos() {
        assertEquals(2, OpCode.LOAD.getPeso());
        assertEquals(2, OpCode.STORE.getPeso());
        assertEquals(1, OpCode.MOV.getPeso());
        assertEquals(3, OpCode.ADD.getPeso());
        assertEquals(3, OpCode.SUB.getPeso());
        assertEquals(1, OpCode.INC.getPeso());
        assertEquals(1, OpCode.DEC.getPeso());
        assertEquals(1, OpCode.SWAP.getPeso());
        assertEquals(2, OpCode.JMP.getPeso());
        assertEquals(2, OpCode.CMP.getPeso());
        assertEquals(2, OpCode.JE.getPeso());
        assertEquals(2, OpCode.JNE.getPeso());
        assertEquals(3, OpCode.PARAM.getPeso());
        assertEquals(1, OpCode.PUSH.getPeso());
        assertEquals(1, OpCode.POP.getPeso());
        assertEquals(2, Interrupcion.FIN_PROGRAMA.getPeso());
        assertEquals(2, Interrupcion.PANTALLA.getPeso());
        assertEquals(Interrupcion.PESO_VARIABLE, Interrupcion.TECLADO.getPeso());
        assertEquals(5, Interrupcion.ARCHIVOS.getPeso());
    }

    @Test
    @DisplayName("El peso de INT sale del codigo de interrupcion")
    void pesoDeInt() {
        Instruccion int21 = new Instruccion(OpCode.INT, Forma.INTERRUPCION,
                List.of(Operando.interrupcion(Interrupcion.ARCHIVOS)), "INT 21H", 1);
        Instruccion int20 = new Instruccion(OpCode.INT, Forma.INTERRUPCION,
                List.of(Operando.interrupcion(Interrupcion.FIN_PROGRAMA)), "INT 20H", 1);
        assertEquals(5, int21.getPeso());
        assertEquals(2, int20.getPeso());
        assertTrue(int20.esFinDePrograma());
        assertFalse(int21.esFinDePrograma());
        assertEquals(1, movNumero(RegistroID.AX, 1).getPeso());
        assertNull(movNumero(RegistroID.AX, 1).getInterrupcion());
    }

    @Test
    @DisplayName("Solo los saltos se reconocen como saltos")
    void saltos() {
        assertTrue(OpCode.JMP.esSalto());
        assertTrue(OpCode.JE.esSalto());
        assertTrue(OpCode.JNE.esSalto());
        assertFalse(OpCode.CMP.esSalto());
        assertFalse(OpCode.MOV.esSalto());
    }

    @Test
    @DisplayName("Las busquedas por nombre ignoran mayusculas y espacios")
    void busquedaPorNombre() {
        assertEquals(OpCode.MOV, OpCode.desdeMnemonico("mov"));
        assertEquals(OpCode.JNE, OpCode.desdeMnemonico("  Jne  "));
        assertEquals(RegistroID.AX, RegistroID.desdeNombre("ax"));
        assertEquals(RegistroID.DX, RegistroID.desdeNombre(" Dx "));
        assertEquals(Interrupcion.ARCHIVOS, Interrupcion.desdeCodigo("21h"));
    }

    @Test
    @DisplayName("Las busquedas rechazan mnemonicos, registros y codigos inexistentes")
    void busquedaRechazaDesconocidos() {
        assertThrows(IllegalArgumentException.class, () -> OpCode.desdeMnemonico("JUMP"));
        assertThrows(IllegalArgumentException.class, () -> OpCode.desdeMnemonico(null));
        assertThrows(IllegalArgumentException.class, () -> RegistroID.desdeNombre("EX"));
        assertThrows(IllegalArgumentException.class, () -> RegistroID.desdeNombre(null));
        assertThrows(IllegalArgumentException.class, () -> Interrupcion.desdeCodigo("21"));
    }
}
