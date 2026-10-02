package com.mycompany.minipc.excepciones;

/**
 * Nombre: ViolacionProteccionException
 * Entradas: el mensaje que describe el acceso no permitido
 * Salidas: no aplica
 * Restricciones: es una EjecucionException: el proceso que la provoca termina
 *                con ese error y los demas siguen
 * Descripcion: un proceso intento usar memoria fuera de su region (base y
 *              alcance), leer la zona del kernel o tocar un archivo que no le
 *              corresponde. Es la interrupcion de programa por "reference
 *              outside a user's allowed memory space" (Stallings, tabla 1.1).
 */
public class ViolacionProteccionException extends EjecucionException {

    private static final long serialVersionUID = 1L;

    /**
     * Nombre: ViolacionProteccionException
     * Entradas: mensaje, descripcion del acceso no permitido
     * Salidas: la excepcion construida
     * Restricciones: ninguna
     * Descripcion: antepone "Violacion de proteccion" para que se reconozca en
     *              la consola y en el cuadro de error.
     */
    public ViolacionProteccionException(String mensaje) {
        super("Violacion de proteccion: " + mensaje);
    }
}
