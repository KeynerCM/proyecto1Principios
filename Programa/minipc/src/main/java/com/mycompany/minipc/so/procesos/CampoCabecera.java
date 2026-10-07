package com.mycompany.minipc.so.procesos;

/**
 * Nombre: CampoCabecera
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: el orden de las constantes es el orden de las celdas al
 *                inicio de la memoria
 * Descripcion: las celdas con las que empieza la zona del kernel, antes de la
 *              tabla de BCP. Son los datos globales que el sistema operativo
 *              necesita para recorrer sus estructuras: que proceso tiene la
 *              CPU, donde empieza la lista de procesos y cuantos procesos
 *              hay admitidos.
 */
public enum CampoCabecera {

    EN_EJECUCION("En ejecucion"),
    INICIO_LISTA("Inicio lista"),
    PROCESOS_ADMITIDOS("Admitidos");

    private final String etiqueta;

    /**
     * Nombre: CampoCabecera
     * Entradas: etiqueta, nombre corto que se muestra en la tabla de memoria
     * Salidas: la constante construida
     * Restricciones: ninguna
     * Descripcion: asocia a cada campo el texto que ve el usuario, por
     *              ejemplo "SO.Admitidos".
     */
    CampoCabecera(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Nombre: getDireccion
     * Entradas: ninguna
     * Salidas: la direccion de memoria de la celda
     * Restricciones: ninguna
     * Descripcion: la cabecera empieza en la direccion cero, asi que la
     *              direccion es la posicion de la constante.
     */
    public int getDireccion() {
        return ordinal();
    }

    /**
     * Nombre: getEtiqueta
     * Entradas: ninguna
     * Salidas: el nombre corto del campo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public String getEtiqueta() {
        return etiqueta;
    }
}
