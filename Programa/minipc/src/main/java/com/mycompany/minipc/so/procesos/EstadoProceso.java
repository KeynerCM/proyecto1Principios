package com.mycompany.minipc.so.procesos;

/**
 * Nombre: EstadoProceso
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: el BCP guarda el nombre del estado como texto en su celda,
 *                asi que los nombres no deben cambiar
 * Descripcion: los siete estados del enunciado, que son los del modelo de
 *              siete estados de Stallings (seccion 3.2, figura 3.9b). El
 *              "suspendido" del enunciado se parte en dos, segun el proceso
 *              este esperando un evento o no:
 *
 *                NUEVO                 New
 *                PREPARADO             Ready
 *                EJECUCION             Running
 *                EN_ESPERA             Blocked
 *                SUSPENDIDO_PREPARADO  Ready/Suspend
 *                SUSPENDIDO_EN_ESPERA  Blocked/Suspend
 *                FINALIZADO            Exit
 */
public enum EstadoProceso {

    /** En la lista de trabajos, todavia sin admitir. */
    NUEVO,

    /** En memoria, esperando el procesador. */
    PREPARADO,

    /** Ejecutandose en el procesador. */
    EJECUCION,

    /** Esperando un evento de entrada o salida. */
    EN_ESPERA,

    /** En la memoria virtual, listo para volver a memoria principal. */
    SUSPENDIDO_PREPARADO,

    /** En la memoria virtual y ademas esperando un evento. */
    SUSPENDIDO_EN_ESPERA,

    /** Termino, normalmente o por un error. */
    FINALIZADO;

    /**
     * Nombre: esFinal
     * Entradas: ninguna
     * Salidas: true si el proceso ya no puede seguir ejecutando
     * Restricciones: ninguna
     * Descripcion: evita comparar contra FINALIZADO por todo el codigo.
     */
    public boolean esFinal() {
        return this == FINALIZADO;
    }
}
