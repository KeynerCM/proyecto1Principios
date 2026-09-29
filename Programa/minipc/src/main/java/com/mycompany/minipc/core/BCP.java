package com.mycompany.minipc.core;

import com.mycompany.minipc.isa.RegistroID;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Nombre: BCP
 * Entradas: el identificador del proceso y el nombre del programa cargado
 * Salidas: no aplica
 * Restricciones: en este simulador solo hay un proceso a la vez, asi que el
 *                BCP no se guarda en una cola de procesos
 * Descripcion: Bloque de Control de Proceso. Reune todo lo que el sistema
 *              operativo necesitaria para suspender el proceso y reanudarlo
 *              mas tarde exactamente donde quedo: su identidad, su estado, el
 *              contexto del procesador, donde vive en memoria y su
 *              contabilidad. Se mantiene actualizado despues de cada
 *              instruccion para que la interfaz lo muestre paso a paso.
 */
public class BCP {

    private int pid;
    private String nombrePrograma;
    private EstadoProceso estado;

    // Contexto del procesador
    private int pc;
    private String irTexto;
    private int ac;
    private boolean zf;
    private final Map<RegistroID, Integer> snapshotRegistros;

    // Informacion de la pila: valores del fondo al tope
    private List<Integer> pila;

    // Informacion de memoria
    private int direccionBase;
    private int limite;

    // Contabilidad
    private int instruccionesEjecutadas;
    private int ciclosReloj;
    private final LocalDateTime horaCreacion;

    /**
     * Nombre: BCP
     * Entradas: pid, identificador del proceso; nombrePrograma, nombre del
     *           archivo cargado
     * Salidas: el bloque construido, en estado NUEVO
     * Restricciones: la hora de creacion se fija aqui y no cambia mas
     * Descripcion: crea el bloque de control de un proceso recien admitido,
     *              con los cuatro registros en cero y sin contexto todavia.
     */
    public BCP(int pid, String nombrePrograma) {
        this.pid = pid;
        this.nombrePrograma = nombrePrograma;
        this.estado = EstadoProceso.NUEVO;
        this.snapshotRegistros = new EnumMap<>(RegistroID.class);
        for (RegistroID id : RegistroID.values()) {
            snapshotRegistros.put(id, 0);
        }
        this.irTexto = "";
        this.pila = List.of();
        this.horaCreacion = LocalDateTime.now();
    }

    /**
     * Nombre: actualizarDesde
     * Entradas: cpu, procesador del que se toma el contexto
     * Salidas: ninguna
     * Restricciones: el procesador no debe ser nulo
     * Descripcion: copia el contexto actual del procesador dentro del bloque.
     *              Es el equivalente al guardado de contexto que hace el
     *              sistema operativo en un cambio de proceso. Los registros se
     *              copian por valor, de modo que el BCP conserva una foto del
     *              momento y no sigue cambiando con el procesador.
     */
    public void actualizarDesde(Procesador cpu) {
        this.estado = cpu.getEstado();
        this.pc = cpu.getPc();
        this.irTexto = cpu.getIrTexto();
        this.ac = cpu.getAc();
        this.zf = cpu.getZf();
        this.snapshotRegistros.putAll(cpu.getRegistros().instantanea());
        this.pila = List.copyOf(cpu.getPila().getValores());
        this.direccionBase = cpu.getDireccionBase();
        this.limite = cpu.getLimite();
        this.instruccionesEjecutadas = cpu.getInstruccionesEjecutadas();
        this.ciclosReloj = cpu.getCiclosReloj();
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
     * Nombre: setPid
     * Entradas: pid, nuevo identificador
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: permite reasignar el identificador, previsto para el caso
     *              de reutilizar un bloque existente.
     */
    public void setPid(int pid) {
        this.pid = pid;
    }

    /**
     * Nombre: getNombrePrograma
     * Entradas: ninguna
     * Salidas: el nombre del archivo cargado
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public String getNombrePrograma() {
        return nombrePrograma;
    }

    /**
     * Nombre: setNombrePrograma
     * Entradas: nombrePrograma, nuevo nombre de archivo
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: permite reasignar el nombre del programa asociado.
     */
    public void setNombrePrograma(String nombrePrograma) {
        this.nombrePrograma = nombrePrograma;
    }

    /**
     * Nombre: getEstado
     * Entradas: ninguna
     * Salidas: el estado en que quedo el proceso
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public EstadoProceso getEstado() {
        return estado;
    }

    /**
     * Nombre: setEstado
     * Entradas: estado, nuevo estado del proceso
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: permite fijar el estado sin tener que copiar todo el
     *              contexto del procesador.
     */
    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    /**
     * Nombre: getPc
     * Entradas: ninguna
     * Salidas: la direccion de la proxima instruccion
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al contador de programa guardado.
     */
    public int getPc() {
        return pc;
    }

    /**
     * Nombre: getIrTexto
     * Entradas: ninguna
     * Salidas: la instruccion del IR en texto legible
     * Restricciones: puede ser cadena vacia si todavia no se ejecuto nada
     * Descripcion: devuelve por ejemplo "MOV AX, 5", que es lo que se muestra
     *              del IR en el panel del BCP.
     */
    public String getIrTexto() {
        return irTexto;
    }

    /**
     * Nombre: getAc
     * Entradas: ninguna
     * Salidas: el contenido del acumulador
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al acumulador guardado.
     */
    public int getAc() {
        return ac;
    }

    /**
     * Nombre: getZf
     * Entradas: ninguna
     * Salidas: el valor guardado de la bandera de cero
     * Restricciones: ninguna
     * Descripcion: forma parte del contexto: si el proceso se interrumpe
     *              entre un CMP y su JE, la bandera debe volver igual.
     */
    public boolean getZf() {
        return zf;
    }

    /**
     * Nombre: getPila
     * Entradas: ninguna
     * Salidas: los valores guardados de la pila, del fondo al tope
     * Restricciones: la lista es inmutable
     * Descripcion: el enunciado pide que el BCP guarde la informacion de la
     *              pila del proceso.
     */
    public List<Integer> getPila() {
        return pila;
    }

    /**
     * Nombre: getRegistro
     * Entradas: id, registro a consultar
     * Salidas: el valor que tenia al momento de la ultima actualizacion
     * Restricciones: devuelve cero si el registro no figura, nunca falla
     * Descripcion: lo usa la ventana para llenar los cuatro campos de
     *              registros del panel del BCP.
     */
    public int getRegistro(RegistroID id) {
        return snapshotRegistros.getOrDefault(id, 0);
    }

    /**
     * Nombre: getSnapshotRegistros
     * Entradas: ninguna
     * Salidas: copia de los cuatro registros guardados
     * Restricciones: la copia es independiente; modificarla no altera el BCP
     * Descripcion: permite recorrer todos los registros de una vez en lugar de
     *              pedirlos uno por uno.
     */
    public Map<RegistroID, Integer> getSnapshotRegistros() {
        return new EnumMap<>(snapshotRegistros);
    }

    /**
     * Nombre: getDireccionBase
     * Entradas: ninguna
     * Salidas: la direccion donde arranca el programa en memoria
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getDireccionBase() {
        return direccionBase;
    }

    /**
     * Nombre: getLimite
     * Entradas: ninguna
     * Salidas: cuantas posiciones de memoria ocupa el programa
     * Restricciones: ninguna
     * Descripcion: junto con la direccion base delimita la region del proceso,
     *              que es el par que un sistema operativo real usaria para
     *              comprobar los accesos.
     */
    public int getLimite() {
        return limite;
    }

    /**
     * Nombre: getInstruccionesEjecutadas
     * Entradas: ninguna
     * Salidas: cuantas instrucciones lleva ejecutadas el proceso
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al contador correspondiente.
     */
    public int getInstruccionesEjecutadas() {
        return instruccionesEjecutadas;
    }

    /**
     * Nombre: getCiclosReloj
     * Entradas: ninguna
     * Salidas: cuantos ciclos consumio el proceso
     * Restricciones: cada instruccion consume dos ciclos, uno de fetch y uno
     *                de execute
     * Descripcion: acceso de solo lectura al contador correspondiente.
     */
    public int getCiclosReloj() {
        return ciclosReloj;
    }

    /**
     * Nombre: getHoraCreacion
     * Entradas: ninguna
     * Salidas: el instante en que el proceso fue admitido
     * Restricciones: se fija al construir y no cambia
     * Descripcion: es uno de los atributos que un BCP real guarda para la
     *              contabilidad del sistema.
     */
    public LocalDateTime getHoraCreacion() {
        return horaCreacion;
    }
}
