package com.mycompany.minipc.excepciones;

import java.util.List;

/**
 * Nombre: ConfiguracionException
 * Entradas: uno o varios problemas encontrados en la configuracion
 * Salidas: no aplica
 * Restricciones: es una excepcion verificada; la lista que transporta es de
 *                solo lectura
 * Descripcion: reune todos los valores invalidos del archivo de configuracion
 *              para reportarlos de una sola vez, igual que el ensamblador con
 *              los errores de sintaxis, de modo que el usuario corrija el
 *              archivo en una pasada.
 */
public class ConfiguracionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final List<String> errores;

    /**
     * Nombre: ConfiguracionException
     * Entradas: errores, mensajes que describen cada valor invalido
     * Salidas: la excepcion construida
     * Restricciones: la lista no debe ser nula ni vacia; se copia, de modo
     *                que modificarla despues no afecta a la excepcion
     * Descripcion: construye la excepcion y arma el mensaje general uniendo
     *              los errores con saltos de linea.
     */
    public ConfiguracionException(List<String> errores) {
        super(String.join(System.lineSeparator(), errores));
        this.errores = List.copyOf(errores);
    }

    /**
     * Nombre: getErrores
     * Entradas: ninguna
     * Salidas: los mensajes de error, en el orden en que se detectaron
     * Restricciones: la lista devuelta es inmutable
     * Descripcion: la interfaz la usa para mostrar todos los problemas juntos.
     */
    public List<String> getErrores() {
        return errores;
    }
}
