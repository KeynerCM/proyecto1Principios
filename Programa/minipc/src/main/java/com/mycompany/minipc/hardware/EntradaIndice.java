package com.mycompany.minipc.hardware;

/**
 * Nombre: EntradaIndice
 * Entradas: el nombre del archivo, la direccion donde empieza y su tamano
 * Salidas: no aplica
 * Restricciones: es inmutable; el nombre no puede ser vacio ni contener el
 *                separador, la direccion no puede ser negativa y el tamano
 *                debe ser de al menos uno
 * Descripcion: una fila del indice de archivos del disco: el nombre del
 *              archivo, la direccion donde empieza y su tamano, para saber
 *              cuantas posiciones leer sin tener que recorrer el disco. En el
 *              disco la fila se guarda como texto, por ejemplo "file.asm|20|7";
 *              esta clase solo sirve para leer y armar ese texto, no guarda
 *              nada por su cuenta.
 */
public final class EntradaIndice {

    /**
     * Separa los tres campos en la celda del indice. Se usa la barra
     * vertical porque Windows no la admite en nombres de archivo.
     */
    public static final String SEPARADOR = "|";

    private final String nombre;
    private final int direccionInicio;
    private final int tamano;

    /**
     * Nombre: EntradaIndice
     * Entradas: nombre, nombre del archivo; direccionInicio, primera posicion
     *           que ocupa en el disco; tamano, cantidad de posiciones
     * Salidas: la entrada construida
     * Restricciones: lanza IllegalArgumentException si algun valor es invalido
     * Descripcion: valida los tres campos de una vez, de modo que una entrada
     *              que existe siempre describe un archivo coherente.
     */
    public EntradaIndice(String nombre, int direccionInicio, int tamano) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo es obligatorio");
        }
        if (nombre.contains(SEPARADOR)) {
            throw new IllegalArgumentException("El nombre del archivo no puede contener \""
                    + SEPARADOR + "\": " + nombre);
        }
        if (direccionInicio < 0) {
            throw new IllegalArgumentException("La direccion de inicio no puede ser negativa");
        }
        if (tamano < 1) {
            throw new IllegalArgumentException("El tamano del archivo debe ser de al menos 1");
        }
        this.nombre = nombre;
        this.direccionInicio = direccionInicio;
        this.tamano = tamano;
    }

    /**
     * Nombre: desdeTexto
     * Entradas: texto, contenido de una celda del indice
     * Salidas: la entrada que describe el texto, o nulo si la celda esta vacia
     * Restricciones: lanza IllegalArgumentException si el texto no tiene el
     *                formato "nombre|inicio|tamano"
     * Descripcion: interpreta la celda del disco. Los dos numeros se
     *              convierten a int para poder recorrer el archivo.
     */
    public static EntradaIndice desdeTexto(String texto) {
        if (texto == null || texto.isEmpty()) {
            return null;
        }
        String[] partes = texto.split("\\" + SEPARADOR);
        if (partes.length != 3) {
            throw new IllegalArgumentException("Entrada del indice mal formada: \"" + texto + "\"");
        }
        try {
            return new EntradaIndice(partes[0], Integer.parseInt(partes[1]),
                    Integer.parseInt(partes[2]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Entrada del indice mal formada: \"" + texto + "\"",
                    e);
        }
    }

    /**
     * Nombre: aTexto
     * Entradas: ninguna
     * Salidas: la entrada tal como se guarda en la celda, por ejemplo
     *          "file.asm|20|7"
     * Restricciones: ninguna
     * Descripcion: es la operacion inversa de desdeTexto.
     */
    public String aTexto() {
        return nombre + SEPARADOR + direccionInicio + SEPARADOR + tamano;
    }

    /**
     * Nombre: getNombre
     * Entradas: ninguna
     * Salidas: el nombre del archivo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Nombre: getDireccionInicio
     * Entradas: ninguna
     * Salidas: la primera posicion del disco que ocupa el archivo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getDireccionInicio() {
        return direccionInicio;
    }

    /**
     * Nombre: getTamano
     * Entradas: ninguna
     * Salidas: cuantas posiciones ocupa el archivo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getTamano() {
        return tamano;
    }

    /**
     * Nombre: getDireccionFin
     * Entradas: ninguna
     * Salidas: la ultima posicion del disco que ocupa el archivo
     * Restricciones: ninguna
     * Descripcion: evita repetir el calculo inicio + tamano - 1 en cada lugar
     *              que necesita recorrer el archivo.
     */
    public int getDireccionFin() {
        return direccionInicio + tamano - 1;
    }

    /**
     * Nombre: equals
     * Entradas: otro, objeto a comparar
     * Salidas: true si describe el mismo archivo en el mismo lugar
     * Restricciones: ninguna
     * Descripcion: como la entrada se vuelve a armar cada vez que se lee del
     *              disco, dos lecturas de la misma celda deben ser iguales.
     */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof EntradaIndice)) {
            return false;
        }
        EntradaIndice entrada = (EntradaIndice) otro;
        return nombre.equals(entrada.nombre) && direccionInicio == entrada.direccionInicio
                && tamano == entrada.tamano;
    }

    /**
     * Nombre: hashCode
     * Entradas: ninguna
     * Salidas: el codigo hash coherente con equals
     * Restricciones: ninguna
     * Descripcion: necesario al redefinir equals.
     */
    @Override
    public int hashCode() {
        return aTexto().hashCode();
    }

    /**
     * Nombre: toString
     * Entradas: ninguna
     * Salidas: la entrada en texto, por ejemplo "file.asm -> 20 (7)"
     * Restricciones: ninguna
     * Descripcion: forma legible para los mensajes de la consola.
     */
    @Override
    public String toString() {
        return nombre + " -> " + direccionInicio + " (" + tamano + ")";
    }
}
