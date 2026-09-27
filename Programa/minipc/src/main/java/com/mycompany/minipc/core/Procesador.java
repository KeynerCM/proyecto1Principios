package com.mycompany.minipc.core;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.minipc.excepciones.DesbordamientoException;
import com.mycompany.minipc.excepciones.MemoriaInsuficienteException;
import com.mycompany.minipc.isa.Instruccion;
import com.mycompany.minipc.isa.OpCode;
import com.mycompany.minipc.isa.RegistroID;

/**
 * Nombre: Procesador
 * Entradas: el programa a ejecutar y el nombre del archivo de origen
 * Salidas: el estado del proceso tras cada instruccion, comunicado a los
 *          observadores registrados
 * Restricciones: ejecuta un solo proceso a la vez; no hay multiprogramacion
 *                ni cambio de contexto entre procesos
 * Descripcion: el procesador del Mini PC. Implementa el ciclo de instruccion:
 *              el procesador repite indefinidamente traer la instruccion que
 *              apunta el PC (etapa fetch) e interpretarla y ejecutarla (etapa
 *              execute). La memoria guarda la instruccion ya ensamblada, asi
 *              que el IR recibe el objeto directamente y la aritmetica se
 *              resuelve sobre enteros de Java.
 */
public class Procesador {

    private final Memoria memoria;
    private final BancoRegistros registros;
    private final Estadisticas estadisticas;
    private final List<ObservadorCPU> observadores;

    private BCP bcp;

    /** Program Counter: direccion de la proxima instruccion. */
    private int pc;

    /** Instruction Register: la instruccion que se esta ejecutando. */
    private Instruccion ir;

    /** Accumulator: almacenamiento temporal donde ocurre la aritmetica. */
    private int ac;

    private int direccionBase;
    private int direccionFin;
    private int cantidadInstrucciones;
    private int instruccionesEjecutadas;
    private int ciclosReloj;

    private EstadoProceso estado;
    private int siguientePid;

    /**
     * Nombre: Procesador
     * Entradas: ninguna
     * Salidas: el procesador construido, sin programa cargado
     * Restricciones: la memoria arranca con la configuracion por defecto
     * Descripcion: crea el procesador con su memoria, sus registros y sus
     *              contadores. Queda en estado NUEVO hasta que se le cargue
     *              un programa.
     */
    public Procesador() {
        this.memoria = new Memoria();
        this.registros = new BancoRegistros();
        this.estadisticas = new Estadisticas();
        this.observadores = new ArrayList<>();
        this.siguientePid = 1;
        this.estado = EstadoProceso.NUEVO;
    }

    /**
     * Nombre: cargar
     * Entradas: programa, instrucciones ya ensambladas; nombreArchivo, nombre
     *           del archivo de origen para el BCP
     * Salidas: ninguna; avisa a los observadores con la fase CARGA
     * Restricciones: lanza MemoriaInsuficienteException si el programa no cabe
     *                en la zona de usuario, y en ese caso nada cambia
     * Descripcion: carga un programa en memoria y deja el procesador listo
     *              para ejecutarlo desde la primera instruccion. Pone los
     *              registros y los contadores en cero y crea un BCP nuevo con
     *              el siguiente identificador de proceso.
     */
    public void cargar(List<Instruccion> programa, String nombreArchivo)
            throws MemoriaInsuficienteException {
        direccionBase = memoria.cargarPrograma(programa);
        cantidadInstrucciones = programa.size();
        direccionFin = direccionBase + cantidadInstrucciones - 1;

        registros.reset();
        estadisticas.reset();
        estadisticas.setPosicionesUsadas(memoria.getPosicionesUsadas());

        pc = direccionBase;
        ir = null;
        ac = 0;
        instruccionesEjecutadas = 0;
        ciclosReloj = 0;

        bcp = new BCP(siguientePid++, nombreArchivo);
        estado = EstadoProceso.LISTO;
        bcp.actualizarDesde(this);

        notificar(Fase.CARGA);
    }

    /**
     * Nombre: paso
     * Entradas: ninguna, opera sobre el estado interno del procesador
     * Salidas: true si queda al menos una instruccion por ejecutar
     * Restricciones: devuelve false sin hacer nada si no hay programa o si el
     *                proceso ya termino; lanza DesbordamientoException si la
     *                instruccion provoca un desbordamiento, dejando el
     *                proceso en BLOQUEADO_ERROR
     * Descripcion: ejecuta una sola instruccion, es decir un ciclo de fetch
     *              mas execute completo. El PC se incrementa en la etapa de
     *              fetch, igual que en el libro, de modo que durante la
     *              ejecucion ya apunta a la instruccion siguiente. Avisa a los
     *              observadores dos veces, una por etapa, para que la interfaz
     *              pueda mostrar el ciclo separado.
     */
    public boolean paso() {
        if (!hayPrograma() || estado.esFinal()) {
            return false;
        }
        estado = EstadoProceso.EJECUCION;

        // ---------- ETAPA FETCH ----------
        CeldaMemoria celda = memoria.leerComoUsuario(pc);
        ir = celda.getInstruccion();
        pc++;
        ciclosReloj++;
        estadisticas.registrarLectura();
        bcp.actualizarDesde(this);
        notificar(Fase.FETCH);

        // ---------- ETAPA DECODE ----------
        OpCode opcode = ir.getOpcode();
        RegistroID registro = ir.getRegistro();
        int operando = ir.getOperando();

        // ---------- ETAPA EXECUTE ----------
        try {
            ejecutar(opcode, registro, operando);
        } catch (DesbordamientoException e) {
            // El proceso no puede continuar, pero la interfaz tiene que
            // poder mostrar en que estado quedo antes de ver el error.
            estado = EstadoProceso.BLOQUEADO_ERROR;
            ciclosReloj++;
            bcp.actualizarDesde(this);
            notificar(Fase.EXECUTE);
            throw e;
        }

        ciclosReloj++;
        instruccionesEjecutadas++;
        estadisticas.registrar(opcode);

        if (pc > direccionFin) {
            estado = EstadoProceso.TERMINADO;
        }

        bcp.actualizarDesde(this);
        notificar(Fase.EXECUTE);

        return !haTerminado();
    }

    /**
     * Nombre: ejecutar
     * Entradas: opcode, operacion decodificada; registro, registro sobre el
     *           que opera; operando, valor inmediato
     * Salidas: ninguna; modifica el acumulador o el banco de registros
     * Restricciones: lanza IllegalStateException si aparece un opcode sin
     *                implementar
     * Descripcion: etapa de ejecucion propiamente dicha. Cada operacion se
     *              resuelve con aritmetica normal de enteros de Java.
     */
    private void ejecutar(OpCode opcode, RegistroID registro, int operando) {
        switch (opcode) {
            case MOV:
                registros.escribir(registro, operando);
                estadisticas.registrarEscritura();
                break;
            case LOAD:
                ac = registros.leer(registro);
                break;
            case STORE:
                registros.escribir(registro, ac);
                estadisticas.registrarEscritura();
                break;
            case ADD:
                ac = ac + registros.leer(registro);
                break;
            case SUB:
                ac = ac - registros.leer(registro);
                break;
            default:
                throw new IllegalStateException("Operacion no implementada: " + opcode);
        }
    }

    /**
     * Nombre: reset
     * Entradas: ninguna
     * Salidas: ninguna; avisa a los observadores con la fase REINICIO
     * Restricciones: no hace nada si no hay programa cargado
     * Descripcion: vuelve al inicio del programa sin descargarlo de memoria.
     *              Los registros, el acumulador y los contadores quedan en
     *              cero y el proceso vuelve al estado LISTO.
     */
    public void reset() {
        if (!hayPrograma()) {
            return;
        }
        registros.reset();
        estadisticas.reset();
        estadisticas.setPosicionesUsadas(memoria.getPosicionesUsadas());

        pc = direccionBase;
        ir = null;
        ac = 0;
        instruccionesEjecutadas = 0;
        ciclosReloj = 0;
        estado = EstadoProceso.LISTO;

        bcp.actualizarDesde(this);
        notificar(Fase.REINICIO);
    }

    /**
     * Nombre: limpiar
     * Entradas: ninguna
     * Salidas: ninguna; avisa a los observadores con la fase REINICIO
     * Restricciones: descarta el BCP, de modo que despues de llamarla
     *                getBcp() devuelve nulo
     * Descripcion: descarga el programa de la memoria y deja el procesador
     *              como recien arrancado.
     */
    public void limpiar() {
        memoria.limpiarZonaUsuario();
        registros.reset();
        estadisticas.reset();

        pc = 0;
        ir = null;
        ac = 0;
        direccionBase = 0;
        direccionFin = -1;
        cantidadInstrucciones = 0;
        instruccionesEjecutadas = 0;
        ciclosReloj = 0;
        bcp = null;
        estado = EstadoProceso.NUEVO;

        notificar(Fase.REINICIO);
    }

    /**
     * Nombre: configurarMemoria
     * Entradas: tamano, cantidad total de posiciones; limiteKernel, primera
     *           direccion de la zona de usuario
     * Salidas: ninguna
     * Restricciones: lanza IllegalArgumentException si los valores no son
     *                coherentes; descarga siempre el programa actual
     * Descripcion: cambia la configuracion de la memoria. El programa se
     *              descarga porque redimensionar invalida las direcciones ya
     *              asignadas.
     */
    public void configurarMemoria(int tamano, int limiteKernel) {
        memoria.redimensionar(tamano, limiteKernel);
        limpiar();
    }

    /**
     * Nombre: haTerminado
     * Entradas: ninguna
     * Salidas: true si el programa llego al final o quedo bloqueado
     * Restricciones: ninguna
     * Descripcion: agrupa los dos estados finales para que quien consulte no
     *              tenga que distinguirlos cuando no le importa la causa.
     */
    public boolean haTerminado() {
        return estado.esFinal();
    }

    /**
     * Nombre: hayPrograma
     * Entradas: ninguna
     * Salidas: true si hay un programa cargado en memoria
     * Restricciones: ninguna
     * Descripcion: exige que haya instrucciones y BCP, que son las dos cosas
     *              que limpiar() deshace, de modo que no puede dar un falso
     *              positivo tras descargar.
     */
    public boolean hayPrograma() {
        return cantidadInstrucciones > 0 && bcp != null;
    }

    /**
     * Nombre: agregarObservador
     * Entradas: observador, interesado en los cambios del procesador
     * Salidas: ninguna
     * Restricciones: ignora los nulos y los duplicados
     * Descripcion: registra a quien quiera enterarse de cada etapa del ciclo.
     */
    public void agregarObservador(ObservadorCPU observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    /**
     * Nombre: quitarObservador
     * Entradas: observador, a dar de baja
     * Salidas: ninguna
     * Restricciones: no falla si el observador no estaba registrado
     * Descripcion: deja de avisar al observador indicado.
     */
    public void quitarObservador(ObservadorCPU observador) {
        observadores.remove(observador);
    }

    /**
     * Nombre: notificar
     * Entradas: fase, momento del ciclo del que se avisa
     * Salidas: ninguna
     * Restricciones: se ejecuta en el mismo hilo que llamo a paso(), asi que
     *                los observadores deben responder rapido
     * Descripcion: recorre la lista de observadores avisandoles del cambio.
     */
    private void notificar(Fase fase) {
        for (ObservadorCPU observador : observadores) {
            observador.alCambiarEstado(this, fase);
        }
    }

    /**
     * Nombre: getMemoria
     * Entradas: ninguna
     * Salidas: la memoria del procesador
     * Restricciones: ninguna
     * Descripcion: la interfaz la necesita para dibujar la tabla de memoria.
     */
    public Memoria getMemoria() {
        return memoria;
    }

    /**
     * Nombre: getRegistros
     * Entradas: ninguna
     * Salidas: el banco de registros
     * Restricciones: ninguna
     * Descripcion: lo usa el BCP para tomar la instantanea del contexto.
     */
    public BancoRegistros getRegistros() {
        return registros;
    }

    /**
     * Nombre: getEstadisticas
     * Entradas: ninguna
     * Salidas: la contabilidad de la ejecucion
     * Restricciones: ninguna
     * Descripcion: alimenta el dialogo de estadisticas.
     */
    public Estadisticas getEstadisticas() {
        return estadisticas;
    }

    /**
     * Nombre: getBcp
     * Entradas: ninguna
     * Salidas: el bloque de control del proceso actual, o nulo si no hay
     * Restricciones: devuelve nulo tras limpiar(), asi que hay que comprobarlo
     * Descripcion: da acceso al BCP para que la interfaz muestre sus atributos.
     */
    public BCP getBcp() {
        return bcp;
    }

    /**
     * Nombre: getEstado
     * Entradas: ninguna
     * Salidas: el estado actual del proceso
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al estado.
     */
    public EstadoProceso getEstado() {
        return estado;
    }

    /**
     * Nombre: getPc
     * Entradas: ninguna
     * Salidas: la direccion de la proxima instruccion
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al contador de programa.
     */
    public int getPc() {
        return pc;
    }

    /**
     * Nombre: getIr
     * Entradas: ninguna
     * Salidas: la instruccion en curso, o nulo si no se ha ejecutado nada
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al registro de instruccion.
     */
    public Instruccion getIr() {
        return ir;
    }

    /**
     * Nombre: getIrTexto
     * Entradas: ninguna
     * Salidas: el texto legible de la instruccion en curso
     * Restricciones: es cadena vacia mientras no se haya ejecutado nada
     * Descripcion: es lo que se muestra del IR en el panel del BCP.
     */
    public String getIrTexto() {
        return ir == null ? "" : ir.getTextoFuente();
    }

    /**
     * Nombre: getAc
     * Entradas: ninguna
     * Salidas: el contenido del acumulador
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al acumulador.
     */
    public int getAc() {
        return ac;
    }

    /**
     * Nombre: getDireccionBase
     * Entradas: ninguna
     * Salidas: la direccion donde arranca el programa
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getDireccionBase() {
        return direccionBase;
    }

    /**
     * Nombre: getLimite
     * Entradas: ninguna
     * Salidas: cuantas posiciones de memoria ocupa el programa cargado
     * Restricciones: ninguna
     * Descripcion: junto con la direccion base delimita la region del proceso.
     */
    public int getLimite() {
        return cantidadInstrucciones;
    }

    /**
     * Nombre: getInstruccionesEjecutadas
     * Entradas: ninguna
     * Salidas: cuantas instrucciones lleva ejecutadas
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al contador correspondiente.
     */
    public int getInstruccionesEjecutadas() {
        return instruccionesEjecutadas;
    }

    /**
     * Nombre: getCiclosReloj
     * Entradas: ninguna
     * Salidas: cuantos ciclos consumio la ejecucion
     * Restricciones: cada instruccion consume dos, uno por etapa
     * Descripcion: acceso de solo lectura al contador correspondiente.
     */
    public int getCiclosReloj() {
        return ciclosReloj;
    }

    /**
     * Nombre: getIndiceInstruccionActual
     * Entradas: ninguna
     * Salidas: indice dentro del programa contando desde cero, o -1 si ya no
     *          queda ninguna instruccion pendiente
     * Restricciones: ninguna
     * Descripcion: traduce la direccion absoluta del PC a la posicion relativa
     *              dentro del programa, que es lo que la interfaz necesita
     *              para resaltar la fila correspondiente de la tabla.
     */
    public int getIndiceInstruccionActual() {
        if (!hayPrograma() || pc > direccionFin) {
            return -1;
        }
        return pc - direccionBase;
    }
}
