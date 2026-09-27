package com.mycompany.minipc.isa;

/**
 * Nombre: OpCode
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: ninguna
 * Descripcion: juego de instrucciones del Mini PC. Cada operacion indica si
 *              su sintaxis exige un valor inmediato.
 */
public enum OpCode {

    /** Carga un valor inmediato en un registro. Rx recibe el operando. */
    MOV(true),

    /** Copia el contenido de un registro al acumulador. AC recibe Rx. */
    LOAD(false),

    /** Copia el acumulador a un registro. Rx recibe AC. */
    STORE(false),

    /** Suma un registro al acumulador. AC recibe AC mas Rx. */
    ADD(false),

    /** Resta un registro del acumulador. AC recibe AC menos Rx. */
    SUB(false);

    private final boolean requiereInmediato;

    /**
     * Nombre: OpCode
     * Entradas: requiereInmediato, si la sintaxis exige un valor literal
     * Salidas: la constante construida
     * Restricciones: privado, solo lo invoca la propia enumeracion
     * Descripcion: asocia a cada operacion si lleva o no un operando
     *              inmediato.
     */
    OpCode(boolean requiereInmediato) {
        this.requiereInmediato = requiereInmediato;
    }

    /**
     * Nombre: requiereInmediato
     * Entradas: ninguna
     * Salidas: true si la instruccion lleva un valor inmediato
     * Restricciones: ninguna
     * Descripcion: solo MOV lo lleva; el resto operan unicamente sobre
     *              registros y dejan el campo de operando en cero. El
     *              ensamblador lo consulta para saber cuantos tokens esperar.
     */
    public boolean requiereInmediato() {
        return requiereInmediato;
    }

    /**
     * Nombre: desdeMnemonico
     * Entradas: mnemonico, texto de la operacion tal como aparece en el .asm
     * Salidas: la operacion correspondiente
     * Restricciones: si el mnemonico es nulo o no existe lanza
     *                IllegalArgumentException; el ensamblador la atrapa para
     *                convertirla en un error con numero de linea
     * Descripcion: busca la operacion por su nombre sin distinguir mayusculas
     *              y descartando espacios sobrantes.
     */
    public static OpCode desdeMnemonico(String mnemonico) {
        if (mnemonico != null) {
            String buscado = mnemonico.trim().toUpperCase();
            for (OpCode op : values()) {
                if (op.name().equals(buscado)) {
                    return op;
                }
            }
        }
        throw new IllegalArgumentException("Operacion desconocida: " + mnemonico);
    }
}
