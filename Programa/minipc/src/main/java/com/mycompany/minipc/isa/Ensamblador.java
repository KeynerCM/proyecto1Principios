package com.mycompany.minipc.isa;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.minipc.excepciones.SintaxisException;

/**
 * Nombre: Ensamblador
 * Entradas: las lineas de texto de un archivo .asm
 * Salidas: la lista de instrucciones traducidas
 * Restricciones: no tiene estado, de modo que una misma instancia puede
 *                ensamblar varios archivos sin interferencia entre ellos
 * Descripcion: traduce el texto de un archivo .asm a instrucciones del Mini
 *              PC. Es un ensamblador de una sola pasada: el juego de
 *              instrucciones no tiene saltos ni etiquetas, asi que no hay
 *              referencias hacia adelante que resolver. Recorre el archivo
 *              completo antes de fallar y junta todos los errores en una
 *              sola SintaxisException, para que la interfaz los muestre de
 *              una vez.
 */
public class Ensamblador {

    private static final String COMENTARIO_PUNTO_COMA = ";";

    private static final String COMENTARIO_BARRAS = "//";

    /**
     * Nombre: ensamblar
     * Entradas: lineas, contenido del archivo con una entrada por linea
     * Salidas: las instrucciones traducidas, en el orden del archivo
     * Restricciones: lanza SintaxisException si alguna linea tiene errores o
     *                si el archivo no contiene ninguna instruccion; en ese
     *                caso no se devuelve nada parcial
     * Descripcion: ensambla un archivo completo. Las lineas vacias y las de
     *              solo comentario se descartan porque no ocupan posicion en
     *              memoria, pero el numero de linea original se conserva para
     *              que los errores apunten al lugar correcto del archivo.
     */
    public List<Instruccion> ensamblar(List<String> lineas) throws SintaxisException {
        List<Instruccion> programa = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        for (int i = 0; i < lineas.size(); i++) {
            int numeroLinea = i + 1;
            String util = quitarComentario(lineas.get(i)).trim();
            if (util.isEmpty()) {
                continue;
            }
            try {
                programa.add(ensamblarLinea(util, numeroLinea));
            } catch (SintaxisException e) {
                errores.addAll(e.getErrores());
            }
        }

        if (!errores.isEmpty()) {
            throw new SintaxisException(errores);
        }
        if (programa.isEmpty()) {
            throw new SintaxisException("El archivo no contiene ninguna instruccion");
        }
        return programa;
    }

    /**
     * Nombre: ensamblarLinea
     * Entradas: linea, texto ya limpio de comentarios y espacios sobrantes;
     *           numeroLinea, posicion dentro del archivo contando desde uno
     * Salidas: la instruccion correspondiente a esa linea
     * Restricciones: lanza SintaxisException ante cualquier problema de
     *                formato; la linea no debe venir vacia
     * Descripcion: separa la linea en tokens tratando la coma como espacio,
     *              de modo que "MOV AX, 5" y "MOV AX 5" son equivalentes, y
     *              luego lee operacion, registro y operando en ese orden.
     */
    private Instruccion ensamblarLinea(String linea, int numeroLinea) throws SintaxisException {
        String[] tokens = linea.replace(',', ' ').trim().split("\\s+");

        OpCode opcode = leerOpcode(tokens[0], numeroLinea);
        RegistroID registro = leerRegistro(tokens, numeroLinea, opcode);
        int operando = leerOperando(tokens, numeroLinea, opcode);

        return new Instruccion(opcode, registro, operando, linea, numeroLinea);
    }

    /**
     * Nombre: leerOpcode
     * Entradas: token, primer elemento de la linea; numeroLinea, para el mensaje
     * Salidas: la operacion reconocida
     * Restricciones: lanza SintaxisException si el mnemonico no existe
     * Descripcion: traduce el fallo tecnico de OpCode.desdeMnemonico en un
     *              mensaje con numero de linea, que es lo que el usuario
     *              necesita para corregir su archivo.
     */
    private OpCode leerOpcode(String token, int numeroLinea) throws SintaxisException {
        try {
            return OpCode.desdeMnemonico(token);
        } catch (IllegalArgumentException e) {
            throw new SintaxisException(
                    "Linea " + numeroLinea + ": operacion desconocida \"" + token + "\"");
        }
    }

    /**
     * Nombre: leerRegistro
     * Entradas: tokens, elementos de la linea; numeroLinea, para el mensaje;
     *           opcode, operacion ya reconocida
     * Salidas: el registro sobre el que opera la instruccion
     * Restricciones: lanza SintaxisException si falta el registro o si el
     *                nombre indicado no existe
     * Descripcion: toma el segundo token y lo traduce, informando con numero
     *              de linea tanto la ausencia como el nombre invalido.
     */
    private RegistroID leerRegistro(String[] tokens, int numeroLinea, OpCode opcode)
            throws SintaxisException {
        if (tokens.length < 2) {
            throw new SintaxisException(
                    "Linea " + numeroLinea + ": falta el registro para " + opcode);
        }
        try {
            return RegistroID.desdeNombre(tokens[1]);
        } catch (IllegalArgumentException e) {
            throw new SintaxisException(
                    "Linea " + numeroLinea + ": registro inexistente \"" + tokens[1] + "\"");
        }
    }

    /**
     * Nombre: leerOperando
     * Entradas: tokens, elementos de la linea; numeroLinea, para el mensaje;
     *           opcode, operacion ya reconocida
     * Salidas: el valor inmediato, o cero si la operacion no lleva
     * Restricciones: lanza SintaxisException si sobran operandos, si falta el
     *                inmediato que MOV exige o si el valor no es numerico
     * Descripcion: concentra las tres formas en que el operando puede estar
     *              mal escrito, usando requiereInmediato para saber cuantos
     *              tokens corresponden a cada operacion.
     */
    private int leerOperando(String[] tokens, int numeroLinea, OpCode opcode)
            throws SintaxisException {
        int esperados = opcode.requiereInmediato() ? 3 : 2;

        if (tokens.length > esperados) {
            throw new SintaxisException("Linea " + numeroLinea + ": sobran operandos para "
                    + opcode + ", se esperaban " + esperados + " elementos");
        }
        if (!opcode.requiereInmediato()) {
            return 0;
        }
        if (tokens.length < 3) {
            throw new SintaxisException(
                    "Linea " + numeroLinea + ": falta el valor inmediato para " + opcode);
        }

        try {
            return Integer.parseInt(tokens[2]);
        } catch (NumberFormatException e) {
            throw new SintaxisException(
                    "Linea " + numeroLinea + ": valor no numerico \"" + tokens[2] + "\"");
        }
    }

    /**
     * Nombre: quitarComentario
     * Entradas: linea, texto original del archivo
     * Salidas: la parte util de la linea, que puede quedar vacia
     * Restricciones: la linea no debe ser nula
     * Descripcion: recorta la linea en la primera marca de comentario que
     *              aparezca, sea punto y coma o doble barra, quedandose con
     *              la que ocurra antes.
     */
    private String quitarComentario(String linea) {
        int corte = linea.length();
        int puntoComa = linea.indexOf(COMENTARIO_PUNTO_COMA);
        int barras = linea.indexOf(COMENTARIO_BARRAS);
        if (puntoComa >= 0) {
            corte = Math.min(corte, puntoComa);
        }
        if (barras >= 0) {
            corte = Math.min(corte, barras);
        }
        return linea.substring(0, corte);
    }
}
