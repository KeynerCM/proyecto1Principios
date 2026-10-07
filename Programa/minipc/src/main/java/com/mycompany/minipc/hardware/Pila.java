package com.mycompany.minipc.hardware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mycompany.minipc.excepciones.DesbordamientoException;

/**
 * Nombre: Pila
 * Entradas: los valores que el programa guarda con PUSH y PARAM
 * Salidas: los valores que el programa saca con POP
 * Restricciones: tiene capacidad fija de 5 valores; superarla, o sacar de una
 *                pila vacia, es un error de ejecucion
 * Descripcion: la pila del proceso, de tipo LIFO: el ultimo valor que entra es
 *              el primero que sale. Forma parte del contexto del proceso: se
 *              guarda en el BCP en cada cambio de contexto. La usan PUSH, POP y
 *              PARAM.
 */
public class Pila {

    /** Capacidad de la pila. */
    public static final int CAPACIDAD = 5;

    private final List<Integer> valores;

    /**
     * Nombre: Pila
     * Entradas: ninguna
     * Salidas: la pila construida, vacia
     * Restricciones: ninguna
     * Descripcion: crea una pila vacia con capacidad para 5 valores.
     */
    public Pila() {
        this.valores = new ArrayList<>(CAPACIDAD);
    }

    /**
     * Nombre: apilar
     * Entradas: valor, entero a guardar en el tope
     * Salidas: ninguna
     * Restricciones: lanza DesbordamientoException si la pila ya esta llena,
     *                y en ese caso la pila no cambia
     * Descripcion: coloca el valor en el tope de la pila.
     */
    public void apilar(int valor) {
        if (estaLlena()) {
            throw new DesbordamientoException("Desbordamiento de pila: no se puede guardar "
                    + valor + " porque la pila ya tiene " + CAPACIDAD + " de " + CAPACIDAD
                    + " valores");
        }
        valores.add(valor);
    }

    /**
     * Nombre: desapilar
     * Entradas: ninguna
     * Salidas: el valor que estaba en el tope
     * Restricciones: lanza DesbordamientoException si la pila esta vacia
     * Descripcion: saca el valor del tope de la pila y lo devuelve.
     */
    public int desapilar() {
        if (valores.isEmpty()) {
            throw new DesbordamientoException("Pila vacia: no hay ningun valor que sacar");
        }
        return valores.remove(valores.size() - 1);
    }

    /**
     * Nombre: getLibres
     * Entradas: ninguna
     * Salidas: cuantos valores mas caben
     * Restricciones: ninguna
     * Descripcion: permite a PARAM comprobar de antemano si caben todos sus
     *              valores, para no dejar la pila a medio llenar.
     */
    public int getLibres() {
        return CAPACIDAD - valores.size();
    }

    /**
     * Nombre: estaLlena
     * Entradas: ninguna
     * Salidas: true si ya no cabe ningun valor
     * Restricciones: ninguna
     * Descripcion: evita comparar contra la capacidad desde afuera.
     */
    public boolean estaLlena() {
        return valores.size() == CAPACIDAD;
    }

    /**
     * Nombre: getTamano
     * Entradas: ninguna
     * Salidas: cuantos valores hay en la pila
     * Restricciones: ninguna
     * Descripcion: es el puntero de pila (SP) del proceso.
     */
    public int getTamano() {
        return valores.size();
    }

    /**
     * Nombre: getValores
     * Entradas: ninguna
     * Salidas: los valores de la pila, del fondo al tope
     * Restricciones: la lista es de solo lectura
     * Descripcion: el BCP la copia para mostrar la pila del proceso.
     */
    public List<Integer> getValores() {
        return Collections.unmodifiableList(valores);
    }

    /**
     * Nombre: vaciar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: deja la pila vacia, al cargar o reiniciar un programa.
     */
    public void vaciar() {
        valores.clear();
    }
}
