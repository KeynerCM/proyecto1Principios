package com.mycompany.minipc.so.trabajos;

import java.util.List;
import java.util.function.Consumer;

import com.mycompany.minipc.excepciones.DiscoException;
import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.so.memoria.GestorMemoria;
import com.mycompany.minipc.so.memoria.Intercambio;
import com.mycompany.minipc.so.memoria.MemoriaVirtual;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.ListaProcesos;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;

/**
 * Nombre: PlanificadorTrabajos
 * Entradas: la lista de trabajos, la lista de procesos, la tabla de BCP, el
 *           gestor de memoria, la memoria virtual, el intercambio y el disco
 * Salidas: no aplica
 * Restricciones: admite en orden de llegada y sin saltarse trabajos: si el
 *                primero no cabe, los siguientes tambien esperan
 * Descripcion: el "Planificador de trabajos" del enunciado, que Stallings
 *              llama planificador de largo plazo (seccion 9.1): decide que
 *              programas de la lista de trabajos entran al sistema, y asi
 *              controla el grado de multiprogramacion, que aqui es 5. Para
 *              admitir un trabajo lee el programa del disco, le busca lugar,
 *              crea su BCP en el kernel y lo enlaza al final de la lista de
 *              procesos. El lugar es, en este orden (seccion 9.6 del plan):
 *
 *                1. la memoria principal          -> PREPARADO
 *                2. la memoria virtual del disco  -> SUSPENDIDO_PREPARADO
 *                3. ninguno: sigue NUEVO en la lista de trabajos
 *
 *              El paso 2 es la transicion New -> Ready/Suspend del libro
 *              (figura 3.9b): "there would often be insufficient room in main
 *              memory for a new process" (p. 147).
 */
public class PlanificadorTrabajos {

    private final ListaTrabajos trabajos;
    private final ListaProcesos procesos;
    private final TablaBCP tabla;
    private final GestorMemoria gestorMemoria;
    private final MemoriaVirtual memoriaVirtual;
    private final Intercambio intercambio;
    private final Disco disco;
    private final Consumer<String> bitacora;

    /**
     * Nombre: PlanificadorTrabajos
     * Entradas: trabajos, lista de trabajos; procesos, lista de procesos;
     *           tabla, tabla de BCP; gestorMemoria, administrador de la zona
     *           de usuario; memoriaVirtual, area de intercambio; intercambio,
     *           planificador de mediano plazo; disco, de donde se leen los
     *           programas; bitacora, donde se anota lo que ocurre
     * Salidas: el planificador construido
     * Restricciones: ninguna
     * Descripcion: recibe todo lo que necesita, sin crear nada propio.
     */
    public PlanificadorTrabajos(ListaTrabajos trabajos, ListaProcesos procesos, TablaBCP tabla,
            GestorMemoria gestorMemoria, MemoriaVirtual memoriaVirtual, Intercambio intercambio,
            Disco disco, Consumer<String> bitacora) {
        this.trabajos = trabajos;
        this.procesos = procesos;
        this.tabla = tabla;
        this.gestorMemoria = gestorMemoria;
        this.memoriaVirtual = memoriaVirtual;
        this.intercambio = intercambio;
        this.disco = disco;
        this.bitacora = bitacora;
    }

    /**
     * Nombre: admitir
     * Entradas: reloj, segundo simulado actual
     * Salidas: cuantos trabajos admitio
     * Restricciones: se detiene al llegar a 5 procesos, al quedarse sin
     *                trabajos nuevos o cuando el siguiente no cabe en ninguna
     *                de las dos memorias
     * Descripcion: primero vuelven a memoria los procesos suspendidos que
     *              quepan, porque llegaron antes; despues entran los trabajos
     *              NUEVO; al final el intercambio revisa si hace falta
     *              suspender a un proceso en espera.
     */
    public int admitir(int reloj) {
        intercambio.traerSuspendidos();
        int admitidos = 0;
        Trabajo trabajo = trabajos.siguienteNuevo();
        while (trabajo != null && tabla.hayRanuraLibre()) {
            if (!admitirUno(trabajo, reloj)) {
                break;
            }
            admitidos++;
            trabajo = trabajos.siguienteNuevo();
        }
        intercambio.equilibrar();
        return admitidos;
    }

    /**
     * Nombre: admitirUno
     * Entradas: trabajo, el primer trabajo NUEVO; reloj, segundo actual
     * Salidas: true si se admitio, false si no cabe en ninguna memoria
     * Restricciones: el aviso de falta de espacio se escribe una sola vez; si
     *                hay procesos suspendidos esperando volver, el trabajo
     *                nuevo no se les adelanta y va a la memoria virtual
     * Descripcion: disco -> memoria (principal o virtual) -> BCP -> lista de
     *              procesos.
     */
    private boolean admitirUno(Trabajo trabajo, int reloj) {
        List<String> lineas;
        try {
            lineas = disco.leerPrograma(trabajo.getPrograma());
        } catch (DiscoException e) {
            throw new IllegalStateException("El programa de P" + trabajo.getPid()
                    + " ya no esta en el disco", e);
        }
        int base = intercambio.hayEsperandoMemoria() ? -1 : gestorMemoria.asignar(lineas);
        boolean enMemoriaVirtual = base < 0;
        if (enMemoriaVirtual) {
            base = memoriaVirtual.guardar(lineas);
        }
        if (base < 0) {
            if (!trabajo.isEsperandoMemoria()) {
                bitacora.accept("Planificador de trabajos: P" + trabajo.getPid() + " ("
                        + trabajo.getPrograma() + ") necesita " + lineas.size()
                        + " posiciones y no caben ni en la memoria principal (bloque libre"
                        + " mayor: " + gestorMemoria.mayorBloqueLibre() + ") ni en la"
                        + " memoria virtual (" + memoriaVirtual.mayorBloqueLibre() + ");"
                        + " espera en la lista de trabajos.");
                trabajo.setEsperandoMemoria(true);
            }
            return false;
        }
        Proceso proceso = tabla.crear(trabajo.getPid(), trabajo.getPrograma(), base,
                lineas.size(), reloj);
        procesos.agregarAlFinal(proceso);
        trabajo.admitir(proceso, reloj);
        String rango = base + ".." + (base + lineas.size() - 1);
        if (enMemoriaVirtual) {
            proceso.setEstado(EstadoProceso.SUSPENDIDO_PREPARADO);
            bitacora.accept("Planificador de trabajos: admite " + proceso + " ("
                    + trabajo.getPrograma() + "). BCP en " + proceso.getDireccionBCP() + ".."
                    + proceso.getDireccionFinBCP() + ". No hay lugar en la memoria principal;"
                    + " el programa pasa a la memoria virtual (disco " + rango + ")."
                    + " Pasa a SUSPENDIDO_PREPARADO.");
        } else {
            bitacora.accept("Planificador de trabajos: admite " + proceso + " ("
                    + trabajo.getPrograma() + "). BCP en " + proceso.getDireccionBCP() + ".."
                    + proceso.getDireccionFinBCP() + ", programa en " + rango
                    + ". Pasa a PREPARADO.");
        }
        return true;
    }
}
