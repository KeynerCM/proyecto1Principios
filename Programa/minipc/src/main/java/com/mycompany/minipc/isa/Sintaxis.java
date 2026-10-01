package com.mycompany.minipc.isa;

import java.util.regex.Pattern;

/**
 * Nombre: Sintaxis
 * Entradas: no aplica, solo expone constantes
 * Salidas: no aplica
 * Restricciones: no se instancia
 * Descripcion: piezas de expresiones regulares con que se arman los patrones
 *              de cada forma de instruccion. Estan en un solo lugar para que
 *              todas las formas usen exactamente la misma definicion de
 *              registro, numero y separador. La regla central es SEP: entre
 *              dos operandos va exactamente una coma, con espacios opcionales
 *              alrededor, de modo que "MOV AX,,, 5" nunca coincide.
 */
final class Sintaxis {

    /** Al menos un espacio, entre el mnemonico y el primer operando. */
    static final String ESP = "\\s+";

    /** Exactamente una coma entre operandos, con espacios opcionales. */
    static final String SEP = "\\s*,\\s*";

    /**
     * Un nombre que puede ser un registro. Se acepta cualquier palabra y el
     * nombre se valida despues, para poder decir "registro inexistente EX"
     * en lugar de un generico "formato invalido".
     */
    static final String IDENT = "([A-Za-z]\\w*)";

    /**
     * Un valor inmediato: un entero decimal con signo opcional, o un
     * hexadecimal con sufijo h, como en el x86 ("3Ch", "40h"). El hexadecimal
     * empieza con un digito para no confundirse con un nombre de registro.
     */
    static final String NUM = "([+-]?\\d+|\\d[0-9A-Fa-f]*[hH])";

    /** Un desplazamiento de salto: entero decimal con signo opcional. */
    static final String DESP = "([+-]?\\d+)";

    /**
     * Un texto entre comillas, como el nombre de un archivo. Solo admite
     * letras, digitos, espacios, punto, guion, guion bajo y parentesis, para
     * que una coma o un punto y coma dentro del texto no se confundan con un
     * separador o un comentario.
     */
    static final String TEXTO = "\"([\\w .()-]+)\"";

    /**
     * Un codigo de interrupcion. Se acepta cualquier palabra y el codigo se
     * valida despues, para poder decir cuales son los codigos validos.
     */
    static final String CODIGO = "(\\w+)";

    /** Un numero cualquiera, para reconocer operandos en el diagnostico. */
    static final Pattern ES_NUMERO = Pattern.compile("[+-]?\\d+|\\d[0-9A-Fa-f]*[hH]");

    /** Un desplazamiento cualquiera, para el diagnostico. */
    static final Pattern ES_DESPLAZAMIENTO = Pattern.compile("[+-]?\\d+");

    /** Un nombre cualquiera, para reconocer operandos en el diagnostico. */
    static final Pattern ES_IDENT = Pattern.compile("[A-Za-z]\\w*");

    /** El mnemonico al inicio de la linea. */
    static final Pattern MNEMONICO = Pattern.compile("^([A-Za-z]+)");

    /**
     * Comas mal colocadas: dos seguidas (con o sin espacios entre ellas),
     * una al inicio, una al final, o una pegada al mnemonico antes del
     * primer operando.
     */
    static final Pattern COMAS_MAL = Pattern.compile(",\\s*,|^\\s*,|,\\s*$|^[A-Za-z]+\\s*,");

    /** Separadores entre operandos, solo para contarlos en el diagnostico. */
    static final Pattern SEPARADOR_DIAGNOSTICO = Pattern.compile("\\s*,\\s*|\\s+");

    private Sintaxis() {
    }
}
