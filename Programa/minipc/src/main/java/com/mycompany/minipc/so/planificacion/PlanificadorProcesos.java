package com.mycompany.minipc.so.planificacion;

import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.ListaProcesos;
import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: PlanificadorProcesos
 * Entradas: la lista de procesos y el algoritmo de planificacion
 * Salidas: no aplica
 * Restricciones: solo conoce la interfaz AlgoritmoPlanificacion, nunca FCFS
 * Descripcion: planificador de procesos (de corto plazo). Arma la cola de
 *              listos a partir de la lista de procesos en memoria y le pregunta
 *              al algoritmo cual sigue.
 */
public class PlanificadorProcesos {

    private final ListaProcesos procesos;
    private AlgoritmoPlanificacion algoritmo;

    /**
     * Nombre: PlanificadorProcesos
     * Entradas: procesos, lista de procesos; algoritmo, estrategia a usar
     * Salidas: el planificador construido
     * Restricciones: el algoritmo no debe ser nulo
     * Descripcion: recibe el algoritmo ya creado (inversion de dependencias).
     */
    public PlanificadorProcesos(ListaProcesos procesos, AlgoritmoPlanificacion algoritmo) {
        this.procesos = procesos;
        setAlgoritmo(algoritmo);
    }

    /**
     * Nombre: elegir
     * Entradas: ninguna
     * Salidas: el proceso que debe recibir la CPU, o nulo si no hay preparados
     * Restricciones: no cambia ningun estado
     * Descripcion: la cola de listos son los procesos PREPARADO de la lista.
     */
    public Proceso elegir() {
        return algoritmo.elegirSiguiente(procesos.conEstado(EstadoProceso.PREPARADO));
    }

    /**
     * Nombre: getAlgoritmo
     * Entradas: ninguna
     * Salidas: el algoritmo en uso
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public AlgoritmoPlanificacion getAlgoritmo() {
        return algoritmo;
    }

    /**
     * Nombre: setAlgoritmo
     * Entradas: algoritmo, nueva estrategia
     * Salidas: ninguna
     * Restricciones: lanza IllegalArgumentException si es nulo
     * Descripcion: permite cambiar el algoritmo desde la configuracion.
     */
    public final void setAlgoritmo(AlgoritmoPlanificacion algoritmo) {
        if (algoritmo == null) {
            throw new IllegalArgumentException("El algoritmo de planificacion es obligatorio");
        }
        this.algoritmo = algoritmo;
    }
}
