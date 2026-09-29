package com.mycompany.minipc.core;

import com.mycompany.minipc.isa.Forma;
import com.mycompany.minipc.isa.Instruccion;
import com.mycompany.minipc.isa.OpCode;
import com.mycompany.minipc.isa.Operando;
import com.mycompany.minipc.isa.RegistroID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de los registros y de la celda de memoria.
 */
class BancoRegistrosTest {

    private BancoRegistros banco;

    @BeforeEach
    void prepararBanco() {
        banco = new BancoRegistros();
    }

    @Test
    @DisplayName("Los cuatro registros arrancan en cero")
    void arrancanEnCero() {
        for (RegistroID id : RegistroID.values()) {
            assertEquals(0, banco.leer(id));
        }
        assertEquals(4, banco.todos().size());
    }

    @Test
    @DisplayName("Escribir y leer devuelve el mismo valor")
    void escrituraYLectura() {
        banco.escribir(RegistroID.AX, 5);
        banco.escribir(RegistroID.BX, -8);
        assertEquals(5, banco.leer(RegistroID.AX));
        assertEquals(-8, banco.leer(RegistroID.BX));
        assertEquals(0, banco.leer(RegistroID.CX));
    }

    @Test
    @DisplayName("El registro admite valores fuera de ocho bits")
    void admiteValoresGrandes() {
        banco.escribir(RegistroID.AX, 255);
        banco.escribir(RegistroID.BX, -1000);
        assertEquals(255, banco.leer(RegistroID.AX));
        assertEquals(-1000, banco.leer(RegistroID.BX));
    }

    @Test
    @DisplayName("El reset pone todo en cero")
    void resetLimpiaTodo() {
        banco.escribir(RegistroID.AX, 100);
        banco.escribir(RegistroID.DX, -100);
        banco.reset();
        for (RegistroID id : RegistroID.values()) {
            assertEquals(0, banco.leer(id));
        }
    }

    @Test
    @DisplayName("El recorrido conserva el orden AX, BX, CX, DX")
    void ordenDeRecorrido() {
        List<RegistroID> orden = new ArrayList<>();
        for (Registro registro : banco.todos()) {
            orden.add(registro.getId());
        }
        assertEquals(List.of(RegistroID.AX, RegistroID.BX,
                RegistroID.CX, RegistroID.DX), orden);
    }

    @Test
    @DisplayName("La instantanea es una copia independiente")
    void instantaneaIndependiente() {
        banco.escribir(RegistroID.AX, 7);
        Map<RegistroID, Integer> copia = banco.instantanea();
        assertEquals(7, copia.get(RegistroID.AX));

        banco.escribir(RegistroID.AX, 99);
        assertEquals(7, copia.get(RegistroID.AX), "La copia no debio seguir al banco");
        assertEquals(99, banco.leer(RegistroID.AX));
    }

    @Test
    @DisplayName("La celda de memoria arranca libre y vacia")
    void celdaArrancaLibre() {
        CeldaMemoria celda = new CeldaMemoria();
        assertTrue(celda.estaLibre());
        assertEquals(CeldaMemoria.Tipo.LIBRE, celda.getTipo());
        assertNull(celda.getInstruccion());
        assertEquals(0, celda.getValor());
    }

    @Test
    @DisplayName("La celda guarda la instruccion, el tipo y la etiqueta")
    void celdaGuardaContenido() {
        CeldaMemoria celda = new CeldaMemoria();
        Instruccion mov = movAx5();
        celda.escribir(mov);

        assertFalse(celda.estaLibre());
        assertEquals(CeldaMemoria.Tipo.INSTRUCCION, celda.getTipo());
        assertEquals("MOV AX, 5", celda.getEtiqueta());
        assertSame(mov, celda.getInstruccion());
    }

    @Test
    @DisplayName("Escribir un valor descarta la instruccion anterior")
    void celdaGuardaValor() {
        CeldaMemoria celda = new CeldaMemoria();
        celda.escribir(movAx5());
        celda.escribir(42, CeldaMemoria.Tipo.DATO, "42");

        assertEquals(CeldaMemoria.Tipo.DATO, celda.getTipo());
        assertEquals(42, celda.getValor());
        assertNull(celda.getInstruccion());
    }

    @Test
    @DisplayName("La celda se puede volver a dejar libre")
    void celdaSeLimpia() {
        CeldaMemoria celda = new CeldaMemoria();
        celda.escribir(movAx5());
        celda.limpiar();
        assertTrue(celda.estaLibre());
        assertNull(celda.getInstruccion());
        assertEquals("", celda.getEtiqueta());
    }

    private static Instruccion movAx5() {
        return new Instruccion(OpCode.MOV, Forma.REGISTRO_NUMERO,
                List.of(Operando.registro(RegistroID.AX), Operando.numero(5)), "MOV AX, 5", 1);
    }

    @Test
    @DisplayName("Los estados finales se distinguen de los demas")
    void estadosFinales() {
        assertTrue(EstadoProceso.TERMINADO.esFinal());
        assertTrue(EstadoProceso.BLOQUEADO_ERROR.esFinal());
        assertFalse(EstadoProceso.NUEVO.esFinal());
        assertFalse(EstadoProceso.LISTO.esFinal());
        assertFalse(EstadoProceso.EJECUCION.esFinal());
    }
}
