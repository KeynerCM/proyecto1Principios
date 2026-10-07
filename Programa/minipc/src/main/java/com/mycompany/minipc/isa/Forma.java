package com.mycompany.minipc.isa;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Nombre: Forma
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: el orden de los tipos de operando coincide con el orden de
 *                los grupos de la expresion regular
 * Descripcion: las maneras validas de escribir los operandos de una
 *              instruccion. Cada forma sabe armar la expresion regular que la
 *              linea debe cumplir completa, y describirse en texto para los
 *              mensajes de error, por ejemplo "REG, NUMERO". Una forma puede
 *              tener operandos opcionales al final, como PARAM, que admite
 *              de uno a tres valores.
 */
public enum Forma {

    /** Sin operandos, por ejemplo "INC". */
    SIN_OPERANDOS("", List.of(), 0),

    /** Un registro, por ejemplo "ADD BX". */
    REGISTRO("REG", List.of(TipoOperando.REGISTRO), 1),

    /** Dos registros, por ejemplo "MOV BX, AX". */
    REGISTRO_REGISTRO("REG, REG",
            List.of(TipoOperando.REGISTRO, TipoOperando.REGISTRO), 2),

    /** Un registro y un valor inmediato, por ejemplo "MOV BX, 5". */
    REGISTRO_NUMERO("REG, NUMERO",
            List.of(TipoOperando.REGISTRO, TipoOperando.NUMERO), 2),

    /**
     * Un registro y un texto entre comillas, por ejemplo
     * MOV DX, "datos.txt". El registro recibe la direccion de memoria de la
     * instruccion, que es donde queda guardado el texto.
     */
    REGISTRO_TEXTO("REG, \"TEXTO\"",
            List.of(TipoOperando.REGISTRO, TipoOperando.TEXTO), 2),

    /** Un codigo de interrupcion, por ejemplo "INT 21H". */
    INTERRUPCION("CODIGO", List.of(TipoOperando.INTERRUPCION), 1),

    /** Un desplazamiento con signo opcional, por ejemplo "JNE -3". */
    DESPLAZAMIENTO("+/-DESPLAZAMIENTO", List.of(TipoOperando.DESPLAZAMIENTO), 1),

    /** De uno a tres valores numericos, por ejemplo "PARAM 1, 2, 3". */
    PARAMETROS("v1[, v2[, v3]]",
            List.of(TipoOperando.NUMERO, TipoOperando.NUMERO, TipoOperando.NUMERO), 1);

    /**
     * Nombre: TipoOperando
     * Entradas: no aplica, es una enumeracion de valores fijos
     * Salidas: no aplica
     * Restricciones: ninguna
     * Descripcion: que se espera en cada posicion de operando, con la
     *              expresion regular que lo reconoce.
     */
    public enum TipoOperando {

        /** Un nombre de registro; el nombre se valida despues. */
        REGISTRO(Sintaxis.IDENT),

        /** Un entero con signo opcional, o un hexadecimal con sufijo h. */
        NUMERO(Sintaxis.NUM),

        /** Un desplazamiento de salto, entero con signo opcional. */
        DESPLAZAMIENTO(Sintaxis.DESP),

        /** Un texto entre comillas, como el nombre de un archivo. */
        TEXTO(Sintaxis.TEXTO),

        /** Un codigo de interrupcion; el codigo se valida despues. */
        INTERRUPCION(Sintaxis.CODIGO);

        private final String regex;

        TipoOperando(String regex) {
            this.regex = regex;
        }

        /**
         * Nombre: getRegex
         * Entradas: ninguna
         * Salidas: la expresion regular del operando, con su grupo
         * Restricciones: ninguna
         * Descripcion: acceso de solo lectura al campo correspondiente.
         */
        public String getRegex() {
            return regex;
        }
    }

    private final String descripcion;
    private final List<TipoOperando> operandos;
    private final int minimo;

    /**
     * Nombre: Forma
     * Entradas: descripcion, como se muestra la forma en los mensajes;
     *           operandos, tipo de cada operando en orden; minimo, cuantos
     *           de ellos son obligatorios
     * Salidas: la constante construida
     * Restricciones: privado, solo lo invoca la propia enumeracion
     * Descripcion: guarda la descripcion, los tipos y el minimo.
     */
    Forma(String descripcion, List<TipoOperando> operandos, int minimo) {
        this.descripcion = descripcion;
        this.operandos = operandos;
        this.minimo = minimo;
    }

    /**
     * Nombre: getOperandos
     * Entradas: ninguna
     * Salidas: el tipo de cada operando posible, en orden
     * Restricciones: la lista es inmutable
     * Descripcion: el ensamblador la usa para convertir cada grupo de la
     *              expresion regular y para diagnosticar errores.
     */
    public List<TipoOperando> getOperandos() {
        return operandos;
    }

    /**
     * Nombre: getMinimo
     * Entradas: ninguna
     * Salidas: cuantos operandos son obligatorios
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getMinimo() {
        return minimo;
    }

    /**
     * Nombre: getMaximo
     * Entradas: ninguna
     * Salidas: cuantos operandos admite como maximo
     * Restricciones: ninguna
     * Descripcion: es el largo de la lista de tipos.
     */
    public int getMaximo() {
        return operandos.size();
    }

    /**
     * Nombre: admite
     * Entradas: cantidad, numero de operandos de una linea
     * Salidas: true si la forma acepta esa cantidad
     * Restricciones: ninguna
     * Descripcion: usada para validar instrucciones y para diagnosticar.
     */
    public boolean admite(int cantidad) {
        return cantidad >= minimo && cantidad <= getMaximo();
    }

    /**
     * Nombre: describir
     * Entradas: opcode, operacion a la que se aplica la forma
     * Salidas: la forma completa en texto, por ejemplo "MOV REG, NUMERO"
     * Restricciones: ninguna
     * Descripcion: se usa en los mensajes que dicen que se esperaba.
     */
    public String describir(OpCode opcode) {
        return descripcion.isEmpty() ? opcode.name() : opcode.name() + " " + descripcion;
    }

    /**
     * Nombre: patron
     * Entradas: opcode, operacion a la que se aplica la forma
     * Salidas: la expresion regular que la linea debe cumplir completa
     * Restricciones: no distingue mayusculas; se usa con Matcher.matches(),
     *                que exige que coincida la linea entera
     * Descripcion: arma el patron con el mnemonico, un espacio y los
     *              operandos separados por exactamente una coma.
     */
    public Pattern patron(OpCode opcode) {
        return Pattern.compile(armar(opcode, Sintaxis.SEP), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Nombre: patronSinComa
     * Entradas: opcode, operacion a la que se aplica la forma
     * Salidas: la misma expresion con espacios en lugar de comas
     * Restricciones: solo sirve para diagnosticar; nunca para aceptar lineas
     * Descripcion: si la linea cumple este patron pero no el normal, lo unico
     *              que le falta es la coma, y el mensaje puede decirlo.
     */
    public Pattern patronSinComa(OpCode opcode) {
        return Pattern.compile(armar(opcode, Sintaxis.ESP), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Nombre: armar
     * Entradas: opcode, operacion; separador, expresion entre operandos
     * Salidas: la expresion regular completa
     * Restricciones: ninguna
     * Descripcion: concatena el mnemonico y los operandos. Los operandos a
     *              partir del minimo van dentro de un grupo opcional, con su
     *              separador, de modo que "PARAM 1" y "PARAM 1, 2, 3" cumplen
     *              el mismo patron pero "PARAM 1,, 2" no.
     */
    private String armar(OpCode opcode, String separador) {
        StringBuilder regex = new StringBuilder(opcode.name());
        for (int i = 0; i < operandos.size(); i++) {
            String pieza = (i == 0 ? Sintaxis.ESP : separador) + operandos.get(i).getRegex();
            regex.append(i < minimo ? pieza : "(?:" + pieza + ")?");
        }
        return regex.toString();
    }
}
