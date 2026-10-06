package com.mycompany.minipc.so.memoria;

import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.ListaProcesos;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del intercambio entre la memoria principal y la memoria virtual.
 * En el Proyecto 1 solo se trae a los suspendidos; suspender a un proceso en
 * espera queda listo para el Proyecto 2 y se prueba aqui directamente.
 */
class IntercambioTest {

    private Memoria memoria;
    private Disco disco;
    private TablaBCP tabla;
    private ListaProcesos procesos;
    private GestorMemoria gestor;
    private Intercambio intercambio;

    @BeforeEach
    void preparar() {
        memoria = new Memoria(160, TablaBCP.TAMANO_KERNEL);
        disco = new Disco(512, 64);
        tabla = new TablaBCP(memoria);
        procesos = new ListaProcesos(tabla);
        gestor = new GestorMemoria(memoria);
        intercambio = new Intercambio(procesos, gestor, new MemoriaVirtual(disco), m -> { });
    }

    private Proceso crear(int pid, int lineas) {
        String[] programa = new String[lineas];
        Arrays.fill(programa, "INC");
        int base = gestor.asignar(List.of(programa));
        Proceso proceso = tabla.crear(pid, "p" + pid + ".asm", base, lineas, 0);
        procesos.agregarAlFinal(proceso);
        return proceso;
    }

    @Test
    @DisplayName("Suspender y traer mueven la imagen y reubican BASE y PC")
    void suspenderYTraer() {
        Proceso p = crear(1, 10);
        p.setPc(p.getBase() + 3);
        p.setEstado(EstadoProceso.EN_ESPERA);

        assertTrue(intercambio.suspender(p));
        assertEquals(EstadoProceso.SUSPENDIDO_EN_ESPERA, p.getEstado());
        assertTrue(disco.esDireccionMemoriaVirtual(p.getBase()));
        assertEquals(p.getBase() + 3, p.getPc(), "El PC conserva su desplazamiento");
        assertEquals(0, memoria.getPosicionesUsadas(), "Libero la memoria principal");

        p.setEstado(EstadoProceso.SUSPENDIDO_PREPARADO);
        intercambio.equilibrar();
        assertEquals(EstadoProceso.PREPARADO, p.getEstado());
        assertEquals(memoria.getLimiteKernel(), p.getBase());
        assertEquals(p.getBase() + 3, p.getPc());
        assertTrue(disco.estaLibre(disco.getInicioMemoriaVirtual()), "Libero el disco");
    }

    @Test
    @DisplayName("equilibrar no suspende a un proceso EN_ESPERA en el Proyecto 1")
    void equilibrarNoSuspende() {
        Proceso p = crear(1, 30);
        p.setEstado(EstadoProceso.EN_ESPERA);
        intercambio.equilibrar();
        assertEquals(EstadoProceso.EN_ESPERA, p.getEstado());
        assertEquals(memoria.getLimiteKernel(), p.getBase());
    }
}
