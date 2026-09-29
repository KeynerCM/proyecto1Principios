package com.mycompany.minipc.isa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import com.mycompany.minipc.excepciones.SintaxisException;

/**
 * Nombre: Ensamblador
 * Entradas: las lineas de texto de un archivo .asm
 * Salidas: la lista de instrucciones traducidas
 * Restricciones: no tiene estado, de modo que una misma instancia puede
 *                ensamblar varios archivos sin interferencia entre ellos
 * Descripcion: traduce el texto de un archivo .asm a instrucciones del Mini
 *              PC. Cada linea se valida con una expresion regular que debe
 *              coincidir con la linea completa: una por cada forma valida de
 *              su operacion. Entre dos operandos va exactamente una coma, de
 *              modo que lineas como "MOV AX,,, 5" o "ADD BX," se rechazan.
 *              Los operandos se toman de los grupos de la expresion regular,
 *              nunca separando el texto por comas o espacios. Recorre el
 *              archivo completo antes de fallar y junta todos los errores en
 *              una sola SintaxisException, para que la interfaz los muestre
 *              de una vez.
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
     * Descripcion: reconoce la operacion, descarta las comas mal colocadas y
     *              prueba la linea contra el patron de cada forma de la
     *              operacion. Si ninguna coincide, diagnostica la causa para
     *              dar un mensaje concreto.
     */
    private Instruccion ensamblarLinea(String linea, int numeroLinea) throws SintaxisException {
        // Las comas se revisan primero: una coma al inicio tambien impide
        // reconocer el mnemonico, y el mensaje correcto es el de la coma.
        if (Sintaxis.COMAS_MAL.matcher(linea).find()) {
            throw error(numeroLinea, "comas mal colocadas en \"" + linea + "\". Los"
                    + " operandos se separan con una sola coma, por ejemplo \"MOV AX, 5\"");
        }

        Matcher mnemonico = Sintaxis.MNEMONICO.matcher(linea);
        if (!mnemonico.find()) {
            throw error(numeroLinea, "la instruccion debe empezar con el nombre de una"
                    + " operacion: \"" + linea + "\"");
        }
        OpCode opcode = leerOpcode(mnemonico.group(1), numeroLinea);

        for (Forma forma : opcode.getFormas()) {
            Matcher coincidencia = forma.patron(opcode).matcher(linea);
            if (coincidencia.matches()) {
                return construir(opcode, forma, coincidencia, linea, numeroLinea);
            }
        }
        throw error(numeroLinea, diagnosticar(opcode, linea));
    }

    /**
     * Nombre: construir
     * Entradas: opcode, operacion reconocida; forma, forma con la que
     *           coincidio la linea; coincidencia, resultado de la expresion
     *           regular; linea, texto fuente; numeroLinea, para los mensajes
     * Salidas: la instruccion armada con los operandos de la linea
     * Restricciones: lanza SintaxisException si un registro no existe o un
     *                numero no cabe en un entero
     * Descripcion: convierte cada grupo de la expresion regular segun el
     *              tipo de operando que la forma espera en esa posicion.
     */
    private Instruccion construir(OpCode opcode, Forma forma, Matcher coincidencia,
            String linea, int numeroLinea) throws SintaxisException {
        RegistroID registro = null;
        int operando = 0;
        List<Forma.TipoOperando> tipos = forma.getOperandos();
        for (int i = 0; i < tipos.size(); i++) {
            String texto = coincidencia.group(i + 1);
            if (tipos.get(i) == Forma.TipoOperando.REGISTRO) {
                registro = leerRegistro(texto, numeroLinea);
            } else {
                operando = leerNumero(texto, numeroLinea);
            }
        }
        return new Instruccion(opcode, registro, operando, linea, numeroLinea);
    }

    /**
     * Nombre: diagnosticar
     * Entradas: opcode, operacion reconocida; linea, texto que no coincidio
     *           con ninguna forma
     * Salidas: el mensaje que explica por que la linea es invalida
     * Restricciones: solo se usa despues de que la validacion con expresiones
     *                regulares ya rechazo la linea; separar el texto aqui es
     *                solo para explicar el error, nunca para aceptarlo
     * Descripcion: revisa, en orden, si faltan operandos, si sobran, si solo
     *              falta la coma, o si algun operando no es del tipo esperado.
     *              Si nada de eso explica el fallo, dice que formas se
     *              esperaban.
     */
    private String diagnosticar(OpCode opcode, String linea) {
        String resto = linea.substring(opcode.name().length()).trim();
        List<String> operandos = resto.isEmpty() ? List.of()
                : Arrays.asList(Sintaxis.SEPARADOR_DIAGNOSTICO.split(resto));
        String esperado = ". Se esperaba " + opcode.describirFormas();

        int minimo = Integer.MAX_VALUE;
        int maximo = 0;
        for (Forma forma : opcode.getFormas()) {
            minimo = Math.min(minimo, forma.getOperandos().size());
            maximo = Math.max(maximo, forma.getOperandos().size());
        }

        if (operandos.size() < minimo) {
            if (operandos.isEmpty()
                    && opcode.getFormas().get(0).getOperandos().get(0) == Forma.TipoOperando.REGISTRO) {
                return "falta el registro para " + opcode + esperado;
            }
            return "faltan operandos para " + opcode + esperado;
        }
        if (operandos.size() > maximo) {
            return "sobran operandos para " + opcode + esperado;
        }

        for (Forma forma : opcode.getFormas()) {
            if (forma.getOperandos().size() == operandos.size()
                    && forma.patronSinComa(opcode).matcher(linea).matches()) {
                return "falta la coma entre los operandos en \"" + linea + "\"" + esperado;
            }
        }

        for (Forma forma : opcode.getFormas()) {
            if (forma.getOperandos().size() != operandos.size()) {
                continue;
            }
            for (int i = 0; i < operandos.size(); i++) {
                String texto = operandos.get(i);
                Forma.TipoOperando tipo = forma.getOperandos().get(i);
                if (tipo == Forma.TipoOperando.NUMERO
                        && !Sintaxis.ES_NUMERO.matcher(texto).matches()) {
                    return "valor no numerico \"" + texto + "\"" + esperado;
                }
                if (tipo == Forma.TipoOperando.REGISTRO
                        && !Sintaxis.ES_IDENT.matcher(texto).matches()) {
                    return "se esperaba un registro y se encontro \"" + texto + "\"" + esperado;
                }
            }
        }
        return "formato invalido: \"" + linea + "\"" + esperado;
    }

    /**
     * Nombre: leerOpcode
     * Entradas: token, mnemonico al inicio de la linea; numeroLinea, para el
     *           mensaje
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
            throw error(numeroLinea, "operacion desconocida \"" + token + "\"");
        }
    }

    /**
     * Nombre: leerRegistro
     * Entradas: texto, nombre del registro; numeroLinea, para el mensaje
     * Salidas: el registro correspondiente
     * Restricciones: lanza SintaxisException si el nombre no es un registro
     * Descripcion: la expresion regular acepta cualquier palabra en la
     *              posicion del registro; aqui se comprueba que exista, para
     *              poder nombrarla en el mensaje.
     */
    private RegistroID leerRegistro(String texto, int numeroLinea) throws SintaxisException {
        try {
            return RegistroID.desdeNombre(texto);
        } catch (IllegalArgumentException e) {
            throw error(numeroLinea, "registro inexistente \"" + texto + "\"");
        }
    }

    /**
     * Nombre: leerNumero
     * Entradas: texto, digitos con signo opcional; numeroLinea, para el mensaje
     * Salidas: el valor numerico
     * Restricciones: lanza SintaxisException si el numero no cabe en un entero
     * Descripcion: la expresion regular ya garantizo que son digitos; lo unico
     *              que puede fallar es el tamano.
     */
    private int leerNumero(String texto, int numeroLinea) throws SintaxisException {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw error(numeroLinea, "el valor " + texto + " es demasiado grande");
        }
    }

    /**
     * Nombre: error
     * Entradas: numeroLinea, linea del archivo; detalle, descripcion del
     *           problema
     * Salidas: la excepcion con el mensaje ya armado
     * Restricciones: ninguna
     * Descripcion: da a todos los mensajes el mismo formato, "Linea N: ...".
     */
    private SintaxisException error(int numeroLinea, String detalle) {
        return new SintaxisException("Linea " + numeroLinea + ": " + detalle);
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
