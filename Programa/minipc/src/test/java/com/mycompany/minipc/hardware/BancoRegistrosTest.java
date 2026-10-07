package com.mycompany.minipc.hardware;

import com.mycompany.minipc.isa.RegistroID;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    @DisplayName("Los estados finales se distinguen de los demas")
    void estadosFinales() {
        assertEquals(7, EstadoProceso.values().length, "Modelo de siete estados");
        assertTrue(EstadoProceso.FINALIZADO.esFinal());
        for (EstadoProceso estado : EstadoProceso.values()) {
            if (estado != EstadoProceso.FINALIZADO) {
                assertFalse(estado.esFinal(), estado.name());
            }
        }
    }
}
