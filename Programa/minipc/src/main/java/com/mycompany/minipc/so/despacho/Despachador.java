package com.mycompany.minipc.so.despacho;

import java.util.function.Consumer;

import com.mycompany.minipc.hardware.Procesador;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.ListaProcesos;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;

/**
 * Nombre: Despachador
 * Entradas: la CPU, la tabla de BCP, la lista de procesos y el cambio de
 *           contexto
 * Salidas: no aplica
 * Restricciones: no elige a quien darle la CPU; eso lo decide el planificador
 *                de procesos
 * Descripcion: el "Despachador" del enunciado: le quita la CPU a un proceso y
 *              se la da a otro. Sigue los pasos del cambio de proceso de
 *              Stallings (seccion 3.4): al sacar un proceso guarda su contexto
 *              en el BCP, actualiza su estado y lo mueve a la cola que
 *              corresponde; al darle la CPU a otro, actualiza su estado, carga
 *              sus registros base y alcance y restaura su contexto desde el
 *              BCP en la memoria del kernel. Cada paso queda en la bitacora.
 */
public class Despachador {

    /** Nombre de la unica CPU, que se anota en el BCP. */
    public static final String NOMBRE_CPU = "CPU1";

    private final Procesador cpu;
    private final TablaBCP tabla;
    private final ListaProcesos procesos;
    private final CambioContexto cambioContexto;
    private final Consumer<String> bitacora;

    /**
     * Nombre: Despachador
     * Entradas: cpu, procesador; tabla, tabla de BCP; procesos, lista de
     *           procesos; cambioContexto, quien copia los registros;
     *           bitacora, donde se anotan los pasos
     * Salidas: el despachador construido
     * Restricciones: ninguna
     * Descripcion: recibe todo lo que necesita, sin crear nada propio.
     */
    public Despachador(Procesador cpu, TablaBCP tabla, ListaProcesos procesos,
            CambioContexto cambioContexto, Consumer<String> bitacora) {
        this.cpu = cpu;
        this.tabla = tabla;
        this.procesos = procesos;
        this.cambioContexto = cambioContexto;
        this.bitacora = bitacora;
    }

    /**
     * Nombre: despachar
     * Entradas: entrante, proceso elegido por el planificador
     * Salidas: ninguna
     * Restricciones: la CPU debe estar libre
     * Descripcion: le da la CPU al proceso: EJECUCION, CPU asignada, puntero
     *              "en ejecucion" de la cabecera y contexto restaurado.
     */
    public void despachar(Proceso entrante) {
        entrante.setEstado(EstadoProceso.EJECUCION);
        entrante.setCpu(NOMBRE_CPU);
        tabla.setEnEjecucion(entrante);
        cambioContexto.restaurar(entrante, cpu);
        bitacora.accept("Despachador: " + entrante + " pasa a EJECUCION en " + NOMBRE_CPU
                + ". Restaura su contexto desde el BCP (" + entrante.getDireccionBCP() + ".."
                + entrante.getDireccionFinBCP() + "): PC=" + entrante.getPc() + ", base="
                + entrante.getBase() + ", alcance=" + entrante.getAlcance() + ".");
    }

    /**
     * Nombre: sacar
     * Entradas: nuevoEstado, estado al que pasa el proceso en ejecucion
     * Salidas: el proceso que salio de la CPU, o nulo si estaba libre
     * Restricciones: ninguna
     * Descripcion: guarda el contexto en el BCP, cambia el estado y libera la
     *              CPU. Si vuelve a PREPARADO, pasa al final de la cola.
     */
    public Proceso sacar(EstadoProceso nuevoEstado) {
        Proceso saliente = tabla.getEnEjecucion();
        if (saliente == null) {
            return null;
        }
        cambioContexto.guardar(cpu, saliente);
        saliente.setEstado(nuevoEstado);
        saliente.setCpu("");
        if (nuevoEstado == EstadoProceso.PREPARADO) {
            procesos.moverAlFinal(saliente);
        }
        tabla.setEnEjecucion(null);
        cpu.limpiar();
        bitacora.accept("Despachador: guarda el contexto de " + saliente + " en su BCP (PC="
                + saliente.getPc() + ", AC=" + saliente.getAc() + ") y pasa a " + nuevoEstado
                + ". La CPU queda libre.");
        return saliente;
    }

    /**
     * Nombre: actualizarBCP
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no hace nada si la CPU esta libre
     * Descripcion: copia el contexto del proceso en ejecucion a su BCP sin
     *              quitarle la CPU. El sistema operativo lo hace al final de
     *              cada segundo para que el BCP que se ve en memoria siempre
     *              este al dia.
     */
    public void actualizarBCP() {
        Proceso actual = tabla.getEnEjecucion();
        if (actual != null) {
            cambioContexto.guardar(cpu, actual);
        }
    }
}
