package com.mycompany.minipc.hardware;

/**
 * Nombre: Fase
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: ninguna
 * Descripcion: momento del ciclo de instruccion en que el procesador avisa a
 *              sus observadores. FETCH y EXECUTE,CARGA y REINICIO no son
 *              etapas del ciclo, pero son los otros momentos en que la
 *              interfaz necesita refrescarse.
 */
public enum Fase {

    /** Se trajo la instruccion de memoria al registro IR. */
    FETCH,

    /** Se interpreto y ejecuto la instruccion. */
    EXECUTE,

    /** Se acaba de cargar un programa en memoria. */
    CARGA,

    /** El procesador volvio al inicio del programa. */
    REINICIO
}
