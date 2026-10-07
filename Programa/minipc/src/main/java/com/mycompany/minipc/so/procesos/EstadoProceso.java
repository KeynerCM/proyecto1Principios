package com.mycompany.minipc.so.procesos;

/**
 * Nombre: EstadoProceso
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: el BCP guarda el nombre del estado como texto en su celda,
 *                asi que los nombres no deben cambiar
 * Descripcion: los siete estados de un proceso. Hay dos estados suspendidos,
 *              segun el proceso este esperando un evento o no; en los dos su
 *              programa esta en la memoria virtual del disco. Al lado, el
 *              nombre en ingles de cada estado:
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
