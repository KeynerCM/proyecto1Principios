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
 *              mensajes de error, por ejemplo "REG, NUMERO".
 */
public enum Forma {

    /** Un registro, por ejemplo "ADD BX". */
    REGISTRO("REG", List.of(TipoOperando.REGISTRO)),

    /** Un registro y un valor inmediato, por ejemplo "MOV BX, 5". */
    REGISTRO_NUMERO("REG, NUMERO", List.of(TipoOperando.REGISTRO, TipoOperando.NUMERO));

    /**
     * Nombre: TipoOperando
     * Entradas: no aplica, es una enumeracion de valores fijos
     * Salidas: no aplica
     * Restricciones: ninguna
     * Descripcion: que se espera en cada posicion de operando, con la
     *              expresion regular que lo reconoce.
     */
    public enum TipoOperando {

        /** Un nombre de registro. */
        REGISTRO(Sintaxis.IDENT),

        /** Un entero con signo opcional. */
        NUMERO(Sintaxis.NUM);

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

    /**
     * Nombre: Forma
     * Entradas: descripcion, como se muestra la forma en los mensajes;
     *           operandos, tipo de cada operando en orden
     * Salidas: la constante construida
     * Restricciones: privado, solo lo invoca la propia enumeracion
     * Descripcion: guarda la descripcion y los tipos de operando.
     */
    Forma(String descripcion, List<TipoOperando> operandos) {
        this.descripcion = descripcion;
        this.operandos = operandos;
    }

    /**
     * Nombre: getOperandos
     * Entradas: ninguna
     * Salidas: el tipo de cada operando, en orden
     * Restricciones: la lista es inmutable
     * Descripcion: el ensamblador la usa para convertir cada grupo de la
     *              expresion regular y para diagnosticar errores.
     */
    public List<TipoOperando> getOperandos() {
        return operandos;
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
     * Descripcion: concatena el mnemonico y los operandos de la forma.
     */
    private String armar(OpCode opcode, String separador) {
        StringBuilder regex = new StringBuilder(opcode.name());
        for (int i = 0; i < operandos.size(); i++) {
            regex.append(i == 0 ? Sintaxis.ESP : separador);
            regex.append(operandos.get(i).getRegex());
        }
        return regex.toString();
    }
}
