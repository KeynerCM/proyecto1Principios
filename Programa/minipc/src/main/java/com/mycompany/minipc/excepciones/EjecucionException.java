package com.mycompany.minipc.excepciones;

/**
 * Nombre: EjecucionException
 * Entradas: mensaje que describe el error ocurrido al ejecutar
 * Salidas: no aplica
 * Restricciones: es una excepcion no verificada, porque se origina en medio
 *                del ciclo de instruccion, dentro de Procesador.paso(), cuyo
 *                contrato no declara excepciones
 * Descripcion: error de un programa en ejecucion, lo que Stallings llama una
 *              interrupcion de programa o trap (tabla 1.1 y tabla 3.8): una
 *              condicion causada por la propia instruccion, como un salto
 *              fuera del programa o una pila desbordada. El procesador deja
 *              el proceso en BLOQUEADO_ERROR y el controlador de la interfaz
 *              la atrapa para informar al usuario.
 */
public class EjecucionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Nombre: EjecucionException
     * Entradas: mensaje, texto que la interfaz muestra tal cual al usuario
     * Salidas: la excepcion construida
     * Restricciones: el mensaje debe ser concreto
     * Descripcion: crea la excepcion con el mensaje indicado.
     */
    public EjecucionException(String mensaje) {
        super(mensaje);
    }
}
