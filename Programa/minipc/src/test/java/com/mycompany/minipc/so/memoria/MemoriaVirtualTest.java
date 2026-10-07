package com.mycompany.minipc.so.memoria;

import com.mycompany.minipc.hardware.Disco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del area de intercambio del disco.
 */
class MemoriaVirtualTest {

    private Disco disco;
    private MemoriaVirtual virtual;

    @BeforeEach
    void preparar() {
        disco = new Disco(128, 8);
        virtual = new MemoriaVirtual(disco);
    }

    @Test
    @DisplayName("Guarda con primer ajuste dentro del area de memoria virtual")
    void guardaConPrimerAjuste() {
        assertEquals(120, virtual.guardar(List.of("INC", "DEC", "INT 20H")));
        assertEquals(123, virtual.guardar(List.of("MOV AX, 1", "INT 20H")));
        assertEquals("DEC", disco.leer(121));
        assertEquals(3, virtual.mayorBloqueLibre());

        virtual.liberar(120, 3);
        assertTrue(disco.estaLibre(120));
        assertEquals(120, virtual.guardar(List.of("INC")), "Reutiliza el hueco");
        assertEquals(List.of("MOV AX, 1", "INT 20H"), virtual.leer(123, 2));
    }

    @Test
    @DisplayName("Si no hay un bloque suficiente devuelve -1 y no cambia el disco")
    void sinEspacio() {
        assertEquals(-1, virtual.guardar(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9")));
        assertEquals(8, virtual.mayorBloqueLibre());
    }

    @Test
    @DisplayName("El intercambio no puede escribir fuera del area de memoria virtual")
    void noPisaLosArchivos() {
        assertThrows(IllegalArgumentException.class,
                () -> disco.escribirMemoriaVirtual(119, "INC"));
        assertThrows(IllegalArgumentException.class,
                () -> disco.escribirMemoriaVirtual(5, "INC"));
    }

    @Test
    @DisplayName("Limpiar vacia toda el area")
    void limpiar() {
        virtual.guardar(List.of("INC", "DEC"));
        virtual.limpiar();
        assertEquals(8, virtual.mayorBloqueLibre());
    }
}
