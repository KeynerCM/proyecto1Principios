package com.mycompany.minipc.so.memoria;

import java.util.List;
import java.util.function.Consumer;

import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.ListaProcesos;
import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: Intercambio
 * Entradas: la lista de procesos, el gestor de la memoria principal y la
 *           memoria virtual
 * Salidas: no aplica
 * Restricciones: nunca mueve al proceso en ejecucion; el BCP se queda siempre
 *                en el kernel y solo viaja la imagen del programa
 * Descripcion: el intercambio (swapping) entre la memoria principal y la
 *              memoria virtual, que Stallings asigna al planificador de
 *              mediano plazo (secciones 3.2 y 9.1). Aplica las transiciones
 *              de la figura 3.9b:
 *
 *                EN_ESPERA            -> SUSPENDIDO_EN_ESPERA (suspender)
 *                SUSPENDIDO_PREPARADO -> PREPARADO            (traer)
 *
 *              La de NUEVO -> SUSPENDIDO_PREPARADO la hace el planificador de
 *              trabajos al admitir, y SUSPENDIDO_EN_ESPERA ->
 *              SUSPENDIDO_PREPARADO la hace el manejador de interrupciones
 *              cuando llega el valor del teclado. Al mover la imagen se
 *              reubica el proceso: cambian su BASE y su PC.
 */
public class Intercambio {

    private final ListaProcesos procesos;
    private final GestorMemoria gestorMemoria;
    private final MemoriaVirtual memoriaVirtual;
    private final Consumer<String> bitacora;

    /**
     * Nombre: Intercambio
     * Entradas: procesos, lista de procesos; gestorMemoria, memoria
     *           principal; memoriaVirtual, area de intercambio del disco;
     *           bitacora, donde se anota cada movimiento
     * Salidas: el intercambio construido
     * Restricciones: ninguna
     * Descripcion: guarda las referencias.
     */
    public Intercambio(ListaProcesos procesos, GestorMemoria gestorMemoria,
            MemoriaVirtual memoriaVirtual, Consumer<String> bitacora) {
        this.procesos = procesos;
        this.gestorMemoria = gestorMemoria;
        this.memoriaVirtual = memoriaVirtual;
        this.bitacora = bitacora;
    }

    /**
     * Nombre: equilibrar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: se llama en cada segundo, despues de admitir trabajos
     * Descripcion: primero trae los SUSPENDIDO_PREPARADO que quepan. Si aun
     *              asi no hay ningun proceso listo en memoria y alguno espera
     *              en el disco, suspende a un proceso EN_ESPERA para hacerle
     *              lugar, como dice el libro: "If there are no ready
     *              processes, then at least one blocked process is swapped
     *              out to make room for another process that is not blocked"
     *              (p. 146). Repite mientras siga sin haber procesos listos.
     */
    public void equilibrar() {
        traerSuspendidos();
        while (!hayListosEnMemoria() && hayEsperandoMemoria()) {
            List<Proceso> bloqueados = procesos.conEstado(EstadoProceso.EN_ESPERA);
            if (bloqueados.isEmpty()) {
                return;
            }
            // El ultimo de la lista es el que mas tardara en recibir el
            // teclado, que se entrega en orden de llegada.
            if (!suspender(bloqueados.get(bloqueados.size() - 1))) {
                return;
            }
            traerSuspendidos();
        }
    }

    /**
     * Nombre: traerSuspendidos
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: respeta el orden de llegada: si el primero no cabe, los
     *                demas tambien esperan
     * Descripcion: trae a la memoria principal los SUSPENDIDO_PREPARADO
     *              mientras haya espacio.
     */
    public void traerSuspendidos() {
        for (Proceso proceso : procesos.conEstado(EstadoProceso.SUSPENDIDO_PREPARADO)) {
            if (!traer(proceso)) {
                return;
            }
        }
    }

    /**
     * Nombre: hayEsperandoMemoria
     * Entradas: ninguna
     * Salidas: true si algun proceso SUSPENDIDO_PREPARADO espera volver a la
     *          memoria principal
     * Restricciones: ninguna
     * Descripcion: el planificador de trabajos lo consulta para no adelantar
     *              un trabajo nuevo a un proceso que llego antes.
     */
    public boolean hayEsperandoMemoria() {
        return !procesos.conEstado(EstadoProceso.SUSPENDIDO_PREPARADO).isEmpty();
    }

    /**
     * Nombre: suspender
     * Entradas: proceso, uno EN_ESPERA con su imagen en memoria principal
     * Salidas: true si se suspendio, false si no cabe en la memoria virtual
     * Restricciones: si no cabe, nada cambia
     * Descripcion: copia la imagen al disco, libera su espacio en memoria,
     *              reubica el proceso y lo deja SUSPENDIDO_EN_ESPERA.
     */
    public boolean suspender(Proceso proceso) {
        int base = proceso.getBase();
        int alcance = proceso.getAlcance();
        int inicio = memoriaVirtual.guardar(gestorMemoria.leer(base, alcance));
        if (inicio < 0) {
            return false;
        }
        gestorMemoria.liberar(base, alcance);
        proceso.reubicar(inicio);
        proceso.setEstado(EstadoProceso.SUSPENDIDO_EN_ESPERA);
        bitacora.accept("Memoria virtual: no hay procesos PREPARADO en memoria; " + proceso
                + " (EN_ESPERA) sale de " + rango(base, alcance) + " al disco "
                + rango(inicio, alcance) + " y pasa a SUSPENDIDO_EN_ESPERA.");
        return true;
    }

    /**
     * Nombre: traer
     * Entradas: proceso, uno SUSPENDIDO_PREPARADO con su imagen en el disco
     * Salidas: true si volvio a memoria, false si no hay espacio
     * Restricciones: si no cabe, nada cambia
     * Descripcion: copia la imagen a la memoria principal, libera el disco,
     *              reubica el proceso y lo deja PREPARADO.
     */
    public boolean traer(Proceso proceso) {
        int inicio = proceso.getBase();
        int alcance = proceso.getAlcance();
        int base = gestorMemoria.asignar(memoriaVirtual.leer(inicio, alcance));
        if (base < 0) {
            return false;
        }
        memoriaVirtual.liberar(inicio, alcance);
        proceso.reubicar(base);
        proceso.setEstado(EstadoProceso.PREPARADO);
        bitacora.accept("Memoria virtual: " + proceso + " vuelve del disco "
                + rango(inicio, alcance) + " a la memoria principal " + rango(base, alcance)
                + " y pasa a PREPARADO.");
        return true;
    }

    /**
     * Nombre: hayListosEnMemoria
     * Entradas: ninguna
     * Salidas: true si hay un proceso PREPARADO o en EJECUCION
     * Restricciones: ninguna
     * Descripcion: si la CPU tiene trabajo no hace falta suspender a nadie.
     */
    private boolean hayListosEnMemoria() {
        return !procesos.conEstado(EstadoProceso.PREPARADO).isEmpty()
                || !procesos.conEstado(EstadoProceso.EJECUCION).isEmpty();
    }

    /**
     * Nombre: rango
     * Entradas: inicio, primera posicion; cantidad, posiciones
     * Salidas: el texto "inicio..fin"
     * Restricciones: ninguna
     * Descripcion: para los mensajes de la bitacora.
     */
    private static String rango(int inicio, int cantidad) {
        return inicio + ".." + (inicio + cantidad - 1);
    }
}
