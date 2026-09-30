package com.mycompany.minipc.so.planificacion;

import java.util.List;

import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: FCFS
 * Entradas: no aplica
 * Salidas: no aplica
 * Restricciones: no es expropiativo: el proceso usa la CPU hasta terminar o
 *                bloquearse
 * Descripcion: First Come First Served. Stallings (seccion 9.2): "the process
 *              that has been in the ready queue the longest is selected". La
 *              cola de listos llega en orden de llegada, asi que el elegido
 *              es el primero. La prioridad del BCP no se usa.
 */
public class FCFS implements AlgoritmoPlanificacion {

    /** Nombre con el que se identifica en la configuracion. */
    public static final String NOMBRE = "FCFS";

    /**
     * Nombre: getNombre
     * Entradas: ninguna
     * Salidas: "FCFS"
     * Restricciones: ninguna
     * Descripcion: ver AlgoritmoPlanificacion.
     */
    @Override
    public String getNombre() {
        return NOMBRE;
    }

    /**
     * Nombre: elegirSiguiente
     * Entradas: preparados, cola de listos en orden de llegada
     * Salidas: el primero de la cola, o nulo si esta vacia
     * Restricciones: ninguna
     * Descripcion: el que lleva mas tiempo esperando.
     */
    @Override
    public Proceso elegirSiguiente(List<Proceso> preparados) {
        return preparados.isEmpty() ? null : preparados.get(0);
    }
}
