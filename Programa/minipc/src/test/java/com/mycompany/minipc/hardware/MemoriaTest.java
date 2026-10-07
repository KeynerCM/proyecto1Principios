package com.mycompany.minipc.hardware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la memoria como hardware: zonas, celdas de texto, conversion a
 * numero, proteccion del kernel y validacion del tamano.
 */
class MemoriaTest {

    private Memoria memoria;

    @BeforeEach
    void preparar() {
        memoria = new Memoria(256, 128);
    }

    @Test
    @DisplayName("La memoria se divide en kernel y usuario segun el limite recibido")
    void separacionDeZonas() {
        assertEquals(256, memoria.getTamano());
        assertEquals(128, memoria.getLimiteKernel());
        assertEquals(128, memoria.getEspacioUsuario());
        assertTrue(memoria.esDireccionKernel(0));
        assertTrue(memoria.esDireccionKernel(127));
        assertFalse(memoria.esDireccionKernel(128));
        assertFalse(memoria.esDireccionKernel(-1));
    }

    @Test
    @DisplayName("Todas las celdas arrancan vacias")
    void arrancaVacia() {
        assertTrue(memoria.estaLibre(0));
        assertTrue(memoria.estaLibre(255));
        assertEquals("", memoria.leer(130));
        assertEquals(0, memoria.getPosicionesUsadas());
    }

    @Test
    @DisplayName("Cada celda guarda texto: una instruccion o un numero")
    void celdasDeTexto() {
        memoria.escribir(130, "MOV AX, 5");
        memoria.escribirEntero(131, -15);

        assertEquals("MOV AX, 5", memoria.leer(130));
        assertEquals("-15", memoria.leer(131));
        assertEquals(-15, memoria.leerEntero(131));
        assertEquals(0, memoria.leerEntero(132), "Una celda vacia vale cero");
        assertEquals(2, memoria.getPosicionesUsadas());
    }

    @Test
    @DisplayName("Leer como numero una celda con una instruccion se informa")
    void leerEnteroDeUnaInstruccion() {
        memoria.escribir(130, "MOV AX, 5");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> memoria.leerEntero(130));
        assertTrue(e.getMessage().contains("MOV AX, 5"), e.getMessage());
    }

    @Test
    @DisplayName("El proceso de usuario no puede leer la zona de kernel")
    void protegeLaZonaDeKernel() {
        memoria.escribir(0, "7");
        assertThrows(IllegalArgumentException.class, () -> memoria.leerComoUsuario(0));
        assertThrows(IllegalArgumentException.class, () -> memoria.leerComoUsuario(127));
        // Desde fuera del modo usuario si se puede, para poder mostrarla en pantalla.
        assertEquals("7", memoria.leer(0));
    }

    @Test
    @DisplayName("Las direcciones fuera de la memoria se rechazan")
    void rechazaDireccionesInvalidas() {
        assertThrows(IndexOutOfBoundsException.class, () -> memoria.leer(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> memoria.leer(256));
        assertThrows(IndexOutOfBoundsException.class, () -> memoria.escribir(1000, "1"));
    }

    @Test
    @DisplayName("Limpiar la zona de usuario no toca la del kernel")
    void limpiarRespetaElKernel() {
        memoria.escribirEntero(10, 42);
        memoria.escribir(130, "INC");
        memoria.limpiarZonaUsuario();

        assertEquals(0, memoria.getPosicionesUsadas());
        assertEquals("42", memoria.leer(10), "El kernel conserva lo que tenia");
    }

    @Test
    @DisplayName("El porcentaje de uso se calcula sobre la zona de usuario")
    void porcentajeDeUso() {
        for (int i = 128; i < 192; i++) {
            memoria.escribir(i, "INC");
        }
        assertEquals(50, memoria.getPorcentajeUso(), "64 de 128 posiciones es la mitad");
    }

    @Test
    @DisplayName("A los programas les deben quedar al menos 32 posiciones")
    void rechazaConfiguracionInvalida() {
        assertTrue(Memoria.validar(160, 128).isEmpty());
        assertEquals(1, Memoria.validar(159, 128).size());
        assertTrue(Memoria.validar(159, 128).get(0).contains("al menos 160"));
        assertEquals(1, Memoria.validar(256, 0).size(), "El kernel ocupa al menos una");
        assertThrows(IllegalArgumentException.class, () -> memoria.redimensionar(128, 128));
    }

    @Test
    @DisplayName("Redimensionar deja la memoria vacia con el nuevo tamano")
    void redimensionarReconstruyeZonas() {
        memoria.escribir(130, "INC");
        memoria.redimensionar(512, 128);

        assertEquals(512, memoria.getTamano());
        assertEquals(384, memoria.getEspacioUsuario());
        assertEquals(0, memoria.getPosicionesUsadas());
    }
}
