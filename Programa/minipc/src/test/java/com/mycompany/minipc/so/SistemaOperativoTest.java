package com.mycompany.minipc.so;

import com.mycompany.minipc.isa.RegistroID;
import com.mycompany.minipc.so.planificacion.FCFS;
import com.mycompany.minipc.so.planificacion.FabricaAlgoritmos;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;
import com.mycompany.minipc.so.trabajos.ListaTrabajos;
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
 * Pruebas del sistema operativo completo: lista de trabajos, planificador de
 * trabajos, lista de procesos, planificador FCFS, despachador y cambio de
 * contexto, todo sobre la memoria de texto.
 */
class SistemaOperativoTest {

    private SistemaOperativo so;
    private List<String> bitacora;

    @BeforeEach
    void preparar() {
        so = crear(256);
    }

    private SistemaOperativo crear(int memoria) {
        SistemaOperativo nuevo = new SistemaOperativo(memoria, 512, 64, new FCFS());
        bitacora = new ArrayList<>();
        nuevo.setBitacora(bitacora::add);
        return nuevo;
    }

    /** Guarda el programa en el disco y lo agrega a la lista de trabajos. */
    private Trabajo cargar(String nombre, String... lineas) throws Exception {
        so.getDisco().guardarPrograma(nombre, List.of(lineas));
        return so.agregarTrabajo(nombre);
    }

    private static String[] programaDe(int lineas) {
        String[] programa = new String[lineas];
        for (int i = 0; i < lineas; i++) {
            programa[i] = "INC";
        }
        return programa;
    }

    private void ejecutarTodo() {
        for (int i = 0; i < 1000 && so.tick(); i++) {
            // cada tick es un segundo de CPU
        }
        assertFalse(so.hayPendientes(), "Todos los trabajos debieron finalizar");
    }

    @Test
    @DisplayName("Un trabajo cargado entra NUEVO; al admitirlo tiene BCP y programa en memoria")
    void admitirCreaElProceso() throws Exception {
        Trabajo t = cargar("file.asm", "MOV AX, 5", "INT 20H");
        assertEquals(EstadoProceso.NUEVO, t.getEstado());
        assertEquals(1, t.getPid());

        assertEquals(1, so.admitir());

        Proceso p = t.getProceso();
        assertNotNull(p);
        assertEquals(EstadoProceso.PREPARADO, t.getEstado(), "El estado se lee del BCP");
        assertEquals(TablaBCP.direccionBCP(0), p.getDireccionBCP());
        assertEquals(128, p.getBase());
        assertEquals("MOV AX, 5", so.getMemoria().leer(128));
        assertEquals("INT 20H", so.getMemoria().leer(129));
        assertEquals("PREPARADO", so.getMemoria().leer(p.getDireccionBCP() + 2));
    }

    @Test
    @DisplayName("El despachador carga el contexto del BCP en la CPU y lo guarda al terminar")
    void cambioDeContexto() throws Exception {
        Trabajo t = cargar("file.asm", "MOV AX, 5", "MOV BX, 3", "INT 20H");
        so.admitir();
        Proceso p = t.getProceso();

        so.tick();
        assertEquals(p, so.getEnEjecucion());
        assertEquals(EstadoProceso.EJECUCION, p.getEstado());
        assertEquals("CPU1", p.getCpu());
        assertEquals(128, so.getCpu().getBase(), "Registro base cargado por el despachador");
        assertEquals(5, p.getRegistro(RegistroID.AX), "El BCP en memoria esta al dia");
        assertEquals(129, p.getPc());
        assertEquals("MOV AX, 5", p.getIr());
        assertEquals(1, p.getTiempoEmpleado());
        assertTrue(bitacora.stream().anyMatch(m -> m.contains("Despachador: P1 pasa a EJECUCION")));

        so.tick();
        so.tick();
        so.tick();
        assertNull(so.getEnEjecucion(), "Al terminar la CPU queda libre");
        assertFalse(so.getCpu().tieneContexto());
        assertEquals(EstadoProceso.FINALIZADO, t.getEstado());
        assertEquals(0, so.getTablaBCP().getProcesosAdmitidos(), "El BCP se libero");
        assertTrue(so.getMemoria().estaLibre(128), "La memoria del programa se libero");
        assertEquals(4, t.getTiempoCpu(), "Pesos 1 + 1 + 2");
    }

    @Test
    @DisplayName("FCFS: el primero en llegar se ejecuta completo antes que el segundo")
    void fcfs() throws Exception {
        Trabajo a = cargar("a.asm", "INC", "INC", "INT 20H");
        Trabajo b = cargar("b.asm", "INC", "INT 20H");
        so.admitir();

        so.tick();
        assertEquals(a.getProceso(), so.getEnEjecucion());
        assertEquals(EstadoProceso.PREPARADO, b.getEstado());

        ejecutarTodo();
        assertEquals(0, a.getInicio());
        assertEquals(4, a.getFin(), "Pesos 1 + 1 + 2");
        assertEquals(7, b.getFin(), "B espero a que A terminara: 4 + 1 + 2");
        assertEquals(7, so.getReloj());
    }

    @Test
    @DisplayName("Como maximo 5 procesos admitidos; el sexto espera en la lista de trabajos")
    void gradoDeMultiprogramacion() throws Exception {
        List<Trabajo> trabajos = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            trabajos.add(cargar("p" + i + ".asm", "INC", "INT 20H"));
        }
        assertEquals(5, so.admitir());
        assertEquals(5, so.getTablaBCP().getProcesosAdmitidos());
        assertEquals(EstadoProceso.NUEVO, trabajos.get(5).getEstado());

        so.tick();
        so.tick();
        so.tick();
        assertEquals(EstadoProceso.FINALIZADO, trabajos.get(0).getEstado());
        assertEquals(EstadoProceso.PREPARADO, trabajos.get(5).getEstado(),
                "Al liberarse un BCP se admite el siguiente");
        assertEquals(EstadoProceso.NUEVO, trabajos.get(6).getEstado());

        ejecutarTodo();
    }

    @Test
    @DisplayName("Si no cabe en la memoria principal, el trabajo nuevo pasa a la memoria virtual")
    void nuevoAMemoriaVirtual() throws Exception {
        so = crear(160);
        Trabajo a = cargar("a.asm", programaDe(20));
        Trabajo b = cargar("b.asm", programaDe(20));

        assertEquals(2, so.admitir(), "La zona de usuario de 32 solo alcanza para uno");
        assertEquals(EstadoProceso.PREPARADO, a.getEstado());
        assertEquals(EstadoProceso.SUSPENDIDO_PREPARADO, b.getEstado());
        int inicioVirtual = so.getDisco().getInicioMemoriaVirtual();
        assertEquals(inicioVirtual, b.getProceso().getBase(), "La BASE apunta al disco");
        assertEquals(inicioVirtual, b.getProceso().getPc());
        assertEquals("INC", so.getDisco().leer(inicioVirtual));
        assertTrue(bitacora.stream().anyMatch(m -> m.contains("pasa a la memoria virtual")));

        ejecutarTodo();
        assertEquals(EstadoProceso.FINALIZADO, a.getEstado());
        assertEquals(EstadoProceso.FINALIZADO, b.getEstado());
        assertEquals(0, b.getInicio(), "El inicio es cuando se admitio, aunque fuera al disco");
        assertTrue(bitacora.stream().anyMatch(m -> m.contains("P2 vuelve del disco")));
        assertTrue(so.getDisco().estaLibre(inicioVirtual), "La memoria virtual queda libre");
        assertEquals(0, so.getMemoria().getPosicionesUsadas(), "Al final no queda nada en memoria");
        assertEquals(20, so.getOcupacionMaxima(), "Pero el maximo fue el programa de A");
        assertEquals(20, so.getMemoriaVirtualMaxima(), "B ocupo 20 en la memoria virtual");
    }

    @Test
    @DisplayName("Si no cabe en ninguna memoria, el trabajo espera en la lista de trabajos")
    void esperaPorMemoria() throws Exception {
        so = new SistemaOperativo(160, 512, 16, new FCFS());
        so.setBitacora(bitacora::add);
        Trabajo a = cargar("a.asm", programaDe(20));
        Trabajo b = cargar("b.asm", programaDe(20));

        assertEquals(1, so.admitir(), "La memoria virtual de 16 tampoco alcanza");
        assertEquals(EstadoProceso.NUEVO, b.getEstado());
        assertTrue(b.isEsperandoMemoria());
        assertTrue(bitacora.stream().anyMatch(m -> m.contains("espera en la lista de trabajos")));

        ejecutarTodo();
        assertEquals(EstadoProceso.FINALIZADO, a.getEstado());
        assertEquals(EstadoProceso.FINALIZADO, b.getEstado());
        assertTrue(b.getInicio() >= a.getFin());
    }

    @Test
    @DisplayName("Mientras A espera el teclado no se suspende; B sigue en la memoria virtual")
    void esperaNoSeSuspende() throws Exception {
        so = crear(160);
        String[] lineasA = programaDe(20);
        lineasA[0] = "INT 09H";
        lineasA[1] = "INT 10H";
        lineasA[19] = "INT 20H";
        Trabajo a = cargar("a.asm", lineasA);
        Trabajo b = cargar("b.asm", programaDe(20));
        so.admitir();

        so.tick();
        so.tick();
        assertEquals(EstadoProceso.EN_ESPERA, a.getEstado());
        assertEquals(128, a.getProceso().getBase(), "A se queda en memoria: la CPU lo espera");
        assertEquals(EstadoProceso.SUSPENDIDO_PREPARADO, b.getEstado());

        so.entradaTeclado("7");
        assertEquals(EstadoProceso.PREPARADO, a.getEstado());
        assertEquals(7, a.getProceso().getRegistro(RegistroID.DX));

        ejecutarTodo();
        assertEquals(EstadoProceso.FINALIZADO, b.getEstado());
        assertTrue(b.getFin() > a.getFin(), "B entra a memoria cuando A termina");
        assertEquals(List.of("[P1] >> Ingresar valor: 7", "[P1] 7"), so.getPantalla().getLineas());
    }

    @Test
    @DisplayName("Reiniciar vacia la memoria virtual")
    void reiniciarLimpiaMemoriaVirtual() throws Exception {
        so = crear(160);
        cargar("a.asm", programaDe(20));
        cargar("b.asm", programaDe(20));
        so.admitir();
        int inicioVirtual = so.getDisco().getInicioMemoriaVirtual();
        assertFalse(so.getDisco().estaLibre(inicioVirtual));

        so.reiniciar();
        assertTrue(so.getDisco().estaLibre(inicioVirtual));
        assertEquals(0, so.getOcupacionMaxima(), "Reiniciar empieza a medir de nuevo");
        assertEquals(0, so.getMemoriaVirtualMaxima());
    }

    @Test
    @DisplayName("Un error de ejecucion finaliza ese proceso y los demas siguen")
    void errorDeEjecucion() throws Exception {
        Trabajo malo = cargar("malo.asm", "POP AX", "INT 20H");
        Trabajo bueno = cargar("bueno.asm", "MOV AX, 1", "INT 20H");
        so.admitir();

        so.tick();
        List<String> errores = so.tomarErrores();
        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("Pila vacia"), errores.get(0));
        assertEquals(EstadoProceso.FINALIZADO, malo.getEstado());
        assertNotNull(malo.getError());
        assertTrue(so.tomarErrores().isEmpty(), "Cada error se entrega una sola vez");

        ejecutarTodo();
        assertNull(bueno.getError());
    }

    @Test
    @DisplayName("Cada proceso sigue con sus registros: el contexto no se mezcla")
    void contextosSeparados() throws Exception {
        cargar("a.asm", "MOV AX, 7", "INT 20H");
        Trabajo b = cargar("b.asm", "INC AX", "INC AX", "INT 20H");
        so.admitir();

        for (int i = 0; i < 4; i++) {
            so.tick();
        }
        assertEquals(b.getProceso(), so.getEnEjecucion());
        assertEquals(1, b.getProceso().getRegistro(RegistroID.AX),
                "B empieza con sus propios registros en cero, no con el AX = 7 de A");
    }

    @Test
    @DisplayName("Sin trabajos, tick no avanza el reloj")
    void sinTrabajos() {
        assertFalse(so.tick());
        assertEquals(0, so.getReloj());
    }

    @Test
    @DisplayName("Reiniciar devuelve los trabajos a NUEVO y el reloj a cero")
    void reiniciar() throws Exception {
        Trabajo t = cargar("file.asm", "INC", "INT 20H");
        so.admitir();
        ejecutarTodo();

        so.reiniciar();
        assertEquals(EstadoProceso.NUEVO, t.getEstado());
        assertEquals(0, so.getReloj());
        assertEquals(1, so.admitir());
        ejecutarTodo();
    }

    @Test
    @DisplayName("La lista de trabajos admite 20; un programa enorme se rechaza")
    void limitesDeLaListaDeTrabajos() throws Exception {
        so.getDisco().guardarPrograma("p.asm", List.of("INC"));
        for (int i = 0; i < ListaTrabajos.CAPACIDAD; i++) {
            so.agregarTrabajo("p.asm");
        }
        assertThrows(IllegalStateException.class, () -> so.agregarTrabajo("p.asm"));
        assertThrows(IllegalArgumentException.class, () -> so.agregarTrabajo("no.asm"));

        so.limpiar();
        so.getDisco().guardarPrograma("enorme.asm", List.of(programaDe(129)));
        assertThrows(IllegalArgumentException.class, () -> so.agregarTrabajo("enorme.asm"));
    }

    @Test
    @DisplayName("La fabrica solo conoce FCFS en este proyecto")
    void fabricaDeAlgoritmos() {
        assertEquals("FCFS", FabricaAlgoritmos.crear("fcfs").getNombre());
        assertThrows(IllegalArgumentException.class, () -> FabricaAlgoritmos.crear("RR"));
        assertEquals(List.of("FCFS"), FabricaAlgoritmos.disponibles());
    }

    @Test
    @DisplayName("El reloj simulado se muestra como hora:minuto:segundo")
    void formatoDelReloj() {
        assertEquals("00:00:00", SistemaOperativo.formatearReloj(0));
        assertEquals("00:01:05", SistemaOperativo.formatearReloj(65));
        assertEquals("01:00:01", SistemaOperativo.formatearReloj(3601));
    }
}
