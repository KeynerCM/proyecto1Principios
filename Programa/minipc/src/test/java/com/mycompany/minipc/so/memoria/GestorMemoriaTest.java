package com.mycompany.minipc.so.memoria;

import com.mycompany.minipc.hardware.Memoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la asignacion de la zona de usuario con primer ajuste.
 */
class GestorMemoriaTest {

    private Memoria memoria;
    private GestorMemoria gestor;

    @BeforeEach
    void preparar() {
        memoria = new Memoria(256, 128);
        gestor = new GestorMemoria(memoria);
    }

    private static List<String> programaDe(int lineas) {
        List<String> programa = new ArrayList<>();
        for (int i = 0; i < lineas; i++) {
            programa.add("MOV AX, " + i);
        }
        return programa;
    }

    @Test
    @DisplayName("El primer programa va al inicio de la zona de usuario, una linea por celda")
    void asignaAlInicio() {
        int base = gestor.asignar(programaDe(7));

        assertEquals(128, base);
        assertEquals("MOV AX, 0", memoria.leer(128));
        assertEquals("MOV AX, 6", memoria.leer(134));
        assertTrue(memoria.estaLibre(135));
        assertEquals(7, memoria.getPosicionesUsadas());
    }

    @Test
    @DisplayName("Los programas siguientes van a continuacion")
    void asignaContiguo() {
        gestor.asignar(programaDe(5));
        assertEquals(133, gestor.asignar(programaDe(3)));
    }

    @Test
    @DisplayName("Si no hay un bloque suficiente devuelve -1 y la memoria no cambia")
    void sinEspacio() {
        gestor.asignar(programaDe(120));
        assertEquals(-1, gestor.asignar(programaDe(10)));
        assertEquals(120, memoria.getPosicionesUsadas());
        assertEquals(8, gestor.mayorBloqueLibre());
    }

    @Test
    @DisplayName("Liberar deja el hueco disponible y el primer ajuste lo reutiliza")
    void liberarYReutilizar() {
        int a = gestor.asignar(programaDe(5));
        gestor.asignar(programaDe(3));
        gestor.liberar(a, 5);

        assertTrue(memoria.estaLibre(128));
        assertEquals(128, gestor.asignar(programaDe(4)), "Cabe en el hueco de 5");
        assertEquals(136, gestor.asignar(programaDe(2)), "No cabe en el hueco de 1");
    }

    @Test
    @DisplayName("Un programa mas grande que toda la zona de usuario no cabe nunca")
    void cabeAlgunaVez() {
        assertTrue(gestor.cabeAlgunaVez(128));
        assertFalse(gestor.cabeAlgunaVez(129));
    }
}
