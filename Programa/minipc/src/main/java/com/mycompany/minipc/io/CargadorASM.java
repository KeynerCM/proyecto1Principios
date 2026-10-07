package com.mycompany.minipc.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Nombre: CargadorASM
 * Entradas: el archivo de codigo ensamblador elegido por el usuario
 * Salidas: las lineas de texto de ese archivo
 * Restricciones: solo acepta archivos con extension .asm; nunca escribe en
 *                el disco, solo lee
 * Descripcion: lee archivos de codigo ensamblador. No interpreta el
 *              contenido: entrega las lineas tal como estan y traducirlas es
 *              tarea del Ensamblador. Esa separacion permite ensamblar texto
 *              que no venga de un archivo, por ejemplo en las pruebas.
 */
public class CargadorASM {

    /** Unica extension aceptada. */
    public static final String EXTENSION = "asm";

    /**
     * Nombre: leer
     * Entradas: archivo, archivo a leer
     * Salidas: las lineas del archivo, sin el salto de linea final
     * Restricciones: lanza IOException si el archivo no existe, no es un
     *                archivo o no se puede leer, e IllegalArgumentException si
     *                la extension no es .asm
     * Descripcion: lee un archivo .asm completo. Intenta primero UTF-8 y, si
     *              el archivo fue guardado con la codificacion de Windows,
     *              reintenta con ISO-8859-1 en lugar de fallar: el Bloc de
     *              notas de una maquina en espanol produce archivos asi.
     */
    public List<String> leer(File archivo) throws IOException {
        validar(archivo);
        try {
            return Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            return Files.readAllLines(archivo.toPath(), StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * Nombre: validar
     * Entradas: archivo, archivo a evaluar
     * Salidas: ninguna si el archivo sirve
     * Restricciones: lanza IllegalArgumentException si el archivo es nulo o
     *                no tiene extension .asm, e IOException si no existe, no
     *                es un archivo o no hay permiso de lectura
     * Descripcion: comprueba una a una las condiciones que debe cumplir el
     *              archivo, en orden de lo mas basico a lo mas especifico,
     *              para que el mensaje de error senale la causa concreta y no
     *              un fallo generico.
     */
    private void validar(File archivo) throws IOException {
        if (archivo == null) {
            throw new IllegalArgumentException("No se indico ningun archivo");
        }
        if (!archivo.exists()) {
            throw new IOException("El archivo no existe: " + archivo.getAbsolutePath());
        }
        if (!archivo.isFile()) {
            throw new IOException("La ruta no apunta a un archivo: " + archivo.getAbsolutePath());
        }
        if (!archivo.canRead()) {
            throw new IOException("No hay permiso para leer el archivo: " + archivo.getName());
        }
        if (!tieneExtensionValida(archivo)) {
            throw new IllegalArgumentException("El archivo debe tener extension ." + EXTENSION
                    + ", se recibio \"" + archivo.getName() + "\"");
        }
    }

    /**
     * Nombre: tieneExtensionValida
     * Entradas: archivo, archivo a evaluar
     * Salidas: true si el nombre termina en .asm
     * Restricciones: un archivo nulo devuelve false en lugar de fallar
     * Descripcion: comprueba la extension sin distinguir mayusculas, de modo
     *              que PROGRAMA.ASM tambien se acepta. Es publico porque el
     *              filtro del selector de archivos lo consulta antes de que
     *              haya nada que leer.
     */
    public boolean tieneExtensionValida(File archivo) {
        return archivo != null
                && archivo.getName().toLowerCase().endsWith("." + EXTENSION);
    }
}
