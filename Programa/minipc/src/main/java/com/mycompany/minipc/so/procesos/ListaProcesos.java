package com.mycompany.minipc.so.procesos;

import java.util.ArrayList;
import java.util.List;

/**
 * Nombre: ListaProcesos
 * Entradas: la tabla de BCP, que da acceso a la cabecera y a los enlaces
 * Salidas: no aplica
 * Restricciones: no guarda los procesos en una coleccion de Java; la lista
 *                existe solo en memoria, en los enlaces entre BCP
 * Descripcion: la "Estructura de Lista de Proceso" del enunciado. Es una lista
 *              enlazada: la cabecera del sistema operativo guarda la direccion
 *              del primer BCP y cada BCP guarda en su campo SIGUIENTE la
 *              direccion del que sigue. Recorrerla es seguir esas direcciones
 *              en memoria. El orden es el de llegada a la lista, y un proceso
 *              que vuelve a quedar PREPARADO pasa al final, de modo que el
 *              orden de los PREPARADO es el de llegada a la cola de listos
 *              que usa FCFS.
 */
public class ListaProcesos {

    private final TablaBCP tabla;

    /**
     * Nombre: ListaProcesos
     * Entradas: tabla, tabla de BCP en memoria
     * Salidas: la lista construida
     * Restricciones: ninguna
     * Descripcion: la lista arranca vacia porque la tabla arranca formateada.
     */
    public ListaProcesos(TablaBCP tabla) {
        this.tabla = tabla;
    }

    /**
     * Nombre: agregarAlFinal
     * Entradas: proceso, proceso a enlazar
     * Salidas: ninguna
     * Restricciones: el proceso no debe estar ya en la lista
     * Descripcion: si la lista esta vacia, la cabecera apunta al proceso; si
     *              no, el ultimo BCP pasa a apuntarle.
     */
    public void agregarAlFinal(Proceso proceso) {
        proceso.setSiguiente(null);
        Proceso ultimo = getUltimo();
        if (ultimo == null) {
            tabla.setInicioLista(proceso);
        } else {
            ultimo.setSiguiente(proceso);
        }
    }

    /**
     * Nombre: quitar
     * Entradas: proceso, proceso a desenlazar
     * Salidas: ninguna
     * Restricciones: si el proceso no esta en la lista no hace nada
     * Descripcion: el anterior pasa a apuntar al siguiente del que se quita.
     */
    public void quitar(Proceso proceso) {
        Proceso anterior = null;
        Proceso actual = tabla.getInicioLista();
        while (actual != null && !actual.equals(proceso)) {
            anterior = actual;
            actual = actual.getSiguiente();
        }
        if (actual == null) {
            return;
        }
        if (anterior == null) {
            tabla.setInicioLista(actual.getSiguiente());
        } else {
            anterior.setSiguiente(actual.getSiguiente());
        }
        actual.setSiguiente(null);
    }

    /**
     * Nombre: moverAlFinal
     * Entradas: proceso, proceso que vuelve a la cola de listos
     * Salidas: ninguna
     * Restricciones: el proceso debe estar en la lista
     * Descripcion: lo quita y lo vuelve a enlazar al final.
     */
    public void moverAlFinal(Proceso proceso) {
        quitar(proceso);
        agregarAlFinal(proceso);
    }

    /**
     * Nombre: recorrer
     * Entradas: ninguna
     * Salidas: los procesos en el orden de la lista
     * Restricciones: la lista devuelta es una foto; cambiarla no cambia la
     *                memoria
     * Descripcion: empieza en la direccion que guarda la cabecera y sigue los
     *              enlaces SIGUIENTE hasta uno vacio.
     */
    public List<Proceso> recorrer() {
        List<Proceso> procesos = new ArrayList<>();
        Proceso actual = tabla.getInicioLista();
        while (actual != null && procesos.size() <= TablaBCP.MAX_PROCESOS) {
            procesos.add(actual);
            actual = actual.getSiguiente();
        }
        return procesos;
    }

    /**
     * Nombre: conEstado
     * Entradas: estado, estado buscado
     * Salidas: los procesos con ese estado, en el orden de la lista
     * Restricciones: ninguna
     * Descripcion: con PREPARADO da la cola de listos que usa el planificador.
     */
    public List<Proceso> conEstado(EstadoProceso estado) {
        List<Proceso> resultado = new ArrayList<>();
        for (Proceso proceso : recorrer()) {
            if (proceso.getEstado() == estado) {
                resultado.add(proceso);
            }
        }
        return resultado;
    }

    /**
     * Nombre: estaVacia
     * Entradas: ninguna
     * Salidas: true si la cabecera no apunta a ningun BCP
     * Restricciones: ninguna
     * Descripcion: acceso comodo a la cabecera.
     */
    public boolean estaVacia() {
        return tabla.getInicioLista() == null;
    }

    /**
     * Nombre: getUltimo
     * Entradas: ninguna
     * Salidas: el ultimo proceso de la lista, o nulo si esta vacia
     * Restricciones: ninguna
     * Descripcion: recorre los enlaces hasta el que no tiene siguiente.
     */
    private Proceso getUltimo() {
        List<Proceso> procesos = recorrer();
        return procesos.isEmpty() ? null : procesos.get(procesos.size() - 1);
    }
}
