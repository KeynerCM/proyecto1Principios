package com.mycompany.minipc.core;

/**
 * Nombre: EntradaIndice
 * Entradas: el nombre del archivo, la direccion donde empieza y su tamano
 * Salidas: no aplica
 * Restricciones: es inmutable; el nombre no puede ser vacio, la direccion no
 *                puede ser negativa y el tamano debe ser de al menos uno
 * Descripcion: una fila del indice de archivos del disco. El enunciado pide
 *              que el indice guarde el nombre y la direccion donde se
 *              almacena cada archivo; se agrega el tamano para saber cuantas
 *              posiciones leer sin tener que recorrer el disco.
 */
public final class EntradaIndice {

    private final String nombre;
    private final int direccionInicio;
    private final int tamano;

    /**
     * Nombre: EntradaIndice
     * Entradas: nombre, nombre del archivo; direccionInicio, primera posicion
     *           que ocupa en el disco; tamano, cantidad de posiciones
     * Salidas: la entrada construida
     * Restricciones: lanza IllegalArgumentException si algun valor es invalido
     * Descripcion: valida los tres campos de una vez, de modo que una entrada
     *              que existe siempre describe un archivo coherente.
     */
    public EntradaIndice(String nombre, int direccionInicio, int tamano) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo es obligatorio");
        }
        if (direccionInicio < 0) {
            throw new IllegalArgumentException("La direccion de inicio no puede ser negativa");
        }
        if (tamano < 1) {
            throw new IllegalArgumentException("El tamano del archivo debe ser de al menos 1");
        }
        this.nombre = nombre;
        this.direccionInicio = direccionInicio;
        this.tamano = tamano;
    }

    /**
     * Nombre: getNombre
     * Entradas: ninguna
     * Salidas: el nombre del archivo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Nombre: getDireccionInicio
     * Entradas: ninguna
     * Salidas: la primera posicion del disco que ocupa el archivo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getDireccionInicio() {
        return direccionInicio;
    }

    /**
     * Nombre: getTamano
     * Entradas: ninguna
     * Salidas: cuantas posiciones ocupa el archivo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getTamano() {
        return tamano;
    }

    /**
     * Nombre: getDireccionFin
     * Entradas: ninguna
     * Salidas: la ultima posicion del disco que ocupa el archivo
     * Restricciones: ninguna
     * Descripcion: evita repetir el calculo inicio + tamano - 1 en cada lugar
     *              que necesita recorrer el archivo.
     */
    public int getDireccionFin() {
        return direccionInicio + tamano - 1;
    }

    /**
     * Nombre: toString
     * Entradas: ninguna
     * Salidas: la entrada en texto, por ejemplo "file.asm -> 20 (7)"
     * Restricciones: ninguna
     * Descripcion: es lo que muestra la tabla del disco en las celdas del
     *              indice.
     */
    @Override
    public String toString() {
        return nombre + " -> " + direccionInicio + " (" + tamano + ")";
    }
}
