package com.mycompany.minipc.so.trabajos;

import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: Trabajo
 * Entradas: el PID asignado y el nombre del programa guardado en el disco
 * Salidas: no aplica
 * Restricciones: mientras el trabajo esta admitido, su estado se lee del BCP
 *                en memoria, no de este objeto
 * Descripcion: una fila de la lista de trabajos: un programa del disco que
 *              espera ser admitido (NUEVO), que ya es un proceso con BCP, o
 *              que termino. Mientras es un proceso, solo guarda la referencia
 *              a su Proceso, que lee todo de memoria. Al terminar, el BCP se
 *              libera para otro proceso, asi que aqui quedan las horas de
 *              inicio y fin y el tiempo de CPU para las estadisticas.
 */
public class Trabajo {

    private final int pid;
    private final String programa;
    private Proceso proceso;
    private EstadoProceso estadoSinBcp;
    private String error;
    private int inicio;
    private int fin;
    private int tiempoCpu;
    private boolean esperandoMemoria;

    /**
     * Nombre: Trabajo
     * Entradas: pid, identificador del futuro proceso; programa, nombre del
     *           archivo en el disco
     * Salidas: el trabajo construido, en estado NUEVO
     * Restricciones: ninguna
     * Descripcion: crea la fila de la lista de trabajos.
     */
    public Trabajo(int pid, String programa) {
        this.pid = pid;
        this.programa = programa;
        reiniciar();
    }

    /**
     * Nombre: admitir
     * Entradas: proceso, el proceso creado para este trabajo; reloj, segundo
     *           simulado en que se admite
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: desde ahora el estado se lee del BCP del proceso.
     */
    public void admitir(Proceso proceso, int reloj) {
        this.proceso = proceso;
        this.inicio = reloj;
        this.esperandoMemoria = false;
    }

    /**
     * Nombre: finalizar
     * Entradas: reloj, segundo simulado en que termino; tiempoCpu, segundos de
     *           CPU que uso; error, motivo si termino por un error, o nulo
     * Salidas: ninguna
     * Restricciones: se llama antes de liberar el BCP
     * Descripcion: guarda los datos que se necesitan despues de liberar el BCP.
     */
    public void finalizar(int reloj, int tiempoCpu, String error) {
        this.proceso = null;
        this.estadoSinBcp = EstadoProceso.FINALIZADO;
        this.fin = reloj;
        this.tiempoCpu = tiempoCpu;
        this.error = error;
    }

    /**
     * Nombre: reiniciar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: es final porque el constructor la invoca
     * Descripcion: vuelve el trabajo a NUEVO, para ejecutarlo otra vez.
     */
    public final void reiniciar() {
        this.proceso = null;
        this.estadoSinBcp = EstadoProceso.NUEVO;
        this.error = null;
        this.inicio = -1;
        this.fin = -1;
        this.tiempoCpu = 0;
        this.esperandoMemoria = false;
    }

    /**
     * Nombre: getEstado
     * Entradas: ninguna
     * Salidas: el estado del trabajo
     * Restricciones: ninguna
     * Descripcion: si es un proceso, lo lee de su BCP en memoria; si no, es
     *              NUEVO o FINALIZADO.
     */
    public EstadoProceso getEstado() {
        return proceso != null ? proceso.getEstado() : estadoSinBcp;
    }

    /**
     * Nombre: getPid
     * Entradas: ninguna
     * Salidas: el identificador del proceso
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getPid() {
        return pid;
    }

    /**
     * Nombre: getPrograma
     * Entradas: ninguna
     * Salidas: el nombre del archivo en el disco
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public String getPrograma() {
        return programa;
    }

    /**
     * Nombre: getProceso
     * Entradas: ninguna
     * Salidas: el proceso, o nulo si todavia no se admite o ya termino
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Proceso getProceso() {
        return proceso;
    }

    /**
     * Nombre: getError
     * Entradas: ninguna
     * Salidas: el motivo si termino por un error, o nulo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public String getError() {
        return error;
    }

    /**
     * Nombre: getInicio
     * Entradas: ninguna
     * Salidas: el segundo simulado en que se admitio, o -1
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getInicio() {
        return inicio;
    }

    /**
     * Nombre: getFin
     * Entradas: ninguna
     * Salidas: el segundo simulado en que termino, o -1
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getFin() {
        return fin;
    }

    /**
     * Nombre: getTiempoCpu
     * Entradas: ninguna
     * Salidas: los segundos de CPU que uso, leidos del BCP si sigue activo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al dato correspondiente.
     */
    public int getTiempoCpu() {
        return proceso != null ? proceso.getTiempoEmpleado() : tiempoCpu;
    }

    /**
     * Nombre: isEsperandoMemoria
     * Entradas: ninguna
     * Salidas: true si ya se intento admitir y no habia espacio
     * Restricciones: ninguna
     * Descripcion: evita repetir el mismo aviso en la consola cada segundo.
     */
    public boolean isEsperandoMemoria() {
        return esperandoMemoria;
    }

    /**
     * Nombre: setEsperandoMemoria
     * Entradas: esperando, si quedo esperando espacio
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: lo marca el planificador de trabajos.
     */
    public void setEsperandoMemoria(boolean esperando) {
        this.esperandoMemoria = esperando;
    }
}
