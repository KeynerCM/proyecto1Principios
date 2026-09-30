package com.mycompany.minipc.so.trabajos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mycompany.minipc.so.procesos.EstadoProceso;

/**
 * Nombre: ListaTrabajos
 * Entradas: no aplica
 * Salidas: no aplica
 * Restricciones: admite como maximo 20 trabajos, igual que las entradas del
 *                indice del disco
 * Descripcion: la "Lista de Trabajos" y "cola de trabajo" del enunciado: los
 *              programas cargados desde el disco, en orden de llegada, con su
 *              estado. Los NUEVO esperan a que el planificador de trabajos
 *              los admita como procesos.
 */
public class ListaTrabajos {

    /** Capacidad de la lista: "20 procesos" en la pizarra del profesor. */
    public static final int CAPACIDAD = 20;

    private final List<Trabajo> trabajos = new ArrayList<>();

    /**
     * Nombre: agregar
     * Entradas: trabajo, trabajo nuevo
     * Salidas: ninguna
     * Restricciones: lanza IllegalStateException si la lista esta llena
     * Descripcion: lo agrega al final, en estado NUEVO.
     */
    public void agregar(Trabajo trabajo) {
        if (estaLlena()) {
            throw new IllegalStateException("La lista de trabajos esta llena ("
                    + CAPACIDAD + " trabajos)");
        }
        trabajos.add(trabajo);
    }

    /**
     * Nombre: estaLlena
     * Entradas: ninguna
     * Salidas: true si ya hay 20 trabajos
     * Restricciones: ninguna
     * Descripcion: se consulta antes de cargar mas archivos.
     */
    public boolean estaLlena() {
        return trabajos.size() >= CAPACIDAD;
    }

    /**
     * Nombre: siguienteNuevo
     * Entradas: ninguna
     * Salidas: el primer trabajo en estado NUEVO, o nulo si no hay
     * Restricciones: ninguna
     * Descripcion: el planificador de trabajos admite en orden de llegada.
     */
    public Trabajo siguienteNuevo() {
        for (Trabajo trabajo : trabajos) {
            if (trabajo.getEstado() == EstadoProceso.NUEVO) {
                return trabajo;
            }
        }
        return null;
    }

    /**
     * Nombre: buscar
     * Entradas: pid, identificador del proceso
     * Salidas: el trabajo con ese PID, o nulo
     * Restricciones: ninguna
     * Descripcion: permite pasar de un proceso a su fila de la lista.
     */
    public Trabajo buscar(int pid) {
        for (Trabajo trabajo : trabajos) {
            if (trabajo.getPid() == pid) {
                return trabajo;
            }
        }
        return null;
    }

    /**
     * Nombre: hayPendientes
     * Entradas: ninguna
     * Salidas: true si algun trabajo no ha finalizado
     * Restricciones: ninguna
     * Descripcion: la ejecucion automatica sigue mientras haya pendientes.
     */
    public boolean hayPendientes() {
        for (Trabajo trabajo : trabajos) {
            if (!trabajo.getEstado().esFinal()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Nombre: getTrabajos
     * Entradas: ninguna
     * Salidas: los trabajos en orden de llegada
     * Restricciones: la lista no se puede modificar
     * Descripcion: la interfaz la muestra en la tabla de trabajos.
     */
    public List<Trabajo> getTrabajos() {
        return Collections.unmodifiableList(trabajos);
    }

    /**
     * Nombre: reiniciar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: vuelve todos los trabajos a NUEVO para ejecutarlos otra vez.
     */
    public void reiniciar() {
        for (Trabajo trabajo : trabajos) {
            trabajo.reiniciar();
        }
    }

    /**
     * Nombre: vaciar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: quita todos los trabajos.
     */
    public void vaciar() {
        trabajos.clear();
    }
}
