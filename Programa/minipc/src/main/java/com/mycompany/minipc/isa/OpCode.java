package com.mycompany.minipc.isa;

import java.util.List;

/**
 * Nombre: OpCode
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: ninguna
 * Descripcion: juego de instrucciones del Mini PC. Cada operacion declara las
 *              formas en que se pueden escribir sus operandos; el ensamblador
 *              solo acepta una linea si coincide completa con alguna de ellas.
 */
public enum OpCode {

    /** Carga un valor inmediato en un registro. Rx recibe el operando. */
    MOV(Forma.REGISTRO_NUMERO),

    /** Copia el contenido de un registro al acumulador. AC recibe Rx. */
    LOAD(Forma.REGISTRO),

    /** Copia el acumulador a un registro. Rx recibe AC. */
    STORE(Forma.REGISTRO),

    /** Suma un registro al acumulador. AC recibe AC mas Rx. */
    ADD(Forma.REGISTRO),

    /** Resta un registro del acumulador. AC recibe AC menos Rx. */
    SUB(Forma.REGISTRO);

    private final List<Forma> formas;

    /**
     * Nombre: OpCode
     * Entradas: formas, maneras validas de escribir los operandos
     * Salidas: la constante construida
     * Restricciones: privado, solo lo invoca la propia enumeracion
     * Descripcion: asocia a cada operacion sus formas validas.
     */
    OpCode(Forma... formas) {
        this.formas = List.of(formas);
    }

    /**
     * Nombre: getFormas
     * Entradas: ninguna
     * Salidas: las formas validas de la operacion, en orden
     * Restricciones: la lista es inmutable
     * Descripcion: el ensamblador prueba la linea contra cada una.
     */
    public List<Forma> getFormas() {
        return formas;
    }

    /**
     * Nombre: requiereInmediato
     * Entradas: ninguna
     * Salidas: true si la instruccion lleva un valor inmediato
     * Restricciones: ninguna
     * Descripcion: solo MOV lo lleva; el resto operan unicamente sobre
     *              registros y dejan el campo de operando en cero.
     */
    public boolean requiereInmediato() {
        return formas.contains(Forma.REGISTRO_NUMERO);
    }

    /**
     * Nombre: describirFormas
     * Entradas: ninguna
     * Salidas: las formas validas en texto, por ejemplo "MOV REG, NUMERO"
     * Restricciones: ninguna
     * Descripcion: se usa en los mensajes de error para decir que se
     *              esperaba; si hay varias formas se unen con " o ".
     */
    public String describirFormas() {
        StringBuilder texto = new StringBuilder();
        for (Forma forma : formas) {
            if (texto.length() > 0) {
                texto.append(" o ");
            }
            texto.append('"').append(forma.describir(this)).append('"');
        }
        return texto.toString();
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
