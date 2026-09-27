package com.mycompany.minipc.core;

import com.mycompany.minipc.isa.Instruccion;

/**
 * Nombre: CeldaDisco
 * Entradas: no aplica, se crea vacia y se llena despues
 * Salidas: no aplica
 * Restricciones: una celda guarda una sola cosa a la vez: una entrada del
 *                indice o una instruccion de un programa
 * Descripcion: una posicion del disco del Mini PC. Las primeras posiciones
 *              guardan el indice de archivos y el resto guarda el contenido
 *              de los archivos, una instruccion por posicion, igual que en
 *              la memoria principal.
 */
public class CeldaDisco {

    /**
     * Nombre: Tipo
     * Entradas: no aplica, es una enumeracion de valores fijos
     * Salidas: no aplica
     * Restricciones: ninguna
     * Descripcion: que guarda la celda. Determina ademas el color con que la
     *              interfaz la pinta en la tabla del disco.
     */
    public enum Tipo {

        /** Disponible, nunca se escribio o se libero. */
        LIBRE,

        /** Contiene una entrada del indice de archivos. */
        INDICE,

        /** Contiene una instruccion de un programa guardado. */
        PROGRAMA
    }

    private Tipo tipo;
    private EntradaIndice entrada;
    private Instruccion instruccion;

    /**
     * Nombre: CeldaDisco
     * Entradas: ninguna
     * Salidas: la celda construida, libre
     * Restricciones: ninguna
     * Descripcion: crea una celda vacia delegando en limpiar(), de modo que
     *              el estado inicial y el estado tras limpiar son el mismo.
     */
    public CeldaDisco() {
        limpiar();
    }

    /**
     * Nombre: escribirIndice
     * Entradas: entrada, fila del indice a guardar
     * Salidas: ninguna
     * Restricciones: la entrada no debe ser nula
     * Descripcion: convierte la celda en una fila del indice de archivos.
     */
    public void escribirIndice(EntradaIndice entrada) {
        limpiar();
        this.tipo = Tipo.INDICE;
        this.entrada = entrada;
    }

    /**
     * Nombre: escribirInstruccion
     * Entradas: instruccion, linea de programa a guardar
     * Salidas: ninguna
     * Restricciones: la instruccion no debe ser nula
     * Descripcion: guarda una instruccion de un programa en la celda.
     */
    public void escribirInstruccion(Instruccion instruccion) {
        limpiar();
        this.tipo = Tipo.PROGRAMA;
        this.instruccion = instruccion;
    }

    /**
     * Nombre: limpiar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: es final porque el constructor la invoca
     * Descripcion: deja la celda libre y sin contenido.
     */
    public final void limpiar() {
        this.tipo = Tipo.LIBRE;
        this.entrada = null;
        this.instruccion = null;
    }

    /**
     * Nombre: getTipo
     * Entradas: ninguna
     * Salidas: que guarda la celda
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * Nombre: getEntrada
     * Entradas: ninguna
     * Salidas: la fila del indice, o nulo si la celda no es del indice
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public EntradaIndice getEntrada() {
        return entrada;
    }

    /**
     * Nombre: getInstruccion
     * Entradas: ninguna
     * Salidas: la instruccion guardada, o nulo si la celda no es de programa
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Instruccion getInstruccion() {
        return instruccion;
    }

    /**
     * Nombre: estaLibre
     * Entradas: ninguna
     * Salidas: true si la celda no tiene contenido
     * Restricciones: ninguna
     * Descripcion: evita que quien consulte tenga que comparar contra el
     *              valor concreto del enum.
     */
    public boolean estaLibre() {
        return tipo == Tipo.LIBRE;
    }

    /**
     * Nombre: getEtiqueta
     * Entradas: ninguna
     * Salidas: el texto legible de lo que guarda la celda
     * Restricciones: es cadena vacia si la celda esta libre
     * Descripcion: es lo que se muestra en la columna de contenido de la
     *              tabla del disco.
     */
    public String getEtiqueta() {
        switch (tipo) {
            case INDICE:
                return entrada.toString();
            case PROGRAMA:
                return instruccion.getTextoFuente();
            default:
                return "";
        }
    }

    /**
     * Nombre: toString
     * Entradas: ninguna
     * Salidas: representacion legible de la celda
     * Restricciones: ninguna
     * Descripcion: devuelve la etiqueta, o la marca "[libre]" si no guarda
     *              nada. Pensado para depuracion.
     */
    @Override
    public String toString() {
        return estaLibre() ? "[libre]" : getEtiqueta();
    }
}
