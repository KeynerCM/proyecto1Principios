package com.mycompany.minipc.hardware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mycompany.minipc.excepciones.DiscoException;

/**
 * Nombre: Disco
 * Entradas: el tamano total y el tamano del area de memoria virtual
 * Salidas: no aplica
 * Restricciones: el tamano minimo es 64 posiciones; el area de archivos debe
 *                tener al menos una posicion despues de reservar el indice y
 *                la memoria virtual
 * Descripcion: almacenamiento secundario del Mini PC, dividido en tres zonas:
 *
 *                0 .. 19                  indice de archivos
 *                20 .. inicioVirtual - 1  archivos (programas .asm)
 *                inicioVirtual .. fin     memoria virtual (intercambio)
 *
 *              Igual que la memoria, el disco es un arreglo de texto. El
 *              indice vive en las primeras posiciones del propio disco, como
 *              pide el enunciado: cada entrada ocupa una celda con el texto
 *              "nombre|inicio|tamano". No hay una copia aparte del indice;
 *              buscar un archivo es recorrer esas celdas. Los archivos se
 *              guardan de forma contigua, una linea por posicion, con la
 *              politica de primer ajuste.
 */
public class Disco {

    /** Tamano con el que arranca la aplicacion. */
    public static final int TAMANO_POR_DEFECTO = 512;

    /** Tamano del area de memoria virtual con el que arranca la aplicacion. */
    public static final int MEMORIA_VIRTUAL_POR_DEFECTO = 64;

    /** Tamano minimo admitido para el disco. */
    public static final int TAMANO_MINIMO = 64;

    /** Cantidad de posiciones reservadas al indice, una por archivo. */
    public static final int ENTRADAS_INDICE = 20;

    /** Contenido de una posicion libre. */
    public static final String VACIA = "";

    private String[] celdas;
    private int tamano;
    private int tamanoMemoriaVirtual;

    /**
     * Nombre: Disco
     * Entradas: ninguna
     * Salidas: el disco construido con la configuracion por defecto
     * Restricciones: ninguna
     * Descripcion: crea un disco de 512 posiciones con 64 de memoria virtual,
     *              que son los valores por defecto del enunciado.
     */
    public Disco() {
        this(TAMANO_POR_DEFECTO, MEMORIA_VIRTUAL_POR_DEFECTO);
    }

    /**
     * Nombre: Disco
     * Entradas: tamano, cantidad total de posiciones; tamanoMemoriaVirtual,
     *           posiciones reservadas al final para intercambio
     * Salidas: el disco construido y vacio
     * Restricciones: lanza IllegalArgumentException si la combinacion no es
     *                valida (ver redimensionar)
     * Descripcion: crea un disco con la configuracion indicada.
     */
    public Disco(int tamano, int tamanoMemoriaVirtual) {
        redimensionar(tamano, tamanoMemoriaVirtual);
    }

    /**
     * Nombre: validar
     * Entradas: tamano, cantidad total de posiciones; tamanoMemoriaVirtual,
     *           posiciones reservadas para intercambio
     * Salidas: la lista de problemas encontrados, vacia si todo es valido
     * Restricciones: ninguna, no lanza excepciones
     * Descripcion: concentra las reglas de tamano del disco en un solo lugar.
     *              La usa redimensionar y tambien la configuracion, que
     *              necesita reportar los errores sin construir el disco.
     */
    public static List<String> validar(int tamano, int tamanoMemoriaVirtual) {
        List<String> errores = new ArrayList<>();
        if (tamano < TAMANO_MINIMO) {
            errores.add("El tamano del disco debe ser de al menos " + TAMANO_MINIMO
                    + ", se recibio " + tamano);
        }
        if (tamanoMemoriaVirtual < 0) {
            errores.add("La memoria virtual no puede ser negativa, se recibio "
                    + tamanoMemoriaVirtual);
        } else if (tamano - ENTRADAS_INDICE - tamanoMemoriaVirtual < 1) {
            errores.add("La memoria virtual (" + tamanoMemoriaVirtual + ") no deja espacio"
                    + " para archivos: el disco de " + tamano + " posiciones reserva "
                    + ENTRADAS_INDICE + " para el indice");
        }
        return errores;
    }

    /**
     * Nombre: redimensionar
     * Entradas: tamano, cantidad total de posiciones; tamanoMemoriaVirtual,
     *           posiciones reservadas para intercambio
     * Salidas: ninguna
     * Restricciones: lanza IllegalArgumentException si la combinacion no es
     *                valida; descarta todo el contenido anterior
     * Descripcion: reconstruye el arreglo de celdas con el disco vacio. Es
     *              final porque el constructor la invoca.
     */
    public final void redimensionar(int tamano, int tamanoMemoriaVirtual) {
        List<String> errores = validar(tamano, tamanoMemoriaVirtual);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errores));
        }
        this.tamano = tamano;
        this.tamanoMemoriaVirtual = tamanoMemoriaVirtual;
        this.celdas = new String[tamano];
        Arrays.fill(celdas, VACIA);
    }

    /**
     * Nombre: getTamano
     * Entradas: ninguna
     * Salidas: cantidad total de posiciones del disco
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura a la configuracion actual.
     */
    public int getTamano() {
        return tamano;
    }

    /**
     * Nombre: getTamanoMemoriaVirtual
     * Entradas: ninguna
     * Salidas: cuantas posiciones se reservan para intercambio
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura a la configuracion actual.
     */
    public int getTamanoMemoriaVirtual() {
        return tamanoMemoriaVirtual;
    }

    /**
     * Nombre: getInicioArchivos
     * Entradas: ninguna
     * Salidas: la primera posicion del area de archivos
     * Restricciones: ninguna
     * Descripcion: es la posicion siguiente al indice.
     */
    public int getInicioArchivos() {
        return ENTRADAS_INDICE;
    }

    /**
     * Nombre: getInicioMemoriaVirtual
     * Entradas: ninguna
     * Salidas: la primera posicion del area de memoria virtual
     * Restricciones: si la memoria virtual es cero, coincide con el tamano
     * Descripcion: la memoria virtual ocupa las ultimas posiciones del disco.
     */
    public int getInicioMemoriaVirtual() {
        return tamano - tamanoMemoriaVirtual;
    }

    /**
     * Nombre: getEspacioArchivos
     * Entradas: ninguna
     * Salidas: cuantas posiciones tiene el area de archivos
     * Restricciones: ninguna
     * Descripcion: determina el archivo mas grande que se puede guardar.
     */
    public int getEspacioArchivos() {
        return getInicioMemoriaVirtual() - getInicioArchivos();
    }

    /**
     * Nombre: esDireccionIndice
     * Entradas: direccion, posicion a evaluar
     * Salidas: true si pertenece al indice de archivos
     * Restricciones: una direccion negativa devuelve false, no falla
     * Descripcion: lo consulta el renderer de la tabla del disco para elegir
     *              el color.
     */
    public boolean esDireccionIndice(int direccion) {
        return direccion >= 0 && direccion < ENTRADAS_INDICE;
    }

    /**
     * Nombre: esDireccionMemoriaVirtual
     * Entradas: direccion, posicion a evaluar
     * Salidas: true si pertenece al area de memoria virtual
     * Restricciones: una direccion fuera del disco devuelve false, no falla
     * Descripcion: analogo a esDireccionIndice, para la otra zona reservada.
     */
    public boolean esDireccionMemoriaVirtual(int direccion) {
        return direccion >= getInicioMemoriaVirtual() && direccion < tamano;
    }

    /**
     * Nombre: guardarPrograma
     * Entradas: nombre, nombre del archivo; lineas, texto de cada instruccion
     *           ya validada, en orden
     * Salidas: la entrada del indice creada para el archivo
     * Restricciones: lanza DiscoException si ya existe un archivo con ese
     *                nombre, si el indice esta lleno o si no hay un bloque
     *                contiguo suficiente; en esos casos el disco no cambia.
     *                Lanza IllegalArgumentException si el nombre es vacio o
     *                el programa no tiene instrucciones
     * Descripcion: guarda el programa en el primer bloque contiguo libre del
     *              area de archivos que lo contenga (primer ajuste), una linea
     *              por posicion, y anota la entrada en la primera celda libre
     *              del indice. Primero se valida todo y solo entonces se
     *              escribe, de modo que la operacion es atomica.
     */
    public EntradaIndice guardarPrograma(String nombre, List<String> lineas)
            throws DiscoException {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo es obligatorio");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("El programa no tiene instrucciones");
        }
        if (buscar(nombre) != null) {
            throw new DiscoException("Ya existe un archivo llamado \"" + nombre
                    + "\" en el disco");
        }
        int ranura = primeraRanuraLibreDelIndice();
        if (ranura < 0) {
            throw new DiscoException("No se puede guardar \"" + nombre + "\": el indice del"
                    + " disco esta lleno (" + ENTRADAS_INDICE + " archivos)");
        }
        int inicio = buscarBloqueLibre(lineas.size());
        if (inicio < 0) {
            throw new DiscoException("No se puede guardar \"" + nombre + "\": requiere "
                    + lineas.size() + " posiciones contiguas y el bloque libre mas grande"
                    + " del disco es de " + mayorBloqueLibre());
        }

        EntradaIndice entrada = new EntradaIndice(nombre, inicio, lineas.size());
        for (int i = 0; i < lineas.size(); i++) {
            celdas[inicio + i] = lineas.get(i);
        }
        celdas[ranura] = entrada.aTexto();
        return entrada;
    }

    /**
     * Nombre: buscar
     * Entradas: nombre, nombre del archivo a buscar
     * Salidas: la entrada del indice, o nulo si no existe
     * Restricciones: la comparacion ignora mayusculas, como en el sistema de
     *                archivos de Windows
     * Descripcion: recorre las celdas del indice en el disco.
     */
    public EntradaIndice buscar(String nombre) {
        int ranura = ranuraDe(nombre);
        return ranura < 0 ? null : EntradaIndice.desdeTexto(celdas[ranura]);
    }

    /**
     * Nombre: nombreDisponible
     * Entradas: nombre, nombre deseado para un archivo
     * Salidas: el mismo nombre si esta libre, o una variante numerada, por
     *          ejemplo "file (2).asm"
     * Restricciones: el nombre no debe ser nulo
     * Descripcion: permite guardar varias copias del mismo programa, que es
     *              lo normal cuando se quiere ejecutar mas de un proceso con
     *              el mismo codigo. El numero se inserta antes de la
     *              extension para que el archivo conserve su tipo.
     */
    public String nombreDisponible(String nombre) {
        if (buscar(nombre) == null) {
            return nombre;
        }
        int punto = nombre.lastIndexOf('.');
        String base = punto > 0 ? nombre.substring(0, punto) : nombre;
        String extension = punto > 0 ? nombre.substring(punto) : "";
        int copia = 2;
        while (buscar(base + " (" + copia + ")" + extension) != null) {
            copia++;
        }
        return base + " (" + copia + ")" + extension;
    }

    /**
     * Nombre: getIndice
     * Entradas: ninguna
     * Salidas: las entradas del indice, en el orden en que aparecen en el disco
     * Restricciones: la lista es una copia; modificarla no altera el disco
     * Descripcion: permite recorrer todos los archivos guardados, por ejemplo
     *              para armar la lista de trabajos.
     */
    public List<EntradaIndice> getIndice() {
        List<EntradaIndice> indice = new ArrayList<>();
        for (int i = 0; i < ENTRADAS_INDICE; i++) {
            if (!celdas[i].isEmpty()) {
                indice.add(EntradaIndice.desdeTexto(celdas[i]));
            }
        }
        return indice;
    }

    /**
     * Nombre: leerPrograma
     * Entradas: nombre, nombre del archivo a leer
     * Salidas: el texto de cada instruccion del programa, en orden
     * Restricciones: lanza DiscoException si el archivo no existe
     * Descripcion: localiza el archivo en el indice y lee sus posiciones. Es
     *              lo que usa el sistema operativo para pasar un programa del
     *              disco a la memoria principal.
     */
    public List<String> leerPrograma(String nombre) throws DiscoException {
        EntradaIndice entrada = buscarObligatorio(nombre);
        List<String> lineas = new ArrayList<>();
        for (int i = entrada.getDireccionInicio(); i <= entrada.getDireccionFin(); i++) {
            lineas.add(celdas[i]);
        }
        return lineas;
    }

    /**
     * Nombre: eliminar
     * Entradas: nombre, nombre del archivo a eliminar
     * Salidas: ninguna
     * Restricciones: lanza DiscoException si el archivo no existe
     * Descripcion: libera las posiciones del archivo y su celda del indice.
     */
    public void eliminar(String nombre) throws DiscoException {
        EntradaIndice entrada = buscarObligatorio(nombre);
        Arrays.fill(celdas, entrada.getDireccionInicio(), entrada.getDireccionFin() + 1, VACIA);
        celdas[ranuraDe(nombre)] = VACIA;
    }

    /**
     * Nombre: formatear
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: borra todos los archivos y el indice
     * Descripcion: deja el disco vacio sin cambiar su configuracion.
     */
    public void formatear() {
        Arrays.fill(celdas, VACIA);
    }

    /**
     * Nombre: leer
     * Entradas: direccion, posicion a leer
     * Salidas: el texto guardado en esa posicion, vacio si esta libre
     * Restricciones: lanza IndexOutOfBoundsException si la direccion no existe
     * Descripcion: acceso directo a una celda, pensado para la tabla del
     *              disco en la interfaz.
     */
    public String leer(int direccion) {
        if (direccion < 0 || direccion >= tamano) {
            throw new IndexOutOfBoundsException("Direccion fuera del disco: " + direccion
                    + ", el rango valido es 0 a " + (tamano - 1));
        }
        return celdas[direccion];
    }

    /**
     * Nombre: estaLibre
     * Entradas: direccion, posicion a consultar
     * Salidas: true si la posicion no guarda nada
     * Restricciones: lanza IndexOutOfBoundsException si la direccion no existe
     * Descripcion: una posicion esta libre cuando su texto es vacio.
     */
    public boolean estaLibre(int direccion) {
        return leer(direccion).isEmpty();
    }

    /**
     * Nombre: getPosicionesLibres
     * Entradas: ninguna
     * Salidas: cuantas posiciones libres tiene el area de archivos
     * Restricciones: no garantiza que esten contiguas
     * Descripcion: sirve para mostrar la ocupacion del disco.
     */
    public int getPosicionesLibres() {
        int libres = 0;
        for (int i = getInicioArchivos(); i < getInicioMemoriaVirtual(); i++) {
            if (celdas[i].isEmpty()) {
                libres++;
            }
        }
        return libres;
    }

    /**
     * Nombre: buscarObligatorio
     * Entradas: nombre, nombre del archivo
     * Salidas: la entrada del indice
     * Restricciones: lanza DiscoException si el archivo no existe
     * Descripcion: variante de buscar para las operaciones que no tienen
     *              sentido sobre un archivo inexistente.
     */
    private EntradaIndice buscarObligatorio(String nombre) throws DiscoException {
        EntradaIndice entrada = buscar(nombre);
        if (entrada == null) {
            throw new DiscoException("No existe el archivo \"" + nombre + "\" en el disco");
        }
        return entrada;
    }

    /**
     * Nombre: ranuraDe
     * Entradas: nombre, nombre del archivo
     * Salidas: la posicion de la celda del indice que lo describe, o -1
     * Restricciones: un nombre nulo devuelve -1; ignora mayusculas
     * Descripcion: recorre las celdas del indice leyendo el nombre de cada
     *              entrada.
     */
    private int ranuraDe(String nombre) {
        if (nombre == null) {
            return -1;
        }
        for (int i = 0; i < ENTRADAS_INDICE; i++) {
            EntradaIndice entrada = EntradaIndice.desdeTexto(celdas[i]);
            if (entrada != null && entrada.getNombre().equalsIgnoreCase(nombre)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Nombre: primeraRanuraLibreDelIndice
     * Entradas: ninguna
     * Salidas: la posicion de la primera celda libre del indice, o -1
     * Restricciones: ninguna
     * Descripcion: el indice reutiliza las celdas que dejan los archivos
     *              eliminados.
     */
    private int primeraRanuraLibreDelIndice() {
        for (int i = 0; i < ENTRADAS_INDICE; i++) {
            if (celdas[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Nombre: buscarBloqueLibre
     * Entradas: cantidad, posiciones contiguas que se necesitan
     * Salidas: la primera posicion del bloque, o -1 si no hay ninguno
     * Restricciones: solo busca dentro del area de archivos
     * Descripcion: primer ajuste: recorre el area de archivos y devuelve el
     *              primer tramo de celdas libres consecutivas que alcance.
     */
    private int buscarBloqueLibre(int cantidad) {
        int inicioTramo = -1;
        int largoTramo = 0;
        for (int i = getInicioArchivos(); i < getInicioMemoriaVirtual(); i++) {
            if (celdas[i].isEmpty()) {
                if (largoTramo == 0) {
                    inicioTramo = i;
                }
                largoTramo++;
                if (largoTramo == cantidad) {
                    return inicioTramo;
                }
            } else {
                largoTramo = 0;
            }
        }
        return -1;
    }

    /**
     * Nombre: mayorBloqueLibre
     * Entradas: ninguna
     * Salidas: el largo del tramo de celdas libres consecutivas mas grande
     * Restricciones: solo mide dentro del area de archivos
     * Descripcion: se usa en el mensaje de error, para que el usuario sepa
     *              cuanto espacio contiguo queda.
     */
    private int mayorBloqueLibre() {
        int mayor = 0;
        int actual = 0;
        for (int i = getInicioArchivos(); i < getInicioMemoriaVirtual(); i++) {
            actual = celdas[i].isEmpty() ? actual + 1 : 0;
            mayor = Math.max(mayor, actual);
        }
        return mayor;
    }
}
