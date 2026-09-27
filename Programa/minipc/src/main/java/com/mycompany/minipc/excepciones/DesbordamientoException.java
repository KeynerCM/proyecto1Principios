package com.mycompany.minipc.excepciones;

/**
 * Nombre: DesbordamientoException
 * Entradas: mensaje que describe el desbordamiento ocurrido
 * Salidas: no aplica
 * Restricciones: es una excepcion no verificada, porque puede originarse en
 *                medio del ciclo de ejecucion, dentro de Procesador.paso(),
 *                cuyo contrato no declara excepciones
 * Descripcion: se lanza cuando una estructura de tamano fijo del proceso se
 *              desborda, por ejemplo la pila. El controlador de la interfaz
 *              la atrapa y deja el proceso en BLOQUEADO_ERROR.
 */
public class DesbordamientoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Nombre: DesbordamientoException
     * Entradas: mensaje, texto que explica que se desbordo y cual era el
     *           limite admitido
     * Salidas: la excepcion construida
     * Restricciones: ninguna
     * Descripcion: crea la excepcion con un mensaje que la interfaz muestra
     *              tal cual al usuario, por lo que conviene que sea concreto.
     */
    public DesbordamientoException(String mensaje) {
        super(mensaje);
    }
}
