package com.mycompany.minipc.hardware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nombre: Pantalla
 * Entradas: las lineas que escribe el sistema operativo
 * Salidas: el contenido que muestra la interfaz
 * Restricciones: guarda como maximo las ultimas 200 lineas
 * Descripcion: el monitor del Mini PC. Es un dispositivo de solo salida: el
 *              sistema operativo escribe en el lo que pide INT 10H y el aviso
 *              de INT 09H (">> Ingresar valor:"), y el eco de lo que se
 *              escribe en el teclado. Cada linea lleva el proceso que la
 *              escribio, por ejemplo "[P2] 14", porque varios procesos
 *              comparten la misma pantalla.
 */
public class Pantalla {

    /** Lineas que se conservan; las mas viejas se descartan. */
    public static final int MAX_LINEAS = 200;

    private final List<String> lineas = new ArrayList<>();

    /**
     * Nombre: escribir
     * Entradas: linea, texto a mostrar
     * Salidas: ninguna
     * Restricciones: descarta la linea mas vieja si ya hay 200
     * Descripcion: agrega una linea al final de la pantalla.
     */
    public void escribir(String linea) {
        lineas.add(linea);
        if (lineas.size() > MAX_LINEAS) {
            lineas.remove(0);
        }
    }

    /**
     * Nombre: getLineas
     * Entradas: ninguna
     * Salidas: las lineas en pantalla, de la mas vieja a la mas nueva
     * Restricciones: la lista no se puede modificar
     * Descripcion: la interfaz la dibuja.
     */
    public List<String> getLineas() {
        return Collections.unmodifiableList(lineas);
    }

    /**
     * Nombre: completar
     * Entradas: linea, texto exacto de una linea ya escrita; agregado, texto
     *           que se le pega al final
     * Salidas: ninguna
     * Restricciones: busca desde la ultima linea hacia arriba; si la linea ya
     *                no esta (se descarto por vieja), escribe una nueva con
     *                los dos textos
     * Descripcion: el eco del teclado queda en la misma linea del aviso, como
     *              en una terminal: "[P3] >> Ingresar valor: 56".
     */
    public void completar(String linea, String agregado) {
        for (int i = lineas.size() - 1; i >= 0; i--) {
            if (lineas.get(i).equals(linea)) {
                lineas.set(i, linea + agregado);
                return;
            }
        }
        escribir(linea + agregado);
    }

    /**
     * Nombre: limpiar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: deja la pantalla en blanco.
     */
    public void limpiar() {
        lineas.clear();
    }
}
