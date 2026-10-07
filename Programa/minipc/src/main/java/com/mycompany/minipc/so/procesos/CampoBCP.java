package com.mycompany.minipc.so.procesos;

/**
 * Nombre: CampoBCP
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: el orden de las constantes es el orden de las celdas del BCP
 *                en memoria; cambiarlo cambia el mapa de memoria
 * Descripcion: los campos del Bloque de Control de Proceso, uno por celda de
 *              memoria: PID, nombre del programa, estado, prioridad, PC, IR,
 *              AC, AX a DX, la bandera ZF de CMP, el SP y la pila (cada
 *              elemento en su propia celda), base, alcance, CPU, tiempos,
 *              archivos abiertos y enlace al siguiente BCP. El desplazamiento
 *              de cada campo dentro del BCP es su posicion en esta lista, y el
 *              tamano del BCP es la cantidad de constantes: si se agrega un
 *              campo, el tamano del BCP y del kernel se recalculan solos.
 */
public enum CampoBCP {

    PID("PID"),
    PROGRAMA("Programa"),
    ESTADO("Estado"),
    PRIORIDAD("Prioridad"),
    PC("PC"),
    IR("IR"),
    AC("AC"),
    AX("AX"),
    BX("BX"),
    CX("CX"),
    DX("DX"),
    ZF("ZF"),
    SP("SP"),
    PILA_1("Pila[1]"),
    PILA_2("Pila[2]"),
    PILA_3("Pila[3]"),
    PILA_4("Pila[4]"),
    PILA_5("Pila[5]"),
    BASE("Base"),
    ALCANCE("Alcance"),
    CPU("CPU"),
    TIEMPO_INICIO("Inicio"),
    TIEMPO_EMPLEADO("Empleado"),
    ARCHIVOS_ABIERTOS("Archivos"),
    SIGUIENTE("Siguiente");

    private final String etiqueta;

    /**
     * Nombre: CampoBCP
     * Entradas: etiqueta, nombre corto que se muestra en la tabla de memoria
     * Salidas: la constante construida
     * Restricciones: ninguna
     * Descripcion: asocia a cada campo el texto que ve el usuario, por
     *              ejemplo "P2.PC".
     */
    CampoBCP(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Nombre: getDesplazamiento
     * Entradas: ninguna
     * Salidas: la posicion del campo dentro del BCP, desde cero
     * Restricciones: ninguna
     * Descripcion: es la posicion de la constante en el enum.
     */
    public int getDesplazamiento() {
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

    /**
     * Nombre: pila
     * Entradas: indice, posicion en la pila contando desde cero (0 es el fondo)
     * Salidas: el campo que guarda esa posicion de la pila
     * Restricciones: lanza IllegalArgumentException si el indice no esta
     *                entre 0 y 4
     * Descripcion: las cinco celdas de la pila son consecutivas, asi que se
     *              calculan a partir de la primera.
     */
    public static CampoBCP pila(int indice) {
        if (indice < 0 || indice >= 5) {
            throw new IllegalArgumentException("La pila tiene posiciones de 0 a 4: " + indice);
        }
        return values()[PILA_1.ordinal() + indice];
    }
}
