package com.mycompany.minipc.so.interrupciones;

import com.mycompany.minipc.isa.RegistroID;
import com.mycompany.minipc.so.SistemaOperativo;
import com.mycompany.minipc.so.planificacion.FCFS;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.trabajos.Trabajo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de las llamadas al sistema (INT 20H, 10H, 09H y 21H) y de la
 * interrupcion de entrada y salida del teclado, sobre el sistema operativo
 * completo.
 */
class InterrupcionesTest {

    private SistemaOperativo so;
    private List<String> bitacora;

    @BeforeEach
    void preparar() {
        so = new SistemaOperativo(256, 512, 64, new FCFS());
        bitacora = new ArrayList<>();
        so.setBitacora(bitacora::add);
    }

    private Trabajo cargar(String nombre, String... lineas) throws Exception {
        so.getDisco().guardarPrograma(nombre, List.of(lineas));
        Trabajo trabajo = so.agregarTrabajo(nombre);
        so.admitir();
        return trabajo;
    }

    private void ticks(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            so.tick();
        }
    }

    private void ejecutarTodo() {
        for (int i = 0; i < 1000 && so.tick(); i++) {
            // cada tick es un segundo de CPU
        }
    }

    private boolean bitacoraContiene(String texto) {
        return bitacora.stream().anyMatch(m -> m.contains(texto));
    }

    @Test
    @DisplayName("INT 10H imprime DX en la pantalla despues de sus 2 segundos")
    void int10ImprimeDx() throws Exception {
        cargar("pantalla.asm", "MOV DX, 12", "INT 10H", "INT 20H");

        ticks(2);
        assertTrue(so.getPantalla().getLineas().isEmpty(), "INT 10H pesa 2");
        ticks(1);
        assertEquals(List.of("[P1] 12"), so.getPantalla().getLineas());
        assertTrue(bitacoraContiene("Llamada al sistema INT 10H: P1 imprime DX = 12"));
    }

    @Test
    @DisplayName("INT 20H termina el proceso despues de sus 2 segundos")
    void int20Termina() throws Exception {
        Trabajo t = cargar("fin.asm", "INT 20H", "MOV AX, 1");
        ticks(1);
        assertEquals(EstadoProceso.EJECUCION, t.getEstado());
        ticks(1);
        assertEquals(EstadoProceso.FINALIZADO, t.getEstado());
        assertEquals(2, t.getTiempoCpu());
    }

    @Test
    @DisplayName("INT 09H deja al proceso EN_ESPERA y el ENTER lo devuelve con el valor en DX")
    void int09BloqueaHastaElEnter() throws Exception {
        Trabajo t = cargar("teclado.asm", "INT 09H", "MOV AX, DX", "INT 20H");

        ticks(1);
        assertEquals(EstadoProceso.EN_ESPERA, t.getEstado());
        assertNull(so.getEnEjecucion(), "La CPU queda libre para otro proceso");
        assertTrue(so.hayEsperaTeclado());
        assertEquals(List.of("[P1] " + ManejadorInterrupciones.AVISO_TECLADO),
                so.getPantalla().getLineas());

        so.entradaTeclado("12");
        assertEquals(List.of("[P1] >> Ingresar valor: 12"), so.getPantalla().getLineas(),
                "El eco queda en la misma linea del aviso");
        assertEquals(EstadoProceso.PREPARADO, t.getEstado());
        assertEquals(12, t.getProceso().getRegistro(RegistroID.DX), "El valor va al DX del BCP");
        assertFalse(so.hayEsperaTeclado());
        assertTrue(bitacoraContiene("Interrupcion de E/S (teclado): P1 recibe 12 en DX"));

        ticks(1);
        assertEquals(12, t.getProceso().getRegistro(RegistroID.AX));
        ejecutarTodo();
        assertEquals(EstadoProceso.FINALIZADO, t.getEstado());
    }

    @Test
    @DisplayName("Mientras uno espera el teclado, la CPU lo espera y no pasa al siguiente")
    void cpuEsperaAlProceso() throws Exception {
        Trabajo espera = cargar("teclado.asm", "INT 09H", "INT 20H");
        Trabajo otro = cargar("otro.asm", "MOV AX, 1", "INT 20H");

        ticks(1);
        assertEquals(EstadoProceso.EN_ESPERA, espera.getEstado());
        int reloj = so.getReloj();
        ticks(3);
        assertNull(so.getEnEjecucion(), "La CPU no pasa al otro proceso");
        assertEquals(EstadoProceso.PREPARADO, otro.getEstado());
        assertEquals(0, otro.getProceso().getTiempoEmpleado(), "El otro no ha ejecutado nada");
        assertEquals(reloj + 3, so.getReloj(), "El tiempo de espera cuenta en el reloj");

        so.entradaTeclado("4");
        ticks(1);
        assertEquals(espera.getProceso(), so.getEnEjecucion(),
                "Vuelve el que esperaba, no el otro");
        ejecutarTodo();
        assertEquals(EstadoProceso.FINALIZADO, otro.getEstado());
        assertTrue(otro.getFin() > espera.getFin(), "El otro corre cuando el primero termina");
    }

    @Test
    @DisplayName("Si todos esperan el teclado, la CPU queda ociosa pero el reloj avanza")
    void cpuOciosa() throws Exception {
        cargar("teclado.asm", "INT 09H", "INT 20H");
        ticks(1);
        int reloj = so.getReloj();

        assertTrue(so.tick(), "Sigue habiendo un trabajo pendiente");
        so.tick();
        assertEquals(reloj + 2, so.getReloj());
        assertEquals(1, bitacora.stream().filter(m -> m.contains("CPU ociosa")).count(),
                "El aviso se escribe una sola vez");
    }

    @Test
    @DisplayName("El teclado solo acepta numeros de 0 a 255 y solo si alguien espera")
    void validacionDelTeclado() throws Exception {
        assertThrows(IllegalStateException.class, () -> so.entradaTeclado("5"));

        cargar("teclado.asm", "INT 09H", "INT 20H");
        ticks(1);
        assertThrows(IllegalArgumentException.class, () -> so.entradaTeclado("256"));
        assertThrows(IllegalArgumentException.class, () -> so.entradaTeclado("-1"));
        assertThrows(IllegalArgumentException.class, () -> so.entradaTeclado("abc"));
        assertThrows(IllegalArgumentException.class, () -> so.entradaTeclado(""));
        assertTrue(so.hayEsperaTeclado(), "Un valor invalido no despierta al proceso");
        assertNotNull(so.entradaTeclado(" 255 "));
    }

    @Test
    @DisplayName("INT 21H crea, abre, escribe y lee un archivo del disco")
    void int21Archivos() throws Exception {
        Trabajo t = cargar("archivos.asm",
                "MOV DX, \"datos.txt\"",
                "MOV AH, 3Ch",
                "INT 21H",
                "MOV AH, 3Dh",
                "INT 21H",
                "MOV AL, 65",
                "MOV AH, 40h",
                "INT 21H",
                "MOV AL, 66",
                "INT 21H",
                "MOV AL, 0",
                "MOV AH, 4Dh",
                "INT 21H",
                "MOV CX, AL",
                "INT 21H",
                "MOV BX, AL",
                "INT 20H");

        ticks(1 + 1 + 5);
        assertNotNull(so.getDisco().buscar("datos.txt"), "3Ch crea el archivo en el disco");
        ticks(1 + 5);
        assertEquals(List.of("datos.txt:0"), t.getProceso().getArchivosAbiertos(),
                "3Dh lo agrega a los archivos abiertos del BCP");
        ticks(1 + 1 + 5 + 1 + 5);
        assertEquals(List.of(65, 66), so.getDisco().leerDatos("datos.txt"));
        ticks(1 + 1 + 5 + 1 + 5 + 1);
        assertEquals(65, t.getProceso().getRegistro(RegistroID.CX), "Primer byte leido");
        assertEquals(66, t.getProceso().getRegistro(RegistroID.BX), "Segundo byte leido");
        assertEquals(List.of("datos.txt:2"), t.getProceso().getArchivosAbiertos());
        assertTrue(bitacoraContiene("INT 21H: P1 escribe AL = 65 en datos.txt"));
        ejecutarTodo();
        assertNull(t.getError());
    }

    @Test
    @DisplayName("INT 21H con AH = 41h elimina el archivo")
    void int21Elimina() throws Exception {
        so.getDisco().crearArchivo("viejo.txt");
        Trabajo t = cargar("borrar.asm", "MOV DX, \"viejo.txt\"", "MOV AH, 41h", "INT 21H",
                "INT 20H");
        ejecutarTodo();
        assertNull(t.getError());
        assertNull(so.getDisco().buscar("viejo.txt"));
    }

    @Test
    @DisplayName("Leer un archivo sin abrirlo es un error del proceso")
    void int21SinAbrir() throws Exception {
        so.getDisco().crearArchivo("datos.txt");
        Trabajo t = cargar("mal.asm", "MOV DX, \"datos.txt\"", "MOV AH, 4Dh", "INT 21H",
                "INT 20H");
        ejecutarTodo();
        assertEquals(EstadoProceso.FINALIZADO, t.getEstado());
        assertTrue(t.getError().contains("no esta abierto"), t.getError());
    }

    @Test
    @DisplayName("Abrir un archivo que no existe es un error del proceso")
    void int21NoExiste() throws Exception {
        Trabajo t = cargar("mal.asm", "MOV DX, \"nada.txt\"", "MOV AH, 3Dh", "INT 21H",
                "INT 20H");
        ejecutarTodo();
        assertTrue(t.getError().contains("No existe el archivo"), t.getError());
    }

    @Test
    @DisplayName("DX fuera del programa es una violacion de proteccion")
    void int21DxInvalido() throws Exception {
        Trabajo t = cargar("mal.asm", "MOV DX, 5", "MOV AH, 3Ch", "INT 21H", "INT 20H");
        ejecutarTodo();
        assertTrue(t.getError().startsWith("Violacion de proteccion"), t.getError());
        assertTrue(t.getError().contains("sale del programa"), t.getError());
    }

    @Test
    @DisplayName("DX dentro del programa pero sin nombre entre comillas es un error")
    void int21DxSinNombre() throws Exception {
        Trabajo t = cargar("mal.asm", "MOV DX, 1", "MOV AH, 3Ch", "INT 21H", "INT 20H");
        ejecutarTodo();
        assertTrue(t.getError().contains("no contiene un nombre"), t.getError());
    }

    @Test
    @DisplayName("DX es relativo a la base: el mismo programa funciona en otra region")
    void int21DxRelativo() throws Exception {
        cargar("relleno.asm", "MOV AX, 1", "MOV AX, 2", "MOV AX, 3", "INT 20H");
        Trabajo t = cargar("crea.asm", "MOV DX, \"rel.txt\"", "MOV AH, 3Ch", "INT 21H",
                "INT 20H");
        ejecutarTodo();
        assertNull(t.getError());
        assertNotNull(so.getDisco().buscar("rel.txt"));
    }

    @Test
    @DisplayName("INT 21H no puede eliminar un programa del disco")
    void int21NoEliminaProgramas() throws Exception {
        Trabajo t = cargar("borra.asm", "MOV DX, \"borra.asm\"", "MOV AH, 41h", "INT 21H",
                "INT 20H");
        ejecutarTodo();
        assertTrue(t.getError().contains("es un programa"), t.getError());
        assertNotNull(so.getDisco().buscar("borra.asm"));
    }

    @Test
    @DisplayName("Un servicio de INT 21H que no existe se informa con los validos")
    void int21ServicioDesconocido() throws Exception {
        Trabajo t = cargar("mal.asm", "MOV DX, \"a.txt\"", "MOV AH, 99", "INT 21H",
                "INT 20H");
        ejecutarTodo();
        assertTrue(t.getError().contains("63h no es un servicio"), t.getError());
        assertTrue(t.getError().contains("3Ch crear"), t.getError());
    }
    @Test
    @DisplayName("Los procesos piden el teclado de uno en uno, en orden de llegada")
    void tecladoDeUnoEnUno() throws Exception {
        cargar("a.asm", "INT 09H", "INT 10H", "INT 20H");
        cargar("b.asm", "INT 09H", "INT 10H", "INT 20H");
        ticks(2);
        assertEquals(List.of("[P1] >> Ingresar valor:"), so.getPantalla().getLineas(),
                "P2 no empieza mientras P1 espera");
        assertEquals("P1", so.getEsperandoTeclado().toString());

        so.entradaTeclado("5");
        for (int i = 0; i < 100 && !so.hayEsperaTeclado(); i++) {
            so.tick();
        }
        assertEquals("P2", so.getEsperandoTeclado().toString());
        so.entradaTeclado("9");
        ejecutarTodo();
        assertEquals(List.of("[P1] >> Ingresar valor: 5", "[P1] 5",
                "[P2] >> Ingresar valor: 9", "[P2] 9"), so.getPantalla().getLineas());
    }
}
