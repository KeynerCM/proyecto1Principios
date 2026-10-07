package com.mycompany.minipc.so.procesos;

import com.mycompany.minipc.hardware.Memoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la lista de procesos: una lista enlazada que existe solo en
 * memoria, con la cabecera apuntando al primer BCP y cada BCP al siguiente.
 */
class ListaProcesosTest {

    private Memoria memoria;
    private TablaBCP tabla;
    private ListaProcesos lista;
    private Proceso p1;
    private Proceso p2;
    private Proceso p3;

    @BeforeEach
    void preparar() {
        memoria = new Memoria(256, TablaBCP.calcularKernel(256,
                TablaBCP.PORCENTAJE_KERNEL_POR_DEFECTO));
        tabla = new TablaBCP(memoria);
        lista = new ListaProcesos(tabla);
        p1 = tabla.crear(1, "a.asm", 130, 2, 0);
        p2 = tabla.crear(2, "b.asm", 132, 2, 0);
        p3 = tabla.crear(3, "c.asm", 134, 2, 0);
        lista.agregarAlFinal(p1);
        lista.agregarAlFinal(p2);
        lista.agregarAlFinal(p3);
    }

    private String siguienteDe(Proceso p) {
        return memoria.leer(TablaBCP.direccionCampo(p.getDireccionBCP(), CampoBCP.SIGUIENTE));
    }

    @Test
    @DisplayName("Los enlaces son direcciones de memoria")
    void enlacesEnMemoria() {
        assertEquals("3", memoria.leer(CampoCabecera.INICIO_LISTA.getDireccion()),
                "La cabecera apunta al BCP de P1");
        assertEquals("28", siguienteDe(p1), "P1 apunta al BCP de P2");
        assertEquals("53", siguienteDe(p2), "P2 apunta al BCP de P3");
        assertEquals("", siguienteDe(p3), "P3 es el ultimo");
        assertEquals(List.of(p1, p2, p3), lista.recorrer());
    }

    @Test
    @DisplayName("Quitar del medio une al anterior con el siguiente")
    void quitarDelMedio() {
        lista.quitar(p2);
        assertEquals("53", siguienteDe(p1));
        assertEquals(List.of(p1, p3), lista.recorrer());
    }

    @Test
    @DisplayName("Quitar el primero mueve la cabecera")
    void quitarElPrimero() {
        lista.quitar(p1);
        assertEquals("28", memoria.leer(CampoCabecera.INICIO_LISTA.getDireccion()));
        assertEquals(List.of(p2, p3), lista.recorrer());
    }

    @Test
    @DisplayName("Mover al final deja al proceso al final de la cola")
    void moverAlFinal() {
        lista.moverAlFinal(p1);
        assertEquals(List.of(p2, p3, p1), lista.recorrer());
    }

    @Test
    @DisplayName("Filtrar por estado da la cola de listos en orden")
    void conEstado() {
        p2.setEstado(EstadoProceso.EJECUCION);
        assertEquals(List.of(p1, p3), lista.conEstado(EstadoProceso.PREPARADO));
    }

    @Test
    @DisplayName("Al quitar todos, la lista queda vacia")
    void vaciar() {
        lista.quitar(p1);
        lista.quitar(p2);
        lista.quitar(p3);
        assertTrue(lista.estaVacia());
        assertTrue(memoria.estaLibre(CampoCabecera.INICIO_LISTA.getDireccion()));
    }
}
