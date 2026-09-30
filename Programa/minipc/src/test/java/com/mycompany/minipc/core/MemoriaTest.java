package com.mycompany.minipc.core;

import com.mycompany.minipc.excepciones.MemoriaInsuficienteException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.isa.Ensamblador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la memoria: zonas, validacion de espacio y carga de programas.
 */
class MemoriaTest {

    private Memoria memoria;
    private Ensamblador ensamblador;

    @BeforeEach
    void preparar() {
        memoria = new Memoria();
        ensamblador = new Ensamblador();
    }

    private List<String> programaDeEjemplo() throws SintaxisException {
        return validas(List.of(
                "MOV AX, 5",
                "MOV BX, 3",
                "LOAD AX",
                "ADD BX",
                "SUB AX",
                "STORE AX",
                "MOV BX, -8"));
    }

    private List<String> programaDe(int lineas) throws SintaxisException {
        List<String> texto = new ArrayList<>();
        for (int i = 0; i < lineas; i++) {
            texto.add("MOV AX, 1");
        }
        return validas(texto);
    }

    /** Comprueba con el ensamblador que las lineas son validas y las devuelve. */
    private List<String> validas(List<String> lineas) throws SintaxisException {
        ensamblador.ensamblar(lineas);
        return lineas;
    }

    @Test
    @DisplayName("La configuracion por defecto es la del enunciado")
    void configuracionPorDefecto() {
        assertEquals(256, memoria.getTamano());
        assertEquals(64, memoria.getLimiteKernel());
        assertEquals(192, memoria.getEspacioUsuario());
    }

    @Test
    @DisplayName("Las direcciones 0 a 63 son del kernel y de 64 en adelante del usuario")
    void separacionDeZonas() {
        assertTrue(memoria.esDireccionKernel(0));
        assertTrue(memoria.esDireccionKernel(63));
        assertFalse(memoria.esDireccionKernel(64));
        assertFalse(memoria.esDireccionKernel(255));

        assertTrue(memoria.estaLibre(0), "El kernel arranca vacio");
        assertTrue(memoria.estaLibre(64));
    }

    @Test
    @DisplayName("El programa se carga al inicio de la zona de usuario")
    void cargaEnLaBaseDelUsuario() throws Exception {
        int base = memoria.cargarPrograma(programaDeEjemplo());

        assertEquals(64, base);
        assertEquals("MOV AX, 5", memoria.leer(64), "La celda guarda el texto de la instruccion");
        assertEquals("MOV BX, -8", memoria.leer(70));
        assertTrue(memoria.estaLibre(71), "La celda siguiente debe quedar libre");
        assertEquals(7, memoria.getPosicionesUsadas());
    }

    @Test
    @DisplayName("Cada linea de programa ocupa exactamente una posicion")
    void unaLineaUnaPosicion() throws Exception {
        List<String> programa = programaDeEjemplo();
        memoria.cargarPrograma(programa);
        assertEquals(programa.size(), memoria.getPosicionesUsadas());
    }

    @Test
    @DisplayName("Un programa que no cabe se rechaza sin tocar la memoria")
    void rechazaProgramaQueNoCabe() throws Exception {
        // 128 posiciones con kernel hasta 119 deja 8 libres para el usuario.
        memoria.redimensionar(128, 120);
        assertEquals(8, memoria.getEspacioUsuario());

        MemoriaInsuficienteException e = assertThrows(MemoriaInsuficienteException.class,
                () -> memoria.cargarPrograma(programaDe(10)));

        assertEquals(10, e.getRequeridas());
        assertEquals(8, e.getDisponibles());
        assertEquals(0, memoria.getPosicionesUsadas(), "La carga debe ser atomica");
    }

    @Test
    @DisplayName("Un programa que cabe justo se acepta")
    void aceptaProgramaQueCabeJusto() throws Exception {
        memoria.redimensionar(128, 120);
        memoria.cargarPrograma(programaDe(8));
        assertEquals(8, memoria.getPosicionesUsadas());
    }

    @Test
    @DisplayName("El proceso de usuario no puede leer la zona de kernel")
    void protegeLaZonaDeKernel() {
        assertThrows(IllegalArgumentException.class, () -> memoria.leerComoUsuario(0));
        assertThrows(IllegalArgumentException.class, () -> memoria.leerComoUsuario(63));
        // Desde fuera del modo usuario si se puede, para poder mostrarla en pantalla.
        memoria.escribir(0, "7");
        assertEquals("7", memoria.leer(0));
    }

    @Test
    @DisplayName("Las direcciones fuera de la memoria se rechazan")
    void rechazaDireccionesInvalidas() {
        assertThrows(IndexOutOfBoundsException.class, () -> memoria.leer(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> memoria.leer(256));
        assertThrows(IndexOutOfBoundsException.class, () -> memoria.leer(1000));
    }

    @Test
    @DisplayName("Limpiar la zona de usuario no toca la del kernel")
    void limpiarRespetaElKernel() throws Exception {
        memoria.escribirEntero(10, 42);
        memoria.cargarPrograma(programaDeEjemplo());
        memoria.limpiarZonaUsuario();

        assertEquals(0, memoria.getPosicionesUsadas());
        assertTrue(memoria.estaLibre(64));
        assertEquals("42", memoria.leer(10), "El kernel conserva lo que tenia");
    }

    @Test
    @DisplayName("Los numeros se guardan como texto y se leen como int")
    void numerosComoTexto() {
        memoria.escribirEntero(100, -15);
        assertEquals("-15", memoria.leer(100));
        assertEquals(-15, memoria.leerEntero(100));
        assertEquals(0, memoria.leerEntero(101), "Una celda vacia vale cero");
    }

    @Test
    @DisplayName("Leer como numero una celda con una instruccion se informa")
    void leerEnteroDeUnaInstruccion() throws Exception {
        memoria.cargarPrograma(programaDeEjemplo());
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> memoria.leerEntero(64));
        assertTrue(e.getMessage().contains("MOV AX, 5"), e.getMessage());
    }

    @Test
    @DisplayName("El porcentaje de uso se calcula sobre la zona de usuario")
    void porcentajeDeUso() throws Exception {
        assertEquals(0, memoria.getPorcentajeUso());
        memoria.cargarPrograma(programaDe(96));
        assertEquals(50, memoria.getPorcentajeUso(), "96 de 192 posiciones es la mitad");
    }

    @Test
    @DisplayName("La configuracion de memoria rechaza valores incoherentes")
    void rechazaConfiguracionInvalida() {
        assertThrows(IllegalArgumentException.class, () -> memoria.redimensionar(64, 16),
                "El tamano minimo es 128");
        assertThrows(IllegalArgumentException.class, () -> memoria.redimensionar(256, 8),
                "El kernel minimo es 16");
        assertThrows(IllegalArgumentException.class, () -> memoria.redimensionar(256, 256),
                "El kernel no puede ocupar toda la memoria");
        assertThrows(IllegalArgumentException.class, () -> memoria.redimensionar(128, 200),
                "El kernel no puede exceder el tamano");
    }

    @Test
    @DisplayName("Redimensionar reconstruye las zonas")
    void redimensionarReconstruyeZonas() throws Exception {
        memoria.cargarPrograma(programaDeEjemplo());
        memoria.redimensionar(512, 128);

        assertEquals(512, memoria.getTamano());
        assertEquals(128, memoria.getLimiteKernel());
        assertEquals(384, memoria.getEspacioUsuario());
        assertEquals(0, memoria.getPosicionesUsadas());
        assertTrue(memoria.esDireccionKernel(127));
        assertFalse(memoria.esDireccionKernel(128));
    }
}
