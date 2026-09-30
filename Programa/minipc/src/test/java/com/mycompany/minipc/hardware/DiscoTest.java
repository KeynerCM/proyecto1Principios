package com.mycompany.minipc.hardware;

import com.mycompany.minipc.excepciones.DiscoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del disco: zonas, indice en las primeras posiciones y guardado
 * contiguo de programas.
 */
class DiscoTest {

    private Disco disco;

    @BeforeEach
    void preparar() {
        disco = new Disco();
    }

    private static List<String> programaDe(int lineas) {
        List<String> programa = new ArrayList<>();
        for (int i = 0; i < lineas; i++) {
            programa.add("MOV AX, " + i);
        }
        return programa;
    }

    @Test
    @DisplayName("La configuracion por defecto es la del enunciado")
    void configuracionPorDefecto() {
        assertEquals(512, disco.getTamano());
        assertEquals(64, disco.getTamanoMemoriaVirtual());
        assertEquals(20, disco.getInicioArchivos());
        assertEquals(448, disco.getInicioMemoriaVirtual());
        assertEquals(428, disco.getEspacioArchivos());
    }

    @Test
    @DisplayName("Las zonas de indice y memoria virtual se reconocen por direccion")
    void zonas() {
        assertTrue(disco.esDireccionIndice(0));
        assertTrue(disco.esDireccionIndice(19));
        assertFalse(disco.esDireccionIndice(20));
        assertFalse(disco.esDireccionMemoriaVirtual(447));
        assertTrue(disco.esDireccionMemoriaVirtual(448));
        assertTrue(disco.esDireccionMemoriaVirtual(511));
        assertFalse(disco.esDireccionMemoriaVirtual(512));
    }

    @Test
    @DisplayName("El programa se guarda al inicio del area de archivos y queda en el indice")
    void guardaYRegistraEnElIndice() throws DiscoException {
        EntradaIndice entrada = disco.guardarPrograma("file.asm", programaDe(7));

        assertEquals("file.asm", entrada.getNombre());
        assertEquals(20, entrada.getDireccionInicio());
        assertEquals(7, entrada.getTamano());
        assertEquals(26, entrada.getDireccionFin());

        assertEquals("file.asm|20|7", disco.leer(0), "El indice guarda la entrada como texto");
        assertEquals(entrada, EntradaIndice.desdeTexto(disco.leer(0)));
        assertEquals("MOV AX, 0", disco.leer(20));
        assertEquals("MOV AX, 6", disco.leer(26));
        assertTrue(disco.estaLibre(27));
    }

    @Test
    @DisplayName("Los archivos siguientes se guardan a continuacion")
    void guardaVariosArchivosContiguos() throws DiscoException {
        disco.guardarPrograma("a.asm", programaDe(5));
        EntradaIndice b = disco.guardarPrograma("b.asm", programaDe(3));

        assertEquals(25, b.getDireccionInicio());
        assertEquals(b, EntradaIndice.desdeTexto(disco.leer(1)));
        assertEquals(2, disco.getIndice().size());
        assertEquals(428 - 8, disco.getPosicionesLibres());
    }

    @Test
    @DisplayName("Leer un programa devuelve sus instrucciones en orden")
    void leeElPrograma() throws DiscoException {
        List<String> original = programaDe(4);
        disco.guardarPrograma("prog.asm", original);

        assertEquals(original, disco.leerPrograma("prog.asm"));
        assertEquals(original, disco.leerPrograma("PROG.ASM"), "El nombre ignora mayusculas");
    }

    @Test
    @DisplayName("No se admiten dos archivos con el mismo nombre")
    void rechazaNombreDuplicado() throws DiscoException {
        disco.guardarPrograma("file.asm", programaDe(2));

        DiscoException e = assertThrows(DiscoException.class,
                () -> disco.guardarPrograma("FILE.asm", programaDe(3)));
        assertTrue(e.getMessage().contains("Ya existe"), e.getMessage());
        assertEquals(1, disco.getIndice().size());
    }

    @Test
    @DisplayName("Un programa que no cabe no modifica el disco")
    void rechazaProgramaQueNoCabe() {
        DiscoException e = assertThrows(DiscoException.class,
                () -> disco.guardarPrograma("enorme.asm", programaDe(429)));
        assertTrue(e.getMessage().contains("429"), e.getMessage());
        assertTrue(e.getMessage().contains("428"), e.getMessage());
        assertTrue(disco.getIndice().isEmpty());
        assertTrue(disco.estaLibre(20));
    }

    @Test
    @DisplayName("El indice admite como maximo 20 archivos")
    void rechazaIndiceLleno() throws DiscoException {
        for (int i = 0; i < Disco.ENTRADAS_INDICE; i++) {
            disco.guardarPrograma("p" + i + ".asm", programaDe(1));
        }
        DiscoException e = assertThrows(DiscoException.class,
                () -> disco.guardarPrograma("extra.asm", programaDe(1)));
        assertTrue(e.getMessage().contains("indice"), e.getMessage());
    }

    @Test
    @DisplayName("Eliminar libera el espacio y la celda del indice para reutilizarlos")
    void eliminarLiberaEspacio() throws DiscoException {
        disco.guardarPrograma("a.asm", programaDe(5));
        disco.guardarPrograma("b.asm", programaDe(3));

        disco.eliminar("a.asm");

        assertNull(disco.buscar("a.asm"));
        assertTrue(disco.estaLibre(0));
        assertTrue(disco.estaLibre(20));

        // Un archivo que cabe en el hueco lo reutiliza (primer ajuste), y
        // tambien reutiliza la primera celda libre del indice.
        EntradaIndice c = disco.guardarPrograma("c.asm", programaDe(4));
        assertEquals(20, c.getDireccionInicio());
        assertEquals(c, EntradaIndice.desdeTexto(disco.leer(0)));
    }

    @Test
    @DisplayName("Si el hueco no alcanza, el archivo va al siguiente bloque libre")
    void primerAjusteSaltaHuecosChicos() throws DiscoException {
        disco.guardarPrograma("a.asm", programaDe(2));
        disco.guardarPrograma("b.asm", programaDe(3));
        disco.eliminar("a.asm");

        EntradaIndice c = disco.guardarPrograma("c.asm", programaDe(4));
        assertEquals(25, c.getDireccionInicio());
    }

    @Test
    @DisplayName("Operar sobre un archivo inexistente se informa")
    void archivoInexistente() {
        assertThrows(DiscoException.class, () -> disco.leerPrograma("nada.asm"));
        assertThrows(DiscoException.class, () -> disco.eliminar("nada.asm"));
    }

    @Test
    @DisplayName("Los archivos no invaden la memoria virtual")
    void respetaLaMemoriaVirtual() throws DiscoException {
        Disco chico = new Disco(64, 20);
        assertEquals(24, chico.getEspacioArchivos());
        chico.guardarPrograma("justo.asm", programaDe(24));
        assertTrue(chico.estaLibre(44), "La posicion 44 es la primera de memoria virtual");
        assertThrows(DiscoException.class, () -> chico.guardarPrograma("otro.asm", programaDe(1)));
    }

    @Test
    @DisplayName("Formatear deja el disco vacio")
    void formatear() throws DiscoException {
        disco.guardarPrograma("a.asm", programaDe(5));
        disco.formatear();
        assertTrue(disco.getIndice().isEmpty());
        assertEquals(428, disco.getPosicionesLibres());
    }

    @Test
    @DisplayName("Las configuraciones invalidas se rechazan con todos los motivos")
    void validaLaConfiguracion() {
        assertTrue(Disco.validar(512, 64).isEmpty());
        assertEquals(1, Disco.validar(32, 0).size());
        assertEquals(1, Disco.validar(512, -1).size());
        assertEquals(1, Disco.validar(100, 80).size(), "No deja espacio para archivos");
        assertThrows(IllegalArgumentException.class, () -> new Disco(100, 80));
    }

    @Test
    @DisplayName("Un nombre con el separador del indice se rechaza sin tocar el disco")
    void rechazaNombreConSeparador() {
        assertThrows(IllegalArgumentException.class,
                () -> disco.guardarPrograma("a|b.asm", programaDe(2)));
        assertTrue(disco.getIndice().isEmpty());
        assertTrue(disco.estaLibre(20), "La operacion debe ser atomica");
    }

    @Test
    @DisplayName("La entrada del indice se lee y se escribe como texto")
    void entradaComoTexto() {
        EntradaIndice entrada = EntradaIndice.desdeTexto("datos (2).asm|40|3");
        assertEquals("datos (2).asm", entrada.getNombre());
        assertEquals(40, entrada.getDireccionInicio());
        assertEquals(42, entrada.getDireccionFin());
        assertEquals("datos (2).asm|40|3", entrada.aTexto());
        assertNull(EntradaIndice.desdeTexto(""), "Una celda vacia no es una entrada");
        assertThrows(IllegalArgumentException.class, () -> EntradaIndice.desdeTexto("MOV AX, 5"));
    }

    @Test
    @DisplayName("Acceder fuera del disco lanza una excepcion clara")
    void accesoFueraDeRango() {
        assertThrows(IndexOutOfBoundsException.class, () -> disco.leer(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> disco.leer(512));
    }
}
