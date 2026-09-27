package com.mycompany.minipc.core;

import com.mycompany.minipc.isa.RegistroID;

/**
 * Nombre: Registro
 * Entradas: la identidad del registro que representa
 * Salidas: no aplica
 * Restricciones: ninguna
 * Descripcion: un registro de proposito general del Mini PC. El valor se
 *              guarda como entero decimal de Java y la aritmetica se resuelve
 *              directamente sobre el.
 */
public class Registro {

    private final RegistroID id;
    private int valor;

    /**
     * Nombre: Registro
     * Entradas: id, identidad del registro
     * Salidas: el registro construido, con valor cero
     * Restricciones: la identidad no cambia durante la vida del objeto
     * Descripcion: crea el registro en su estado inicial, que es cero, tal
     *              como queda un procesador recien encendido.
     */
    public Registro(RegistroID id) {
        this.id = id;
        this.valor = 0;
    }

    /**
     * Nombre: getId
     * Entradas: ninguna
     * Salidas: la identidad del registro
     * Restricciones: ninguna
     * Descripcion: permite saber de que registro se trata al recorrer el
     *              banco completo.
     */
    public RegistroID getId() {
        return id;
    }

    /**
     * Nombre: getValor
     * Entradas: ninguna
     * Salidas: el contenido actual como entero de Java
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al valor almacenado.
     */
    public int getValor() {
        return valor;
    }

    /**
     * Nombre: setValor
     * Entradas: valor, entero a guardar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: asigna el valor del registro.
     */
    public void setValor(int valor) {
        this.valor = valor;
    }

    /**
     * Nombre: reset
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: devuelve el registro a cero, sin alterar su identidad.
     */
    public void reset() {
        this.valor = 0;
    }

    /**
     * Nombre: toString
     * Entradas: ninguna
     * Salidas: representacion legible del registro, por ejemplo "AX=5"
     * Restricciones: ninguna
     * Descripcion: pensado para depuracion y para los mensajes de las pruebas.
     */
    @Override
    public String toString() {
        return id + "=" + valor;
    }
}
