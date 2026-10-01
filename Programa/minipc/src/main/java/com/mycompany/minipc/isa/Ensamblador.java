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

        if (errores.isEmpty()) {
            validarSaltos(programa, errores);
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
     * Nombre: decodificar
     * Entradas: linea, texto de una posicion de memoria
     * Salidas: la instruccion que representa ese texto
     * Restricciones: lanza SintaxisException si el texto esta vacio o no es
     *                una instruccion valida, por ejemplo si es un numero
     * Descripcion: es la etapa de decodificacion de la CPU. La memoria guarda
     *              cada instruccion como texto; al traerla con el PC, la CPU
     *              la interpreta con las mismas expresiones regulares que se
     *              usaron al cargar el archivo. Como el programa ya se valido
     *              al cargarlo, solo falla si el PC apunta a algo que no es
     *              codigo.
     */
    public Instruccion decodificar(String linea) throws SintaxisException {
        String util = linea == null ? "" : quitarComentario(linea).trim();
        if (util.isEmpty()) {
            throw new SintaxisException("La posicion esta vacia, no contiene una instruccion");
        }
        return ensamblarLinea(util, 0);
    }

    /**
     * Nombre: validarSaltos
     * Entradas: programa, instrucciones ya ensambladas; errores, lista donde
     *           anotar los saltos invalidos
     * Salidas: ninguna
     * Restricciones: solo se llama si todas las lineas eran validas
     * Descripcion: comprueba que el destino de cada JMP, JE y JNE quede dentro
     *              del programa. El desplazamiento se cuenta desde la
     *              instruccion siguiente, porque el PC ya avanzo al traer la
     *              instruccion (Stallings, seccion 1.3). Salir del programa
     *              es el desbordamiento que pide controlar el enunciado; el
     *              procesador lo vuelve a revisar al ejecutar.
     */
    private void validarSaltos(List<Instruccion> programa, List<String> errores) {
        for (int i = 0; i < programa.size(); i++) {
            Instruccion instruccion = programa.get(i);
            if (!instruccion.getOpcode().esSalto()) {
                continue;
            }
            int destino = i + 1 + instruccion.getValor(0);
            if (destino < 0 || destino >= programa.size()) {
                errores.add("Linea " + instruccion.getNumeroLinea() + ": el salto \""
                        + instruccion.getTextoFuente() + "\" sale del programa: llevaria a la"
                        + " instruccion " + (destino + 1) + " y el programa tiene de 1 a "
                        + programa.size());
            }
        }
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
     * Restricciones: lanza SintaxisException si un registro o un codigo de
     *                interrupcion no existe, o si un numero no cabe en un
     *                entero
     * Descripcion: convierte cada grupo de la expresion regular segun el
     *              tipo de operando que la forma espera en esa posicion. Los
     *              operandos opcionales que no se escribieron quedan con el
     *              grupo en nulo y se omiten.
     */
    private Instruccion construir(OpCode opcode, Forma forma, Matcher coincidencia,
            String linea, int numeroLinea) throws SintaxisException {
        List<Operando> operandos = new ArrayList<>();
        List<Forma.TipoOperando> tipos = forma.getOperandos();
        for (int i = 0; i < tipos.size(); i++) {
            String texto = coincidencia.group(i + 1);
            if (texto == null) {
                break;
            }
            switch (tipos.get(i)) {
                case REGISTRO:
                    operandos.add(Operando.registro(
                            leerRegistro(opcode, i, texto, numeroLinea)));
                    break;
                case NUMERO:
                    operandos.add(Operando.numero(leerNumero(texto, numeroLinea)));
                    break;
                case DESPLAZAMIENTO:
                    operandos.add(Operando.desplazamiento(leerNumero(texto, numeroLinea)));
                    break;
                case TEXTO:
                    operandos.add(Operando.texto(texto));
                    break;
                default:
                    operandos.add(Operando.interrupcion(leerInterrupcion(texto, numeroLinea)));
                    break;
            }
        }
        return new Instruccion(opcode, forma, operandos, linea, numeroLinea);
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

        String textoMal = diagnosticarTexto(opcode, resto);
        if (textoMal != null) {
            return textoMal + esperado;
        }

        int minimo = Integer.MAX_VALUE;
        int maximo = 0;
        for (Forma forma : opcode.getFormas()) {
            minimo = Math.min(minimo, forma.getMinimo());
            maximo = Math.max(maximo, forma.getMaximo());
        }

        if (operandos.size() < minimo) {
            if (operandos.isEmpty()
                    && opcode.getFormas().get(0).getOperandos().get(0) == Forma.TipoOperando.REGISTRO) {
                return "falta el registro para " + opcode + esperado;
            }
            return "faltan operandos para " + opcode + esperado;
        }
        if (operandos.size() > maximo) {
            if (opcode == OpCode.PARAM) {
                return "PARAM admite como maximo " + maximo + " parametros y se escribieron "
                        + operandos.size();
            }
            return "sobran operandos para " + opcode + esperado;
        }

        for (Forma forma : opcode.getFormas()) {
            if (forma.admite(operandos.size())
                    && forma.patronSinComa(opcode).matcher(linea).matches()) {
                return "falta la coma entre los operandos en \"" + linea + "\"" + esperado;
            }
        }

        for (Forma forma : opcode.getFormas()) {
            if (!forma.admite(operandos.size())) {
                continue;
            }
            for (int i = 0; i < operandos.size(); i++) {
                String texto = operandos.get(i);
                switch (forma.getOperandos().get(i)) {
                    case NUMERO:
                        if (!Sintaxis.ES_NUMERO.matcher(texto).matches()) {
                            return "valor no numerico \"" + texto + "\"" + esperado;
                        }
                        break;
                    case DESPLAZAMIENTO:
                        if (!Sintaxis.ES_DESPLAZAMIENTO.matcher(texto).matches()) {
                            return "desplazamiento invalido \"" + texto + "\": debe ser un"
                                    + " entero, por ejemplo +2 o -3";
                        }
                        break;
                    case REGISTRO:
                        if (!Sintaxis.ES_IDENT.matcher(texto).matches()) {
                            return "se esperaba un registro y se encontro \"" + texto + "\""
                                    + esperado;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return "formato invalido: \"" + linea + "\"" + esperado;
    }

    /**
     * Nombre: diagnosticarTexto
     * Entradas: opcode, operacion reconocida; resto, lo que sigue al mnemonico
     * Salidas: el mensaje si el problema es un texto entre comillas, o nulo
     * Restricciones: solo explica errores; nunca acepta una linea
     * Descripcion: el diagnostico general separa por espacios y comas, lo que
     *              partiria un texto con espacios. Por eso los textos se
     *              revisan antes: comillas en una operacion que no las admite,
     *              comillas sin cerrar o caracteres no permitidos, y un nombre
     *              de archivo escrito sin comillas.
     */
    private String diagnosticarTexto(OpCode opcode, String resto) {
        boolean admiteTexto = opcode.getFormas().contains(Forma.REGISTRO_TEXTO);
        if (resto.contains("\"")) {
            if (!admiteTexto) {
                return "la operacion " + opcode + " no admite texto entre comillas";
            }
            if (resto.chars().filter(c -> c == '"').count() != 2) {
                return "el texto debe abrir y cerrar comillas: " + resto;
            }
            return "texto invalido " + resto.substring(resto.indexOf('"')) + ": entre las"
                    + " comillas solo se admiten letras, digitos, espacios, punto, guion y"
                    + " parentesis";
        }
        if (admiteTexto && resto.matches("(?i)\\s*[A-Za-z]\\w*\\s*,\\s*[\\w-]+\\.\\w+\\s*")) {
            return "el nombre de archivo va entre comillas, por ejemplo MOV DX, \"datos.txt\"";
        }
        return null;
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
     * Entradas: opcode, operacion de la linea; posicion, indice del operando;
     *           texto, nombre del registro; numeroLinea, para el mensaje
     * Salidas: el registro correspondiente
     * Restricciones: lanza SintaxisException si el nombre no es un registro
     * Descripcion: la expresion regular acepta cualquier palabra en la
     *              posicion del registro; aqui se comprueba que exista, para
     *              poder nombrarla en el mensaje. Si en esa posicion la
     *              operacion tambien admite un numero, como el segundo
     *              operando de MOV, el mensaje lo dice.
     */
    private RegistroID leerRegistro(OpCode opcode, int posicion, String texto, int numeroLinea)
            throws SintaxisException {
        try {
            return RegistroID.desdeNombre(texto);
        } catch (IllegalArgumentException e) {
            for (Forma forma : opcode.getFormas()) {
                if (forma.getMaximo() > posicion
                        && forma.getOperandos().get(posicion) == Forma.TipoOperando.NUMERO) {
                    throw error(numeroLinea, "\"" + texto + "\" no es un registro ni un numero."
                            + " Se esperaba " + opcode.describirFormas());
                }
            }
            throw error(numeroLinea, "registro inexistente \"" + texto + "\"");
        }
    }

    /**
     * Nombre: leerInterrupcion
     * Entradas: texto, codigo escrito despues de INT; numeroLinea, para el
     *           mensaje
     * Salidas: la interrupcion correspondiente
     * Restricciones: lanza SintaxisException si el codigo no existe
     * Descripcion: igual que con los registros, la expresion regular acepta
     *              cualquier palabra y aqui se valida, para poder listar los
     *              codigos validos en el mensaje.
     */
    private Interrupcion leerInterrupcion(String texto, int numeroLinea)
            throws SintaxisException {
        try {
            return Interrupcion.desdeCodigo(texto);
        } catch (IllegalArgumentException e) {
            throw error(numeroLinea, "codigo de interrupcion desconocido \"" + texto
                    + "\". Los validos son " + Interrupcion.codigosValidos());
        }
    }

    /**
     * Nombre: leerNumero
     * Entradas: texto, entero decimal con signo opcional o hexadecimal con
     *           sufijo h; numeroLinea, para el mensaje
     * Salidas: el valor numerico
     * Restricciones: lanza SintaxisException si el numero no cabe en un entero
     * Descripcion: la expresion regular ya garantizo el formato; lo unico que
     *              puede fallar es el tamano. "3Ch" vale 60 y "40h" vale 64.
     */
    private int leerNumero(String texto, int numeroLinea) throws SintaxisException {
        try {
            char ultimo = texto.charAt(texto.length() - 1);
            if (ultimo == 'h' || ultimo == 'H') {
                return Integer.parseInt(texto.substring(0, texto.length() - 1), 16);
            }
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
