package com.mycompany.minipc.hardware;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.minipc.excepciones.DesbordamientoException;
import com.mycompany.minipc.excepciones.EjecucionException;
import com.mycompany.minipc.excepciones.MemoriaInsuficienteException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.isa.Ensamblador;
import com.mycompany.minipc.isa.Forma;
import com.mycompany.minipc.isa.Instruccion;
import com.mycompany.minipc.isa.RegistroID;
import com.mycompany.minipc.so.procesos.BCP;
import com.mycompany.minipc.so.procesos.EstadoProceso;

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
 *              execute). La memoria guarda cada instruccion como texto; el IR
 *              recibe ese texto y la CPU lo decodifica antes de ejecutarlo.
 *              La aritmetica se resuelve sobre enteros de Java.
 */
public class Procesador {

    private final Memoria memoria;
    private final BancoRegistros registros;
    private final Estadisticas estadisticas;
    private final List<ObservadorCPU> observadores;

    /** Decodificador: interpreta el texto que trae el fetch. */
    private final Ensamblador decodificador;

    private BCP bcp;

    /** Program Counter: direccion de la proxima instruccion. */
    private int pc;

    /** Instruction Register: el texto de la instruccion traida de memoria. */
    private String irTexto;

    /** La instruccion del IR ya decodificada, o nulo si no se pudo. */
    private Instruccion ir;

    /** Accumulator: almacenamiento temporal donde ocurre la aritmetica. */
    private int ac;

    /**
     * Bandera de cero: resultado de la ultima comparacion CMP. Es el codigo
     * de condicion que Stallings ubica en la palabra de estado (PSW) y que
     * consultan JE y JNE.
     */
    private boolean zf;

    /** Pila del proceso, de capacidad 5. */
    private final Pila pila;

    /** Se activa al ejecutar INT 20H, para terminar el proceso. */
    private boolean finSolicitado;

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
        this.pila = new Pila();
        this.decodificador = new Ensamblador();
        this.irTexto = "";
        this.observadores = new ArrayList<>();
        this.siguientePid = 1;
        this.estado = EstadoProceso.NUEVO;
    }

    /**
     * Nombre: cargar
     * Entradas: lineas, texto de cada instruccion del programa; nombreArchivo,
     *           nombre del archivo de origen para el BCP
     * Salidas: ninguna; avisa a los observadores con la fase CARGA
     * Restricciones: lanza MemoriaInsuficienteException si el programa no cabe
     *                en la zona de usuario, y en ese caso nada cambia
     * Descripcion: carga un programa en memoria y deja el procesador listo
     *              para ejecutarlo desde la primera instruccion. Pone los
     *              registros y los contadores en cero y crea un BCP nuevo con
     *              el siguiente identificador de proceso.
     */
    public void cargar(List<String> lineas, String nombreArchivo)
            throws MemoriaInsuficienteException {
        direccionBase = memoria.cargarPrograma(lineas);
        cantidadInstrucciones = lineas.size();
        direccionFin = direccionBase + cantidadInstrucciones - 1;

        registros.reset();
        estadisticas.reset();
        estadisticas.setPosicionesUsadas(memoria.getPosicionesUsadas());

        pc = direccionBase;
        irTexto = "";
        ir = null;
        ac = 0;
        zf = false;
        pila.vaciar();
        finSolicitado = false;
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
     *                proceso ya termino; lanza EjecucionException si la
     *                instruccion provoca un error de ejecucion, dejando el
     *                proceso en BLOQUEADO_ERROR
     * Descripcion: ejecuta una sola instruccion, es decir un ciclo de fetch
     *              mas execute completo. El fetch trae el texto de la celda
     *              que apunta el PC al IR; el execute lo decodifica y lo
     *              ejecuta. El PC se incrementa en la etapa de fetch, igual
     *              que en el libro, de modo que durante la ejecucion ya apunta
     *              a la instruccion siguiente. Avisa a los observadores dos
     *              veces, una por etapa, para que la interfaz pueda mostrar el
     *              ciclo separado.
     */
    public boolean paso() {
        if (!hayPrograma() || estado.esFinal()) {
            return false;
        }
        estado = EstadoProceso.EJECUCION;

        // ---------- ETAPA FETCH ----------
        int direccion = pc;
        irTexto = memoria.leerComoUsuario(direccion);
        ir = null;
        pc++;
        ciclosReloj++;
        estadisticas.registrarLectura();
        bcp.actualizarDesde(this);
        notificar(Fase.FETCH);

        // ---------- ETAPA EXECUTE (decodifica y ejecuta) ----------
        try {
            ir = decodificar(direccion, irTexto);
            ejecutar(ir);
        } catch (EjecucionException e) {
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
        estadisticas.registrar(ir.getOpcode());

        if (finSolicitado || pc > direccionFin) {
            estado = EstadoProceso.TERMINADO;
        }

        bcp.actualizarDesde(this);
        notificar(Fase.EXECUTE);

        return !haTerminado();
    }

    /**
     * Nombre: decodificar
     * Entradas: direccion, posicion de donde se trajo el texto; texto,
     *           contenido del IR
     * Salidas: la instruccion lista para ejecutar
     * Restricciones: lanza EjecucionException si el texto no es una
     *                instruccion valida, por ejemplo si el PC llego a una
     *                celda vacia o con un numero
     * Descripcion: interpreta el texto con las mismas reglas del
     *              ensamblador. Un fallo aqui es un error del proceso (trap),
     *              no un error de sintaxis del archivo, que ya se valido al
     *              cargarlo.
     */
    private Instruccion decodificar(int direccion, String texto) {
        try {
            return decodificador.decodificar(texto);
        } catch (SintaxisException e) {
            throw new EjecucionException("La posicion " + direccion
                    + " no contiene una instruccion valida: \"" + texto + "\"");
        }
    }

    /**
     * Nombre: ejecutar
     * Entradas: instruccion, la que esta en el IR
     * Salidas: ninguna; modifica el acumulador, los registros, la bandera de
     *          cero, la pila o el PC
     * Restricciones: lanza EjecucionException si un salto sale del programa,
     *                si la pila se desborda o queda vacia, o si la
     *                interrupcion todavia no esta disponible
     * Descripcion: etapa de ejecucion propiamente dicha. Cada operacion se
     *              resuelve con aritmetica normal de enteros de Java.
     */
    private void ejecutar(Instruccion instruccion) {
        switch (instruccion.getOpcode()) {
            case MOV:
                int valor = instruccion.getForma() == Forma.REGISTRO_REGISTRO
                        ? registros.leer(instruccion.getRegistro(1))
                        : instruccion.getValor(1);
                escribirRegistro(instruccion.getRegistro(0), valor);
                break;
            case LOAD:
                ac = registros.leer(instruccion.getRegistro(0));
                break;
            case STORE:
                escribirRegistro(instruccion.getRegistro(0), ac);
                break;
            case ADD:
                ac = ac + registros.leer(instruccion.getRegistro(0));
                break;
            case SUB:
                ac = ac - registros.leer(instruccion.getRegistro(0));
                break;
            case INC:
                sumarUno(instruccion, 1);
                break;
            case DEC:
                sumarUno(instruccion, -1);
                break;
            case SWAP:
                RegistroID primero = instruccion.getRegistro(0);
                RegistroID segundo = instruccion.getRegistro(1);
                int temporal = registros.leer(primero);
                escribirRegistro(primero, registros.leer(segundo));
                escribirRegistro(segundo, temporal);
                break;
            case CMP:
                zf = registros.leer(instruccion.getRegistro(0))
                        == registros.leer(instruccion.getRegistro(1));
                break;
            case JMP:
                saltar(instruccion);
                break;
            case JE:
                if (zf) {
                    saltar(instruccion);
                }
                break;
            case JNE:
                if (!zf) {
                    saltar(instruccion);
                }
                break;
            case PUSH:
                pila.apilar(registros.leer(instruccion.getRegistro(0)));
                break;
            case POP:
                escribirRegistro(instruccion.getRegistro(0), pila.desapilar());
                break;
            case PARAM:
                apilarParametros(instruccion);
                break;
            case INT:
                interrumpir(instruccion);
                break;
            default:
                throw new EjecucionException("La instruccion \"" + instruccion
                        + "\" no se puede ejecutar");
        }
    }

    /**
     * Nombre: escribirRegistro
     * Entradas: id, registro destino; valor, entero a guardar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: escribe el registro y lo anota en las estadisticas, para
     *              no repetir las dos lineas en cada operacion que escribe.
     */
    private void escribirRegistro(RegistroID id, int valor) {
        registros.escribir(id, valor);
        estadisticas.registrarEscritura();
    }

    /**
     * Nombre: sumarUno
     * Entradas: instruccion, INC o DEC; delta, 1 para incrementar o -1 para
     *           decrementar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: sin operandos actua sobre el AC; con un registro, sobre
     *              ese registro, como pide el enunciado.
     */
    private void sumarUno(Instruccion instruccion, int delta) {
        if (instruccion.getForma() == Forma.SIN_OPERANDOS) {
            ac = ac + delta;
        } else {
            RegistroID id = instruccion.getRegistro(0);
            escribirRegistro(id, registros.leer(id) + delta);
        }
    }

    /**
     * Nombre: saltar
     * Entradas: instruccion, JMP, JE o JNE con su desplazamiento
     * Salidas: ninguna; cambia el PC
     * Restricciones: lanza DesbordamientoException si el destino queda fuera
     *                de la region del proceso
     * Descripcion: el desplazamiento se suma al PC, que ya apunta a la
     *              instruccion siguiente porque avanzo en la etapa de fetch.
     *              Antes de saltar se compara el destino con la direccion
     *              base y el limite del proceso, como hace el hardware de
     *              reubicacion del libro (Stallings, figura 7.8): un salto
     *              fuera de la region genera una interrupcion hacia el
     *              sistema operativo.
     */
    private void saltar(Instruccion instruccion) {
        int destino = pc + instruccion.getValor(0);
        if (destino < direccionBase || destino > direccionFin) {
            throw new DesbordamientoException("Desbordamiento: el salto \"" + instruccion
                    + "\" lleva a la direccion " + destino + ", fuera del programa (direcciones "
                    + direccionBase + " a " + direccionFin + ")");
        }
        pc = destino;
    }

    /**
     * Nombre: apilarParametros
     * Entradas: instruccion, PARAM con uno a tres valores
     * Salidas: ninguna
     * Restricciones: lanza DesbordamientoException si no caben todos los
     *                valores; en ese caso no se apila ninguno
     * Descripcion: guarda los valores en la pila en el orden en que se
     *              escribieron, de modo que el ultimo queda en el tope y es
     *              el primero que devuelve POP.
     */
    private void apilarParametros(Instruccion instruccion) {
        int cantidad = instruccion.getOperandos().size();
        if (cantidad > pila.getLibres()) {
            throw new DesbordamientoException("Desbordamiento de pila: \"" + instruccion
                    + "\" necesita " + cantidad + " posiciones y la pila solo tiene "
                    + pila.getLibres() + " libres de " + Pila.CAPACIDAD);
        }
        for (int i = 0; i < cantidad; i++) {
            pila.apilar(instruccion.getValor(i));
        }
    }

    /**
     * Nombre: interrumpir
     * Entradas: instruccion, INT con su codigo
     * Salidas: ninguna
     * Restricciones: lanza EjecucionException para las interrupciones de
     *                pantalla, teclado y archivos, que todavia no estan
     *                disponibles
     * Descripcion: INT 20H termina el proceso. Las demas necesitan los
     *              dispositivos de entrada y salida del simulador.
     */
    private void interrumpir(Instruccion instruccion) {
        if (instruccion.esFinDePrograma()) {
            finSolicitado = true;
            return;
        }
        throw new EjecucionException("La interrupcion \"" + instruccion
                + "\" todavia no esta disponible en esta version del simulador");
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
        irTexto = "";
        ir = null;
        ac = 0;
        zf = false;
        pila.vaciar();
        finSolicitado = false;
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
        irTexto = "";
        ir = null;
        ac = 0;
        zf = false;
        pila.vaciar();
        finSolicitado = false;
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
     * Salidas: la instruccion en curso ya decodificada, o nulo si no se ha
     *          ejecutado nada o si el texto del IR no era una instruccion
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
        return irTexto;
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
     * Nombre: getZf
     * Entradas: ninguna
     * Salidas: true si la ultima comparacion CMP dio igual
     * Restricciones: es false mientras no se haya comparado nada
     * Descripcion: acceso de solo lectura a la bandera de cero.
     */
    public boolean getZf() {
        return zf;
    }

    /**
     * Nombre: getPila
     * Entradas: ninguna
     * Salidas: la pila del proceso
     * Restricciones: ninguna
     * Descripcion: el BCP la copia para guardar el contexto; las pruebas la
     *              consultan para verificar PUSH, POP y PARAM.
     */
    public Pila getPila() {
        return pila;
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
