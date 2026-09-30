package com.mycompany.minipc.so.planificacion;

import java.util.List;

import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: AlgoritmoPlanificacion
 * Entradas: no aplica, es una interfaz
 * Salidas: no aplica
 * Restricciones: una implementacion solo decide; no cambia estados ni toca la
 *                CPU, eso lo hace el despachador
 * Descripcion: el punto intercambiable del planificador de procesos (patron
 *              Estrategia). En este proyecto la unica implementacion es FCFS;
 *              en el proyecto 2 se agregan SPN, SRT, RR y HRRN como clases
 *              nuevas, sin cambiar el planificador ni el despachador.
 */
public interface AlgoritmoPlanificacion {

    /**
     * Nombre: getNombre
     * Entradas: ninguna
     * Salidas: el nombre corto del algoritmo, por ejemplo "FCFS"
     * Restricciones: ninguna
     * Descripcion: se muestra en la interfaz y se guarda en la configuracion.
     */
    String getNombre();

    /**
     * Nombre: elegirSiguiente
     * Entradas: preparados, procesos PREPARADO en el orden en que llegaron a
     *           la cola de listos
     * Salidas: el proceso que debe recibir la CPU, o nulo si la lista esta
     *          vacia
     * Restricciones: no debe modificar la lista
     * Descripcion: la decision de planificacion.
     */
    Proceso elegirSiguiente(List<Proceso> preparados);

    /**
     * Nombre: debeExpropiar
     * Entradas: enEjecucion, proceso que tiene la CPU; preparados, cola de
     *           listos
     * Salidas: true si hay que quitarle la CPU al proceso en ejecucion
     * Restricciones: ninguna
     * Descripcion: los algoritmos no expropiativos, como FCFS, nunca quitan
     *              la CPU; RR y SRT lo redefiniran en el proyecto 2.
     */
    default boolean debeExpropiar(Proceso enEjecucion, List<Proceso> preparados) {
        return false;
    }
}
