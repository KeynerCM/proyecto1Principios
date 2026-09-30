package com.mycompany.minipc.so.planificacion;

import java.util.List;

/**
 * Nombre: FabricaAlgoritmos
 * Entradas: el nombre de un algoritmo, tal como viene de la configuracion
 * Salidas: el algoritmo correspondiente
 * Restricciones: en este proyecto solo existe FCFS
 * Descripcion: el unico lugar que conoce las implementaciones. Agregar un
 *              algoritmo en el proyecto 2 es crear su clase y agregar una
 *              linea aqui.
 */
public final class FabricaAlgoritmos {

    /**
     * Nombre: FabricaAlgoritmos
     * Entradas: ninguna
     * Salidas: no aplica
     * Restricciones: no se instancia
     * Descripcion: clase de utilidad con metodos estaticos.
     */
    private FabricaAlgoritmos() {
    }

    /**
     * Nombre: disponibles
     * Entradas: ninguna
     * Salidas: los nombres de los algoritmos que se pueden elegir
     * Restricciones: ninguna
     * Descripcion: alimenta la validacion de la configuracion y el combo del
     *              dialogo.
     */
    public static List<String> disponibles() {
        return List.of(FCFS.NOMBRE);
    }

    /**
     * Nombre: crear
     * Entradas: nombre, nombre del algoritmo, sin distinguir mayusculas
     * Salidas: una instancia del algoritmo
     * Restricciones: lanza IllegalArgumentException si el algoritmo no existe
     * Descripcion: traduce el texto de la configuracion a un objeto.
     */
    public static AlgoritmoPlanificacion crear(String nombre) {
        if (nombre != null && nombre.trim().equalsIgnoreCase(FCFS.NOMBRE)) {
            return new FCFS();
        }
        throw new IllegalArgumentException("Algoritmo de planificacion no disponible en esta"
                + " version: \"" + nombre + "\". Los disponibles son " + disponibles());
    }
}
