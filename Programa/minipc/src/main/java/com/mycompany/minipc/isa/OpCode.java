package com.mycompany.minipc.isa;

import java.util.List;

/**
 * Nombre: OpCode
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: los pesos son fijos
 * Descripcion: juego de instrucciones del Mini PC. Cada operacion declara su
 *              peso, es decir, cuantos segundos de CPU consume, y las formas
 *              en que se pueden escribir sus operandos. El ensamblador solo
 *              acepta una linea si coincide completa con alguna de ellas. El
 *              peso de INT depende del codigo de interrupcion, por eso se
 *              toma de Interrupcion.
 */
public enum OpCode {

    /** AC recibe el valor del registro. */
    LOAD(2, Forma.REGISTRO),

    /** El registro recibe el valor del AC. */
    STORE(2, Forma.REGISTRO),

    /** El registro destino recibe otro registro, un valor inmediato o la direccion de un texto. */
    MOV(1, Forma.REGISTRO_REGISTRO, Forma.REGISTRO_NUMERO, Forma.REGISTRO_TEXTO),

    /** AC recibe AC mas el registro. */
    ADD(3, Forma.REGISTRO),

    /** AC recibe AC menos el registro. */
    SUB(3, Forma.REGISTRO),

    /** Incrementa en 1 el AC, o el registro indicado. */
    INC(1, Forma.SIN_OPERANDOS, Forma.REGISTRO),

    /** Decrementa en 1 el AC, o el registro indicado. */
    DEC(1, Forma.SIN_OPERANDOS, Forma.REGISTRO),

    /** Intercambia los valores de dos registros. */
    SWAP(1, Forma.REGISTRO_REGISTRO),

    /** Pide un servicio al sistema operativo; el peso depende del codigo. */
    INT(OpCode.PESO_DE_LA_INTERRUPCION, Forma.INTERRUPCION),

    /** Salta segun el desplazamiento. */
    JMP(2, Forma.DESPLAZAMIENTO),

    /** Compara dos registros y deja el resultado para JE y JNE. */
    CMP(2, Forma.REGISTRO_REGISTRO),

    /** Salta si la ultima comparacion dio igual. */
    JE(2, Forma.DESPLAZAMIENTO),

    /** Salta si la ultima comparacion dio distinto. */
    JNE(2, Forma.DESPLAZAMIENTO),

    /** Guarda en la pila de uno a tres valores de entrada. */
    PARAM(3, Forma.PARAMETROS),

    /** Guarda en la pila el valor del registro. */
    PUSH(1, Forma.REGISTRO),

    /** Saca el valor del tope de la pila y lo guarda en el registro. */
    POP(1, Forma.REGISTRO);

    /** Marca del peso de INT, que no es fijo sino que depende del codigo. */
    public static final int PESO_DE_LA_INTERRUPCION = 0;

    private final int peso;
    private final List<Forma> formas;

    /**
     * Nombre: OpCode
     * Entradas: peso, segundos de CPU que consume; formas, maneras validas
     *           de escribir los operandos
     * Salidas: la constante construida
     * Restricciones: privado, solo lo invoca la propia enumeracion
     * Descripcion: asocia a cada operacion su peso y sus formas validas.
     */
    OpCode(int peso, Forma... formas) {
        this.peso = peso;
        this.formas = List.of(formas);
    }

    /**
     * Nombre: getPeso
     * Entradas: ninguna
     * Salidas: los segundos de CPU que consume la operacion
     * Restricciones: para INT devuelve PESO_DE_LA_INTERRUPCION; el peso real
     *                se obtiene de la instruccion, que conoce el codigo
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getPeso() {
        return peso;
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
     * Nombre: esSalto
     * Entradas: ninguna
     * Salidas: true si la operacion cambia el PC con un desplazamiento
     * Restricciones: ninguna
     * Descripcion: el ensamblador la usa para validar que el destino de los
     *              saltos quede dentro del programa.
     */
    public boolean esSalto() {
        return formas.contains(Forma.DESPLAZAMIENTO);
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
