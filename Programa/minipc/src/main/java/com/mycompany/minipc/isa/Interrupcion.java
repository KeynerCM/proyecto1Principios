package com.mycompany.minipc.isa;

/**
 * Nombre: Interrupcion
 * Entradas: no aplica, es una enumeracion de valores fijos
 * Salidas: no aplica
 * Restricciones: los codigos y pesos son los del enunciado
 * Descripcion: los servicios que un programa puede pedir al sistema operativo
 *              con la instruccion INT. Cada uno tiene su propio peso, es
 *              decir, los segundos de CPU que consume. La entrada de teclado
 *              no tiene un peso fijo: dura hasta que el usuario escribe el
 *              valor y presiona ENTER.
 */
public enum Interrupcion {

    /** INT 20H: finaliza el programa. */
    FIN_PROGRAMA("20H", 2),

    /** INT 10H: imprime en pantalla el valor de DX. */
    PANTALLA("10H", 2),

    /** INT 09H: lee del teclado un numero de 0 a 255 y lo guarda en DX. */
    TECLADO("09H", Interrupcion.PESO_VARIABLE),

    /** INT 21H: manejo de archivos segun el valor de AH. */
    ARCHIVOS("21H", 5);

    /** Marca de peso que no es fijo sino que depende de la entrada del usuario. */
    public static final int PESO_VARIABLE = -1;

    private final String codigo;
    private final int peso;

    /**
     * Nombre: Interrupcion
     * Entradas: codigo, como se escribe en el .asm; peso, segundos de CPU
     * Salidas: la constante construida
     * Restricciones: privado, solo lo invoca la propia enumeracion
     * Descripcion: asocia a cada interrupcion su codigo y su peso.
     */
    Interrupcion(String codigo, int peso) {
        this.codigo = codigo;
        this.peso = peso;
    }

    /**
     * Nombre: getCodigo
     * Entradas: ninguna
     * Salidas: el codigo tal como se escribe, por ejemplo "21H"
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public String getCodigo() {
        return codigo;
    }

    /**
     * Nombre: getPeso
     * Entradas: ninguna
     * Salidas: los segundos de CPU que consume, o PESO_VARIABLE
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getPeso() {
        return peso;
    }

    /**
     * Nombre: codigosValidos
     * Entradas: ninguna
     * Salidas: los codigos admitidos, por ejemplo "20H, 10H, 09H, 21H"
     * Restricciones: ninguna
     * Descripcion: se usa en el mensaje de error de un codigo desconocido.
     */
    public static String codigosValidos() {
        StringBuilder texto = new StringBuilder();
        for (Interrupcion interrupcion : values()) {
            if (texto.length() > 0) {
                texto.append(", ");
            }
            texto.append(interrupcion.codigo);
        }
        return texto.toString();
    }

    /**
     * Nombre: desdeCodigo
     * Entradas: codigo, texto escrito despues de INT
     * Salidas: la interrupcion correspondiente
     * Restricciones: lanza IllegalArgumentException si el codigo no existe;
     *                no distingue mayusculas
     * Descripcion: el ensamblador la usa para traducir "INT 21h" o "INT 21H".
     */
    public static Interrupcion desdeCodigo(String codigo) {
        for (Interrupcion interrupcion : values()) {
            if (interrupcion.codigo.equalsIgnoreCase(codigo)) {
                return interrupcion;
            }
        }
        throw new IllegalArgumentException("Codigo de interrupcion desconocido: " + codigo);
    }
}
