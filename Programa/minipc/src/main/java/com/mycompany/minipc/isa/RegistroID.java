package com.mycompany.minipc.isa;

/**
 * Nombre: RegistroID
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: ninguna
 * Descripcion: identidad de los cuatro registros de proposito general del
 *              Mini PC. Los nombres AX, BX, CX y DX estan tomados del x86,
 *              pero aqui son simples identificadores sin relacion con los
 *              registros reales del procesador.
 */
public enum RegistroID {

    AX,
    BX,
    CX,
    DX;

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
