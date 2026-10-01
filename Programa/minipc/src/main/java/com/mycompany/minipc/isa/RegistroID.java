package com.mycompany.minipc.isa;

import java.util.List;

/**
 * Nombre: RegistroID
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: AH y AL no son registros aparte: son la parte alta y la
 *                parte baja de AX (confirmado por el profesor)
 * Descripcion: los nombres de registro que acepta el ensamblador. AX, BX, CX
 *              y DX son los cuatro registros de proposito general; AH y AL
 *              permiten leer y escribir un byte de AX, como en el x86, y los
 *              usa INT 21H: AH elige la operacion y AL lleva el dato.
 */
public enum RegistroID {

    AX,
    BX,
    CX,
    DX,
    AH,
    AL;

    /** Los cuatro registros que existen de verdad, en orden. */
    public static final List<RegistroID> GENERALES = List.of(AX, BX, CX, DX);

    /**
     * Nombre: esMitadDeAx
     * Entradas: ninguna
     * Salidas: true para AH y AL
     * Restricciones: ninguna
     * Descripcion: AH y AL no tienen almacenamiento propio; se calculan a
     *              partir de AX.
     */
    public boolean esMitadDeAx() {
        return this == AH || this == AL;
    }

    /**
     * Nombre: desdeNombre
     * Entradas: nombre, texto del registro tal como aparece en el .asm
     * Salidas: el registro correspondiente
     * Restricciones: si el nombre es nulo o no existe lanza
     *                IllegalArgumentException; el ensamblador la atrapa para
     *                convertirla en un error con numero de linea
     * Descripcion: busca el registro por su nombre sin distinguir mayusculas
     *              y descartando espacios sobrantes.
     */
    public static RegistroID desdeNombre(String nombre) {
        if (nombre != null) {
            String buscado = nombre.trim().toUpperCase();
            for (RegistroID id : values()) {
                if (id.name().equals(buscado)) {
                    return id;
                }
            }
        }
        throw new IllegalArgumentException("Registro inexistente: " + nombre);
    }
}
