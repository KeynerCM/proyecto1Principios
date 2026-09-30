package com.mycompany.minipc.so.trabajos;

import java.util.List;
import java.util.function.Consumer;

import com.mycompany.minipc.excepciones.DiscoException;
import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.so.memoria.GestorMemoria;
import com.mycompany.minipc.so.procesos.ListaProcesos;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;

/**
 * Nombre: PlanificadorTrabajos
 * Entradas: la lista de trabajos, la lista de procesos, la tabla de BCP, el
 *           gestor de memoria y el disco
 * Salidas: no aplica
 * Restricciones: admite en orden de llegada y sin saltarse trabajos: si el
 *                primero no cabe, los siguientes tambien esperan
 * Descripcion: el "Planificador de trabajos" del enunciado, que Stallings
 *              llama planificador de largo plazo (seccion 9.1): decide que
 *              programas de la lista de trabajos entran al sistema, y asi
 *              controla el grado de multiprogramacion, que aqui es 5. Para
 *              admitir un trabajo lee el programa del disco, le busca lugar
 *              en la memoria de usuario, crea su BCP en el kernel y lo enlaza
 *              al final de la lista de procesos. Si no hay espacio, el
 *              trabajo espera hasta que se libere, como pide el enunciado.
 */
public class PlanificadorTrabajos {

    private final ListaTrabajos trabajos;
    private final ListaProcesos procesos;
    private final TablaBCP tabla;
    private final GestorMemoria gestorMemoria;
    private final Disco disco;
    private final Consumer<String> bitacora;

    /**
     * Nombre: PlanificadorTrabajos
     * Entradas: trabajos, lista de trabajos; procesos, lista de procesos;
     *           tabla, tabla de BCP; gestorMemoria, administrador de la zona
     *           de usuario; disco, de donde se leen los programas; bitacora,
     *           donde se anota lo que ocurre
     * Salidas: el planificador construido
     * Restricciones: ninguna
     * Descripcion: recibe todo lo que necesita, sin crear nada propio.
     */
    public PlanificadorTrabajos(ListaTrabajos trabajos, ListaProcesos procesos, TablaBCP tabla,
            GestorMemoria gestorMemoria, Disco disco, Consumer<String> bitacora) {
        this.trabajos = trabajos;
        this.procesos = procesos;
        this.tabla = tabla;
        this.gestorMemoria = gestorMemoria;
        this.disco = disco;
        this.bitacora = bitacora;
    }

    /**
     * Nombre: admitir
     * Entradas: reloj, segundo simulado actual
     * Salidas: cuantos trabajos admitio
     * Restricciones: se detiene al llegar a 5 procesos, al quedarse sin
     *                trabajos nuevos o cuando el siguiente no cabe
     * Descripcion: admite trabajos NUEVO mientras se pueda.
     */
    public int admitir(int reloj) {
        int admitidos = 0;
        Trabajo trabajo = trabajos.siguienteNuevo();
        while (trabajo != null && tabla.hayRanuraLibre()) {
            if (!admitirUno(trabajo, reloj)) {
                break;
            }
            admitidos++;
            trabajo = trabajos.siguienteNuevo();
        }
        return admitidos;
    }

    /**
     * Nombre: admitirUno
     * Entradas: trabajo, el primer trabajo NUEVO; reloj, segundo actual
     * Salidas: true si se admitio, false si no hay espacio en memoria
     * Restricciones: el aviso de falta de espacio se escribe una sola vez
     * Descripcion: disco -> memoria de usuario -> BCP -> lista de procesos.
     */
    private boolean admitirUno(Trabajo trabajo, int reloj) {
        List<String> lineas;
        try {
            lineas = disco.leerPrograma(trabajo.getPrograma());
        } catch (DiscoException e) {
            throw new IllegalStateException("El programa de P" + trabajo.getPid()
                    + " ya no esta en el disco", e);
        }
        int base = gestorMemoria.asignar(lineas);
        if (base < 0) {
            if (!trabajo.isEsperandoMemoria()) {
                bitacora.accept("Planificador de trabajos: P" + trabajo.getPid() + " ("
                        + trabajo.getPrograma() + ") necesita " + lineas.size()
                        + " posiciones y el bloque libre mas grande es de "
                        + gestorMemoria.mayorBloqueLibre() + "; espera a que se libere memoria.");
                trabajo.setEsperandoMemoria(true);
            }
            return false;
        }
        Proceso proceso = tabla.crear(trabajo.getPid(), trabajo.getPrograma(), base,
                lineas.size(), reloj);
        procesos.agregarAlFinal(proceso);
        trabajo.admitir(proceso, reloj);
        bitacora.accept("Planificador de trabajos: admite " + proceso + " ("
                + trabajo.getPrograma() + "). BCP en " + proceso.getDireccionBCP() + ".."
                + proceso.getDireccionFinBCP() + ", programa en " + base + ".."
                + (base + lineas.size() - 1) + ". Pasa a PREPARADO.");
        return true;
    }
}
