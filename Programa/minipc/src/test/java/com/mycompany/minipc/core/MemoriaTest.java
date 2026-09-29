package com.mycompany.minipc.core;

import com.mycompany.minipc.excepciones.MemoriaInsuficienteException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.isa.Ensamblador;
import com.mycompany.minipc.isa.Instruccion;
import com.mycompany.minipc.isa.OpCode;
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

    private List<Instruccion> programaDeEjemplo() throws SintaxisException {
        return ensamblador.ensamblar(List.of(
                "MOV AX, 5",
                "MOV BX, 3",
                "LOAD AX",
                "ADD BX",
                "SUB AX",
                "STORE AX",
                "MOV BX, -8"));
    }

    private List<Instruccion> programaDe(int lineas) throws SintaxisException {
        List<String> texto = new ArrayList<>();
        for (int i = 0; i < lineas; i++) {
            texto.add("MOV AX, 1");
        }
        return ensamblador.ensamblar(texto);
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

        assertEquals(CeldaMemoria.Tipo.RESERVADA_KERNEL, memoria.leer(0).getTipo());
        assertEquals(CeldaMemoria.Tipo.RESERVADA_KERNEL, memoria.leer(63).getTipo());
        assertEquals(CeldaMemoria.Tipo.LIBRE, memoria.leer(64).getTipo());
    }

    @Test
    @DisplayName("El programa se carga al inicio de la zona de usuario")
    void cargaEnLaBaseDelUsuario() throws Exception {
        int base = memoria.cargarPrograma(programaDeEjemplo());

        assertEquals(64, base);
        assertEquals(CeldaMemoria.Tipo.INSTRUCCION, memoria.leer(64).getTipo());
        assertEquals("MOV AX, 5", memoria.leer(64).getEtiqueta());
        assertEquals(OpCode.MOV, memoria.leer(64).getInstruccion().getOpcode());
        assertEquals(5, memoria.leer(64).getInstruccion().getValor(1));
        assertEquals("MOV BX, -8", memoria.leer(70).getEtiqueta());
        assertTrue(memoria.leer(71).estaLibre(), "La celda siguiente debe quedar libre");
        assertEquals(7, memoria.getPosicionesUsadas());
    }

    @Test
    @DisplayName("Cada linea de programa ocupa exactamente una posicion")
    void unaLineaUnaPosicion() throws Exception {
        List<Instruccion> programa = programaDeEjemplo();
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
        assertEquals(CeldaMemoria.Tipo.RESERVADA_KERNEL, memoria.leer(0).getTipo());
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
        memoria.cargarPrograma(programaDeEjemplo());
        memoria.limpiarZonaUsuario();

        assertEquals(0, memoria.getPosicionesUsadas());
        assertTrue(memoria.leer(64).estaLibre());
        assertEquals(CeldaMemoria.Tipo.RESERVADA_KERNEL, memoria.leer(0).getTipo());
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
        assertEquals(CeldaMemoria.Tipo.RESERVADA_KERNEL, memoria.leer(127).getTipo());
        assertEquals(CeldaMemoria.Tipo.LIBRE, memoria.leer(128).getTipo());
    }
}
