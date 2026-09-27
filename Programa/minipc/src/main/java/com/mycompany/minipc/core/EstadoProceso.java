package com.mycompany.minipc.core;

/**
 * Nombre: EstadoProceso
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: el simulador no multiprograma, asi que no existen los
 *                estados suspendidos ni una cola de listos con varios
 *                procesos
 * Descripcion: estados por los que pasa el proceso cargado en el Mini PC.
 *              Corresponden al modelo de cinco estados. Las transiciones que ocurren son:
 *              NUEVO a LISTO al terminar de cargarse en memoria;
 *              LISTO a EJECUCION al ejecutarse la primera instruccion;
 *              EJECUCION a TERMINADO al pasar el PC la ultima instruccion;
 *              EJECUCION a BLOQUEADO_ERROR ante un error de ejecucion.
 */
public enum EstadoProceso {

    /** Cargado en memoria, todavia sin admitir. */
    NUEVO,

    /** Listo para ejecutar, esperando el procesador. */
    LISTO,

    /** Ejecutandose en el procesador. */
    EJECUCION,

    /** Termino normalmente. */
    TERMINADO,

    /** Detenido por un error de ejecucion, como un desbordamiento. */
    BLOQUEADO_ERROR;

    /**
     * Nombre: esFinal
     * Entradas: ninguna
     * Salidas: true si el proceso ya no puede seguir ejecutando
     * Restricciones: ninguna
     * Descripcion: agrupa TERMINADO y BLOQUEADO_ERROR, que son distintos para
     *              el usuario pero equivalentes para el procesador: en ambos
     *              casos paso() deja de avanzar. Tenerlo en un metodo evita
     *              repetir la comparacion doble por todo el codigo.
     */
    public boolean esFinal() {
        return this == TERMINADO || this == BLOQUEADO_ERROR;
    }
}
