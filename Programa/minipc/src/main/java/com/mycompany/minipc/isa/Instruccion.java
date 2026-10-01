package com.mycompany.minipc.isa;

import java.util.List;

/**
 * Nombre: Instruccion
 * Entradas: la operacion, la forma con que se escribio, sus operandos, el
 *           texto original y el numero de linea del archivo
 * Salidas: no aplica
 * Restricciones: es inmutable y final; todos sus campos se fijan al
 *                construirla y no hay forma de alterarlos despues
 * Descripcion: una instruccion ya traducida, lista para guardarse en memoria.
 *              Se guarda tal cual en la celda de memoria, sin codificarla.
 *              Conserva ademas el texto original y el numero de linea del
 *              archivo fuente, que la interfaz necesita para llenar la tabla
 *              de instrucciones y para resaltar la que apunta el PC.
 */
public final class Instruccion {

    private final OpCode opcode;
    private final Forma forma;
    private final List<Operando> operandos;
    private final String textoFuente;
    private final int numeroLinea;

    /**
     * Nombre: Instruccion
     * Entradas: opcode, operacion a ejecutar; forma, forma con que se
     *           escribieron los operandos; operandos, en orden; textoFuente,
     *           linea original tal como venia; numeroLinea, posicion dentro
     *           del archivo contando desde uno
     * Salidas: la instruccion construida
     * Restricciones: lanza IllegalArgumentException si la forma no pertenece
     *                a la operacion, si la cantidad de operandos no es la que
     *                admite la forma, o si algun operando no es del tipo
     *                esperado
     * Descripcion: valida de una vez todo lo que podria hacer invalida a la
     *              instruccion, de modo que si el objeto existe es ejecutable.
     */
    public Instruccion(OpCode opcode, Forma forma, List<Operando> operandos,
            String textoFuente, int numeroLinea) {
        if (opcode == null || forma == null || operandos == null) {
            throw new IllegalArgumentException(
                    "La operacion, la forma y los operandos son obligatorios");
        }
        if (!opcode.getFormas().contains(forma)) {
            throw new IllegalArgumentException("La operacion " + opcode
                    + " no admite la forma " + forma);
        }
        if (!forma.admite(operandos.size())) {
            throw new IllegalArgumentException("La forma " + forma + " no admite "
                    + operandos.size() + " operando(s)");
        }
        for (int i = 0; i < operandos.size(); i++) {
            if (operandos.get(i).getTipo() != forma.getOperandos().get(i)) {
                throw new IllegalArgumentException("El operando " + (i + 1) + " de " + opcode
                        + " debe ser de tipo " + forma.getOperandos().get(i));
            }
        }
        this.opcode = opcode;
        this.forma = forma;
        this.operandos = List.copyOf(operandos);
        this.textoFuente = textoFuente;
        this.numeroLinea = numeroLinea;
    }

    /**
     * Nombre: getOpcode
     * Entradas: ninguna
     * Salidas: la operacion que ejecuta la instruccion
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public OpCode getOpcode() {
        return opcode;
    }

    /**
     * Nombre: getForma
     * Entradas: ninguna
     * Salidas: la forma con que se escribieron los operandos
     * Restricciones: ninguna
     * Descripcion: el procesador la consulta para distinguir, por ejemplo,
     *              MOV REG, REG de MOV REG, NUMERO, o INC de INC REG.
     */
    public Forma getForma() {
        return forma;
    }

    /**
     * Nombre: getOperandos
     * Entradas: ninguna
     * Salidas: los operandos, en orden
     * Restricciones: la lista es inmutable
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public List<Operando> getOperandos() {
        return operandos;
    }

    /**
     * Nombre: getRegistro
     * Entradas: indice, posicion del operando contando desde cero
     * Salidas: el registro en esa posicion
     * Restricciones: lanza IllegalStateException si el operando no es un
     *                registro, e IndexOutOfBoundsException si no existe
     * Descripcion: acceso comodo para el procesador, que ya sabe por la forma
     *              que tipo hay en cada posicion.
     */
    public RegistroID getRegistro(int indice) {
        Operando operando = operandos.get(indice);
        if (operando.getTipo() != Forma.TipoOperando.REGISTRO) {
            throw new IllegalStateException("El operando " + (indice + 1) + " de " + this
                    + " no es un registro");
        }
        return operando.getRegistro();
    }

    /**
     * Nombre: getValor
     * Entradas: indice, posicion del operando contando desde cero
     * Salidas: el numero o el desplazamiento en esa posicion
     * Restricciones: lanza IndexOutOfBoundsException si no existe
     * Descripcion: acceso comodo para el procesador.
     */
    public int getValor(int indice) {
        return operandos.get(indice).getValor();
    }

    /**
     * Nombre: getTexto
     * Entradas: indice, posicion del operando contando desde cero
     * Salidas: el texto que iba entre comillas
     * Restricciones: lanza IllegalStateException si el operando no es un texto
     * Descripcion: acceso comodo para el procesador y el sistema de archivos.
     */
    public String getTexto(int indice) {
        Operando operando = operandos.get(indice);
        if (operando.getTipo() != Forma.TipoOperando.TEXTO) {
            throw new IllegalStateException("El operando " + (indice + 1) + " de " + this
                    + " no es un texto");
        }
        return operando.getTexto();
    }

    /**
     * Nombre: getInterrupcion
     * Entradas: ninguna
     * Salidas: la interrupcion pedida, o nulo si no es una instruccion INT
     * Restricciones: ninguna
     * Descripcion: acceso comodo para el procesador.
     */
    public Interrupcion getInterrupcion() {
        return forma == Forma.INTERRUPCION ? operandos.get(0).getInterrupcion() : null;
    }

    /**
     * Nombre: getPeso
     * Entradas: ninguna
     * Salidas: los segundos de CPU que consume la instruccion, o
     *          Interrupcion.PESO_VARIABLE si depende del usuario
     * Restricciones: ninguna
     * Descripcion: el peso de INT sale del codigo de interrupcion; el de las
     *              demas, de la operacion.
     */
    public int getPeso() {
        return opcode == OpCode.INT ? getInterrupcion().getPeso() : opcode.getPeso();
    }

    /**
     * Nombre: esFinDePrograma
     * Entradas: ninguna
     * Salidas: true si es INT 20H
     * Restricciones: ninguna
     * Descripcion: permite avisar si un programa no tiene su instruccion de
     *              fin.
     */
    public boolean esFinDePrograma() {
        return getInterrupcion() == Interrupcion.FIN_PROGRAMA;
    }

    /**
     * Nombre: getTextoFuente
     * Entradas: ninguna
     * Salidas: la linea del archivo tal como fue escrita
     * Restricciones: ninguna
     * Descripcion: se muestra en la tabla de instrucciones y se guarda como
     *              etiqueta de la celda de memoria, para que el usuario vea
     *              su propio codigo y no una reconstruccion.
     */
    public String getTextoFuente() {
        return textoFuente;
    }

    /**
     * Nombre: getNumeroLinea
     * Entradas: ninguna
     * Salidas: la posicion de la instruccion dentro del archivo, desde uno
     * Restricciones: ninguna
     * Descripcion: permite que los mensajes de error apunten a la linea real
     *              del archivo, aunque haya comentarios y lineas vacias que
     *              no ocupan memoria.
     */
    public int getNumeroLinea() {
        return numeroLinea;
    }

    /**
     * Nombre: toString
     * Entradas: ninguna
     * Salidas: representacion legible de la instruccion
     * Restricciones: ninguna
     * Descripcion: devuelve el texto original si existe; si no, reconstruye
     *              la instruccion a partir de la operacion y sus operandos.
     */
    @Override
    public String toString() {
        if (textoFuente != null) {
            return textoFuente;
        }
        StringBuilder texto = new StringBuilder(opcode.name());
        for (int i = 0; i < operandos.size(); i++) {
            texto.append(i == 0 ? " " : ", ").append(operandos.get(i));
        }
        return texto.toString();
    }
}
