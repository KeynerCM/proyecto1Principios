package com.mycompany.minipc.isa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la instruccion y de las busquedas de operaciones y registros.
 */
class InstruccionTest {

    private static Instruccion instr(OpCode op, RegistroID reg, int operando, String texto) {
        return new Instruccion(op, reg, operando, texto, 1);
    }

    @Test
    @DisplayName("La instruccion conserva operacion, registro y operando")
    void conservaLosTresCampos() {
        Instruccion i = instr(OpCode.MOV, RegistroID.AX, 5, "MOV AX, 5");
        assertEquals(OpCode.MOV, i.getOpcode());
        assertEquals(RegistroID.AX, i.getRegistro());
        assertEquals(5, i.getOperando());
    }

    @Test
    @DisplayName("La instruccion conserva el texto y la linea de origen")
    void conservaElOrigen() {
        Instruccion i = new Instruccion(OpCode.MOV, RegistroID.CX, 100, "MOV CX, 100", 7);
        assertEquals("MOV CX, 100", i.getTextoFuente());
        assertEquals(7, i.getNumeroLinea());
        assertEquals(100, i.getOperando());
        assertEquals("MOV CX, 100", i.toString());
    }

    @Test
    @DisplayName("El operando inmediato admite valores de cualquier tamano")
    void aceptaOperandosGrandes() {
        assertEquals(300, instr(OpCode.MOV, RegistroID.BX, 300, "MOV BX, 300").getOperando());
        assertEquals(-128, instr(OpCode.MOV, RegistroID.BX, -128, "MOV BX, -128").getOperando());
    }

    @Test
    @DisplayName("Operacion y registro son obligatorios")
    void rechazaNulos() {
        assertThrows(IllegalArgumentException.class,
                () -> instr(null, RegistroID.AX, 0, "?"));
        assertThrows(IllegalArgumentException.class,
                () -> instr(OpCode.LOAD, null, 0, "?"));
    }

    @Test
    @DisplayName("Solo MOV admite operando inmediato")
    void soloMovAdmiteInmediato() {
        assertTrue(OpCode.MOV.requiereInmediato());
        assertFalse(OpCode.LOAD.requiereInmediato());
        assertFalse(OpCode.STORE.requiereInmediato());
        assertFalse(OpCode.ADD.requiereInmediato());
        assertFalse(OpCode.SUB.requiereInmediato());
        assertThrows(IllegalArgumentException.class,
                () -> instr(OpCode.ADD, RegistroID.BX, 5, "ADD BX, 5"));
    }

    @Test
    @DisplayName("Las busquedas por nombre ignoran mayusculas y espacios")
    void busquedaPorNombre() {
        assertEquals(OpCode.MOV, OpCode.desdeMnemonico("mov"));
        assertEquals(OpCode.ADD, OpCode.desdeMnemonico("  Add  "));
        assertEquals(RegistroID.AX, RegistroID.desdeNombre("ax"));
        assertEquals(RegistroID.DX, RegistroID.desdeNombre(" Dx "));
    }

    @Test
    @DisplayName("Las busquedas rechazan mnemonicos y registros inexistentes")
    void busquedaRechazaDesconocidos() {
        assertThrows(IllegalArgumentException.class, () -> OpCode.desdeMnemonico("JUMP"));
        assertThrows(IllegalArgumentException.class, () -> OpCode.desdeMnemonico(null));
        assertThrows(IllegalArgumentException.class, () -> RegistroID.desdeNombre("EX"));
        assertThrows(IllegalArgumentException.class, () -> RegistroID.desdeNombre(null));
    }
}
