package com.mycompany.minipc.so.memoria;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.minipc.hardware.Disco;

/**
 * Nombre: MemoriaVirtual
 * Entradas: el disco, cuyas ultimas posiciones son el area de intercambio
 * Salidas: no aplica
 * Restricciones: solo usa el area de memoria virtual del disco; el indice y
 *                los archivos no se tocan
 * Descripcion: administra el area de intercambio (swap) del disco, la
 *              "extension de la memoria principal" que describe Stallings
 *              (seccion 1.5). Funciona igual que el GestorMemoria pero sobre
 *              el disco: cada imagen de proceso ocupa un bloque contiguo,
 *              ubicado con primer ajuste, y una celda libre es una celda
 *              vacia. Las imagenes no tienen entrada en el indice porque no
 *              son archivos: el BCP del proceso guarda donde quedo (BASE).
 */
public class MemoriaVirtual {

    private final Disco disco;

    /**
     * Nombre: MemoriaVirtual
     * Entradas: disco, disco del Mini PC
     * Salidas: la memoria virtual construida
     * Restricciones: ninguna
     * Descripcion: no guarda nada propio: todo lo consulta en el disco, asi
     *              que sigue valiendo si el disco cambia de tamano.
     */
    public MemoriaVirtual(Disco disco) {
        this.disco = disco;
    }

    /**
     * Nombre: guardar
     * Entradas: imagen, texto de cada posicion del programa
     * Salidas: la direccion del disco donde quedo, o -1 si no hay un bloque
     *          contiguo suficiente
     * Restricciones: si no hay espacio el disco no cambia
     * Descripcion: busca el bloque con primer ajuste y copia la imagen.
     */
    public int guardar(List<String> imagen) {
        int inicio = buscarBloqueLibre(imagen.size());
        if (inicio < 0) {
            return -1;
        }
        for (int i = 0; i < imagen.size(); i++) {
            disco.escribirMemoriaVirtual(inicio + i, imagen.get(i));
        }
        return inicio;
    }

    /**
     * Nombre: leer
     * Entradas: inicio, direccion del disco; cantidad, posiciones a leer
     * Salidas: el texto de cada posicion, en orden
     * Restricciones: ninguna
     * Descripcion: recupera la imagen para traerla a la memoria principal.
     */
    public List<String> leer(int inicio, int cantidad) {
        List<String> imagen = new ArrayList<>();
        for (int i = inicio; i < inicio + cantidad; i++) {
            imagen.add(disco.leer(i));
        }
        return imagen;
    }

    /**
     * Nombre: liberar
     * Entradas: inicio, direccion del disco; cantidad, posiciones que ocupa
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: deja vacias las posiciones de la imagen.
     */
    public void liberar(int inicio, int cantidad) {
        for (int i = inicio; i < inicio + cantidad; i++) {
            disco.escribirMemoriaVirtual(i, Disco.VACIA);
        }
    }

    /**
     * Nombre: limpiar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: descarta todas las imagenes
     * Descripcion: se usa al reiniciar el sistema.
     */
    public void limpiar() {
        liberar(disco.getInicioMemoriaVirtual(), disco.getTamanoMemoriaVirtual());
    }

    /**
     * Nombre: mayorBloqueLibre
     * Entradas: ninguna
     * Salidas: el tramo de celdas libres consecutivas mas grande
     * Restricciones: ninguna
     * Descripcion: se usa en los mensajes de la bitacora.
     */
    public int mayorBloqueLibre() {
        int mayor = 0;
        int actual = 0;
        for (int i = disco.getInicioMemoriaVirtual(); i < disco.getTamano(); i++) {
            actual = disco.estaLibre(i) ? actual + 1 : 0;
            mayor = Math.max(mayor, actual);
        }
        return mayor;
    }

    /**
     * Nombre: buscarBloqueLibre
     * Entradas: cantidad, posiciones contiguas que se necesitan
     * Salidas: la primera posicion del bloque, o -1 si no hay ninguno
     * Restricciones: solo busca en el area de memoria virtual
     * Descripcion: primer ajuste.
     */
    private int buscarBloqueLibre(int cantidad) {
        int inicioTramo = -1;
        int largoTramo = 0;
        for (int i = disco.getInicioMemoriaVirtual(); i < disco.getTamano(); i++) {
            if (disco.estaLibre(i)) {
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
