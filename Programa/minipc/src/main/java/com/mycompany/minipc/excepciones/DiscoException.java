package com.mycompany.minipc.excepciones;

/**
 * Nombre: DiscoException
 * Entradas: mensaje que explica por que no se pudo completar la operacion
 * Salidas: no aplica
 * Restricciones: es una excepcion verificada, por lo que quien guarde, lea o
 *                elimine archivos del disco esta obligado a contemplar el caso
 * Descripcion: se lanza cuando una operacion sobre el disco no se puede
 *              realizar: el indice esta lleno, no hay espacio contiguo, ya
 *              existe un archivo con ese nombre o el archivo buscado no
 *              existe. Las operaciones son atomicas: si esta excepcion se
 *              lanza, el disco queda tal como estaba.
 */
public class DiscoException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Nombre: DiscoException
     * Entradas: mensaje, texto que la interfaz muestra tal cual al usuario
     * Salidas: la excepcion construida
     * Restricciones: el mensaje debe ser concreto y nombrar el archivo
     * Descripcion: crea la excepcion con el mensaje indicado.
     */
    public DiscoException(String mensaje) {
        super(mensaje);
    }
}
