package com.mycompany.minipc.so.archivos;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mycompany.minipc.excepciones.DiscoException;
import com.mycompany.minipc.excepciones.EjecucionException;
import com.mycompany.minipc.excepciones.ViolacionProteccionException;
import com.mycompany.minipc.hardware.BancoRegistros;
import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.hardware.Procesador;
import com.mycompany.minipc.isa.RegistroID;
import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: SistemaArchivos
 * Entradas: el disco y la memoria principal
 * Salidas: no aplica
 * Restricciones: un error (archivo inexistente, archivo sin abrir, servicio
 *                desconocido) es una EjecucionException: el proceso termina
 *                con ese error
 * Descripcion: atiende INT 21H, el manejo de archivos. AH elige el servicio, DX
 *              apunta al nombre del archivo y AL lleva el byte que se lee o se
 *              escribe:
 *
 *                AH = 3Ch  crear      AH = 3Dh  abrir     AH = 4Dh  leer
 *                AH = 40h  escribir   AH = 41h  eliminar
 *
 *              Los archivos de datos viven en el disco, con su entrada en el
 *              indice, y ocupan una celda con sus bytes, por ejemplo
 *              "[65,66]". La lista de archivos abiertos de cada proceso esta
 *              en su BCP, como "datos.txt:1", donde el numero es la posicion
 *              de la proxima lectura.
 */
public class SistemaArchivos {

    public static final int CREAR = 0x3C;
    public static final int ABRIR = 0x3D;
    public static final int LEER = 0x4D;
    public static final int ESCRIBIR = 0x40;
    public static final int ELIMINAR = 0x41;

    /** Separa el nombre de la posicion de lectura en la lista del BCP. */
    private static final String SEPARADOR_POSICION = ":";

    /** El texto entre comillas de la celda a la que apunta DX. */
    private static final Pattern NOMBRE = Pattern.compile("\"([^\"]+)\"");

    private final Disco disco;
    private final Memoria memoria;

    /**
     * Nombre: SistemaArchivos
     * Entradas: disco, donde viven los archivos; memoria, donde esta el nombre
     * Salidas: el sistema de archivos construido
     * Restricciones: ninguna
     * Descripcion: guarda las referencias.
     */
    public SistemaArchivos(Disco disco, Memoria memoria) {
        this.disco = disco;
        this.memoria = memoria;
    }

    /**
     * Nombre: atender
     * Entradas: cpu, con AH, AL y DX del proceso; proceso, el que llamo
     * Salidas: una frase con lo que se hizo, para la bitacora
     * Restricciones: lanza EjecucionException ante cualquier error
     * Descripcion: ejecuta el servicio que pide AH.
     */
    public String atender(Procesador cpu, Proceso proceso) {
        BancoRegistros registros = cpu.getRegistros();
        int servicio = registros.leer(RegistroID.AH);
        String nombre = nombreApuntadoPorDx(cpu);
        try {
            switch (servicio) {
                case CREAR:
                    disco.crearArchivo(nombre);
                    return "crea el archivo " + nombre + " en el disco";
                case ABRIR:
                    return abrir(proceso, nombre);
                case LEER:
                    return leer(proceso, nombre, registros);
                case ESCRIBIR:
                    return escribir(proceso, nombre, registros.leer(RegistroID.AL));
                case ELIMINAR:
                    disco.eliminarDatos(nombre);
                    cerrar(proceso, nombre);
                    return "elimina el archivo " + nombre;
                default:
                    throw new EjecucionException("INT 21H: AH = " + hex(servicio)
                            + " no es un servicio de archivos. Los validos son 3Ch crear,"
                            + " 3Dh abrir, 4Dh leer, 40h escribir y 41h eliminar");
            }
        } catch (DiscoException e) {
            throw new EjecucionException("INT 21H: " + e.getMessage());
        }
    }

    /**
     * Nombre: nombreApuntadoPorDx
     * Entradas: cpu, con DX y los registros base y alcance del proceso
     * Salidas: el nombre del archivo
     * Restricciones: DX debe apuntar a una celda del propio proceso que tenga
     *                un texto entre comillas; si sale del programa lanza
     *                ViolacionProteccionException y si la celda no tiene un
     *                nombre lanza EjecucionException
     * Descripcion: DX guarda el desplazamiento del nombre dentro del programa,
     *              que es la celda de la instruccion MOV DX, "nombre". La
     *              direccion real es base + DX: primero se compara DX con el
     *              alcance y despues se suma la base. Asi un proceso no puede
     *              leer datos de otro ni del kernel.
     */
    private String nombreApuntadoPorDx(Procesador cpu) {
        int dx = cpu.getRegistros().leer(RegistroID.DX);
        if (dx < 0 || dx >= cpu.getAlcance()) {
            throw new ViolacionProteccionException("INT 21H: DX = " + dx + " sale del programa"
                    + " (desplazamientos 0 a " + (cpu.getAlcance() - 1) + "). Cargue el"
                    + " nombre con MOV DX, \"archivo.txt\"");
        }
        int direccion = cpu.getBase() + dx;
        Matcher texto = NOMBRE.matcher(memoria.leer(direccion));
        if (!texto.find()) {
            throw new EjecucionException("INT 21H: la posicion " + direccion + " (base + DX)"
                    + " no contiene un nombre de archivo entre comillas");
        }
        return texto.group(1);
    }

    /**
     * Nombre: abrir
     * Entradas: proceso, el que abre; nombre, archivo a abrir
     * Salidas: la frase para la bitacora
     * Restricciones: lanza DiscoException si el archivo no existe
     * Descripcion: lo agrega a los archivos abiertos del BCP con la posicion
     *              de lectura en cero; si ya estaba abierto no lo repite.
     */
    private String abrir(Proceso proceso, String nombre) throws DiscoException {
        disco.leerDatos(nombre);
        if (posicionDe(proceso, nombre) < 0) {
            List<String> abiertos = proceso.getArchivosAbiertos();
            abiertos.add(nombre + SEPARADOR_POSICION + 0);
            proceso.setArchivosAbiertos(abiertos);
        }
        return "abre el archivo " + nombre;
    }

    /**
     * Nombre: leer
     * Entradas: proceso, el que lee; nombre, archivo; registros, donde queda AL
     * Salidas: la frase para la bitacora
     * Restricciones: el archivo debe estar abierto
     * Descripcion: deja en AL el byte de la posicion de lectura y la avanza;
     *              al final del archivo deja AL en cero.
     */
    private String leer(Proceso proceso, String nombre, BancoRegistros registros)
            throws DiscoException {
        int posicion = posicionObligatoria(proceso, nombre);
        List<Integer> datos = disco.leerDatos(nombre);
        if (posicion >= datos.size()) {
            registros.escribir(RegistroID.AL, 0);
            return "lee " + nombre + ": fin del archivo, AL = 0";
        }
        int dato = datos.get(posicion);
        registros.escribir(RegistroID.AL, dato);
        moverPosicion(proceso, nombre, posicion + 1);
        return "lee " + nombre + ": AL = " + dato;
    }

    /**
     * Nombre: escribir
     * Entradas: proceso, el que escribe; nombre, archivo; dato, valor de AL
     * Salidas: la frase para la bitacora
     * Restricciones: el archivo debe estar abierto
     * Descripcion: agrega el byte al final del archivo.
     */
    private String escribir(Proceso proceso, String nombre, int dato) throws DiscoException {
        posicionObligatoria(proceso, nombre);
        List<Integer> datos = disco.leerDatos(nombre);
        datos.add(dato);
        disco.escribirDatos(nombre, datos);
        return "escribe AL = " + dato + " en " + nombre;
    }

    /**
     * Nombre: posicionObligatoria
     * Entradas: proceso; nombre, archivo
     * Salidas: la posicion de lectura del archivo abierto
     * Restricciones: lanza EjecucionException si el archivo no esta abierto
     * Descripcion: leer y escribir exigen abrir antes con AH = 3Dh.
     */
    private int posicionObligatoria(Proceso proceso, String nombre) {
        int posicion = posicionDe(proceso, nombre);
        if (posicion < 0) {
            throw new EjecucionException("INT 21H: el archivo " + nombre + " no esta abierto;"
                    + " primero hay que abrirlo con AH = 3Dh");
        }
        return posicion;
    }

    /**
     * Nombre: posicionDe
     * Entradas: proceso; nombre, archivo
     * Salidas: la posicion de lectura, o -1 si el archivo no esta abierto
     * Restricciones: el nombre no distingue mayusculas, como en el disco
     * Descripcion: busca el archivo en la lista del BCP.
     */
    private int posicionDe(Proceso proceso, String nombre) {
        for (String entrada : proceso.getArchivosAbiertos()) {
            String[] partes = entrada.split(SEPARADOR_POSICION);
            if (partes[0].equalsIgnoreCase(nombre)) {
                return Integer.parseInt(partes[1]);
            }
        }
        return -1;
    }

    /**
     * Nombre: moverPosicion
     * Entradas: proceso; nombre, archivo abierto; posicion, nueva posicion
     * Salidas: ninguna
     * Restricciones: el archivo debe estar abierto
     * Descripcion: actualiza la entrada del archivo en la lista del BCP.
     */
    private void moverPosicion(Proceso proceso, String nombre, int posicion) {
        List<String> abiertos = proceso.getArchivosAbiertos();
        for (int i = 0; i < abiertos.size(); i++) {
            if (abiertos.get(i).split(SEPARADOR_POSICION)[0].equalsIgnoreCase(nombre)) {
                abiertos.set(i, nombre + SEPARADOR_POSICION + posicion);
            }
        }
        proceso.setArchivosAbiertos(abiertos);
    }

    /**
     * Nombre: cerrar
     * Entradas: proceso; nombre, archivo
     * Salidas: ninguna
     * Restricciones: no falla si no estaba abierto
     * Descripcion: lo quita de la lista del BCP.
     */
    private void cerrar(Proceso proceso, String nombre) {
        List<String> abiertos = proceso.getArchivosAbiertos();
        abiertos.removeIf(e -> e.split(SEPARADOR_POSICION)[0].equalsIgnoreCase(nombre));
        proceso.setArchivosAbiertos(abiertos);
    }

    /**
     * Nombre: hex
     * Entradas: valor, numero de 0 a 255
     * Salidas: el numero en hexadecimal con sufijo h, por ejemplo "3Ch"
     * Restricciones: ninguna
     * Descripcion: para los mensajes, con la misma notacion del .asm.
     */
    private static String hex(int valor) {
        return String.format("%02Xh", valor);
    }
}
