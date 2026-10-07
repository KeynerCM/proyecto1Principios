package com.mycompany.minipc.so.memoria;

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
 *              memoria virtual, que hace el planificador de mediano plazo.
 *              Aplica estas transiciones:
 *
 *                EN_ESPERA            -> SUSPENDIDO_EN_ESPERA (suspender)
 *                SUSPENDIDO_PREPARADO -> PREPARADO            (traer)
 *
 *              La de NUEVO -> SUSPENDIDO_PREPARADO la hace el planificador de
 *              trabajos al admitir, y SUSPENDIDO_EN_ESPERA ->
 *              SUSPENDIDO_PREPARADO la hace el manejador de interrupciones
 *              cuando llega el valor del teclado. Al mover la imagen se
 *              reubica el proceso: cambian su BASE y su PC.
 *
 *              Como la CPU espera al proceso que pidio el teclado,
 *              equilibrar solo usa traer; suspender no se llama durante la
 *              ejecucion (ver equilibrar).
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
     * Descripcion: trae los SUSPENDIDO_PREPARADO que quepan. No suspende a un
     *              proceso bloqueado para hacer lugar: la CPU espera al proceso
     *              que pidio el teclado y nadie mas puede ejecutarse mientras
     *              tanto, asi que sacarlo de memoria no ayudaria.
     */
    public void equilibrar() {
        traerSuspendidos();
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
