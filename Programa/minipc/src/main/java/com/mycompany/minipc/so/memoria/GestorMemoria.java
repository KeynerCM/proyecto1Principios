package com.mycompany.minipc.so.memoria;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.minipc.hardware.Memoria;

/**
 * Nombre: GestorMemoria
 * Entradas: la memoria principal
 * Salidas: no aplica
 * Restricciones: solo administra la zona de usuario; la del kernel es de la
 *                tabla de BCP
 * Descripcion: asigna y libera el espacio de los programas en la zona de
 *              usuario. Cada programa ocupa un bloque contiguo (la base y el
 *              alcance de su BCP) y se ubica con la politica de primer
 *              ajuste: el primer tramo de celdas libres que alcance. Una
 *              celda libre es una celda vacia, porque un programa nunca
 *              tiene lineas vacias.
 */
public class GestorMemoria {

    private final Memoria memoria;

    /**
     * Nombre: GestorMemoria
     * Entradas: memoria, memoria principal a administrar
     * Salidas: el gestor construido
     * Restricciones: ninguna
     * Descripcion: no guarda nada propio: todo lo consulta en la memoria.
     */
    public GestorMemoria(Memoria memoria) {
        this.memoria = memoria;
    }

    /**
     * Nombre: cabeAlgunaVez
     * Entradas: tamano, posiciones que ocupa un programa
     * Salidas: true si el programa cabe en la zona de usuario vacia
     * Restricciones: ninguna
     * Descripcion: un programa mas grande que toda la zona de usuario no se
     *              podra ejecutar nunca, asi que se rechaza al cargarlo en
     *              lugar de dejarlo esperando para siempre.
     */
    public boolean cabeAlgunaVez(int tamano) {
        return tamano <= memoria.getEspacioUsuario();
    }

    /**
     * Nombre: asignar
     * Entradas: lineas, texto de cada instruccion del programa
     * Salidas: la direccion base donde quedo el programa, o -1 si no hay un
     *          bloque contiguo suficiente
     * Restricciones: si no hay espacio la memoria no cambia
     * Descripcion: busca el bloque con primer ajuste y copia el programa, una
     *              instruccion por posicion.
     */
    public int asignar(List<String> lineas) {
        int base = buscarBloqueLibre(lineas.size());
        if (base < 0) {
            return -1;
        }
        for (int i = 0; i < lineas.size(); i++) {
            memoria.escribir(base + i, lineas.get(i));
        }
        return base;
    }

    /**
     * Nombre: leer
     * Entradas: base, primera posicion del bloque; alcance, cuantas ocupa
     * Salidas: el texto de cada posicion del bloque, en orden
     * Restricciones: ninguna
     * Descripcion: la imagen del programa, para copiarla a la memoria
     *              virtual al suspender el proceso.
     */
    public List<String> leer(int base, int alcance) {
        List<String> imagen = new ArrayList<>();
        for (int i = base; i < base + alcance; i++) {
            imagen.add(memoria.leer(i));
        }
        return imagen;
    }

    /**
     * Nombre: liberar
     * Entradas: base, primera posicion del bloque; alcance, cuantas ocupa
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: deja vacias las posiciones del programa, que asi quedan
     *              disponibles para otro.
     */
    public void liberar(int base, int alcance) {
        for (int i = base; i < base + alcance; i++) {
            memoria.escribir(i, Memoria.VACIA);
        }
    }

    /**
     * Nombre: mayorBloqueLibre
     * Entradas: ninguna
     * Salidas: el tramo de celdas libres consecutivas mas grande
     * Restricciones: ninguna
     * Descripcion: se usa en los mensajes, para decir cuanto espacio hay.
     */
    public int mayorBloqueLibre() {
        int mayor = 0;
        int actual = 0;
        for (int i = memoria.getLimiteKernel(); i < memoria.getTamano(); i++) {
            actual = memoria.estaLibre(i) ? actual + 1 : 0;
            mayor = Math.max(mayor, actual);
        }
        return mayor;
    }

    /**
     * Nombre: buscarBloqueLibre
     * Entradas: cantidad, posiciones contiguas que se necesitan
     * Salidas: la primera posicion del bloque, o -1 si no hay ninguno
     * Restricciones: solo busca en la zona de usuario
     * Descripcion: primer ajuste.
     */
    private int buscarBloqueLibre(int cantidad) {
        int inicioTramo = -1;
        int largoTramo = 0;
        for (int i = memoria.getLimiteKernel(); i < memoria.getTamano(); i++) {
            if (memoria.estaLibre(i)) {
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
}
