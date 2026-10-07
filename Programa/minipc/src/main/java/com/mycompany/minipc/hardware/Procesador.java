package com.mycompany.minipc.hardware;

import com.mycompany.minipc.excepciones.DesbordamientoException;
import com.mycompany.minipc.excepciones.EjecucionException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.excepciones.ViolacionProteccionException;
import com.mycompany.minipc.isa.Ensamblador;
import com.mycompany.minipc.isa.Forma;
import com.mycompany.minipc.isa.Instruccion;
import com.mycompany.minipc.isa.Interrupcion;
import com.mycompany.minipc.isa.RegistroID;

/**
 * Nombre: Procesador
 * Entradas: la memoria principal de la que trae las instrucciones
 * Salidas: el resultado de cada ciclo de instruccion
 * Restricciones: es solo hardware: no conoce procesos, BCP ni estados. El
 *                sistema operativo le carga un contexto (registros, base y
 *                alcance) antes de ejecutar y se lo lleva al BCP despues
 * Descripcion: la CPU del Mini PC. Implementa el ciclo de instruccion: traer la
 *              instruccion que apunta el PC (fetch) e interpretarla y
 *              ejecutarla (execute). La memoria guarda cada instruccion como
 *              texto; el IR recibe ese texto y la CPU lo decodifica antes de
 *              ejecutarlo. Los registros base y alcance delimitan la region del
 *              proceso en ejecucion: un salto o un PC fuera de ella es un error
 *              que termina el proceso.
 */
public class Procesador {

    /**
     * Nombre: Resultado
     * Entradas: no aplica, es una enumeracion de valores fijos
     * Salidas: no aplica
     * Restricciones: ninguna
     * Descripcion: lo que informa la CPU al terminar cada segundo.
     */
    public enum Resultado {

        /** La instruccion todavia no cumple su peso; sigue en el siguiente segundo. */
        EN_CURSO,

        /** La instruccion termino y el proceso puede seguir. */
        CONTINUA,

        /** Termino INT: el sistema operativo debe atender la llamada. */
        LLAMADA_SISTEMA,

        /** El proceso paso su ultima instruccion sin INT 20H. */
        TERMINO
    }

    private final Memoria memoria;
    private final BancoRegistros registros;
    private final Estadisticas estadisticas;
    private final Pila pila;

    /** Decodificador: interpreta el texto que trae el fetch. */
    private final Ensamblador decodificador;

    /** Program Counter: direccion de la proxima instruccion. */
    private int pc;

    /** Instruction Register: el texto de la instruccion traida de memoria. */
    private String irTexto;

    /** La instruccion del IR ya decodificada, o nulo si no se pudo. */
    private Instruccion ir;

    /** Direccion de donde se trajo la instruccion del IR. */
    private int direccionIr;

    /** Accumulator: almacenamiento temporal donde ocurre la aritmetica. */
    private int ac;

    /**
     * Bandera de cero: resultado de la ultima comparacion CMP. La consultan
     * JE y JNE.
     */
    private boolean zf;

    /** Peso de la instruccion del IR, en segundos. */
    private int pesoActual;

    /** Segundos que le faltan a la instruccion del IR; cero si no hay ninguna. */
    private int restante;

    /** Interrupcion que pidio el ultimo INT, para el sistema operativo. */
    private Interrupcion interrupcionPendiente;

    /** Registro base: primera direccion del proceso en ejecucion. */
    private int base;

    /** Registro de alcance (limite): cuantas posiciones ocupa el proceso. */
    private int alcance;

    /**
     * Nombre: Procesador
     * Entradas: memoria, memoria principal compartida con el sistema operativo
     * Salidas: el procesador construido, sin contexto cargado
     * Restricciones: la memoria no debe ser nula
     * Descripcion: crea la CPU con sus registros, su pila y sus contadores.
     */
    public Procesador(Memoria memoria) {
        this.memoria = memoria;
        this.registros = new BancoRegistros();
        this.estadisticas = new Estadisticas();
        this.pila = new Pila();
        this.decodificador = new Ensamblador();
        limpiar();
    }

    /**
     * Nombre: paso
     * Entradas: ninguna, opera sobre el contexto cargado
     * Salidas: EN_CURSO si la instruccion todavia no cumple su peso; CONTINUA
     *          si termino y el proceso sigue; LLAMADA_SISTEMA si termino un
     *          INT; TERMINO si el proceso ya no tiene instrucciones
     * Restricciones: lanza IllegalStateException si no hay contexto cargado
     *                (alcance cero); lanza EjecucionException si la
     *                instruccion provoca un error, que es una interrupcion de
     *                programa que atiende el sistema operativo
     * Descripcion: un segundo de CPU. Cada instruccion dura tantos segundos
     *              como su peso: en el primero se hace el fetch (el texto de la
     *              celda del PC pasa al IR, se decodifica y el PC avanza) y en
     *              el ultimo se aplica su efecto. Asi ADD, de peso 3, necesita
     *              tres llamadas. INT 09H tiene peso variable: dura un segundo
     *              y despues el proceso espera al teclado, cosa que decide el
     *              sistema operativo.
     */
    public Resultado paso() {
        if (alcance <= 0) {
            throw new IllegalStateException("La CPU no tiene un proceso cargado");
        }

        if (restante == 0) {
            if (pc == base + alcance) {
                return Resultado.TERMINO;
            }
            // Proteccion: el PC solo puede traer instrucciones de la region
            // del proceso, comparado con los registros base y alcance.
            if (pc < base || pc > base + alcance) {
                throw new ViolacionProteccionException("el PC (" + pc + ") salio de la region"
                        + " del proceso (" + base + " a " + (base + alcance - 1) + ")");
            }
            // ---------- ETAPA FETCH (y decodificacion) ----------
            direccionIr = pc;
            try {
                irTexto = memoria.leerComoUsuario(direccionIr);
            } catch (IllegalArgumentException e) {
                throw new ViolacionProteccionException(e.getMessage());
            }
            ir = null;
            pc++;
            estadisticas.registrarLectura();
            ir = decodificar(direccionIr, irTexto);
            pesoActual = Math.max(1, ir.getPeso());
            restante = pesoActual;
        }

        restante--;
        if (restante > 0) {
            return Resultado.EN_CURSO;
        }

        // ---------- ETAPA EXECUTE: ultimo segundo del peso ----------
        interrupcionPendiente = null;
        ejecutar(ir);
        estadisticas.registrar(ir.getOpcode());

        if (interrupcionPendiente != null) {
            return Resultado.LLAMADA_SISTEMA;
        }
        return pc >= base + alcance ? Resultado.TERMINO : Resultado.CONTINUA;
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
                int valor;
                if (instruccion.getForma() == Forma.REGISTRO_REGISTRO) {
                    valor = registros.leer(instruccion.getRegistro(1));
                } else if (instruccion.getForma() == Forma.REGISTRO_TEXTO) {
                    // El texto queda guardado en la celda de esta misma
                    // instruccion; el registro recibe su desplazamiento dentro
                    // del programa, como DX en el par DS:DX del x86. Al ser
                    // relativo a la base, sigue valiendo si el proceso se
                    // mueve a otra parte de la memoria.
                    valor = direccionIr - base;
                } else {
                    valor = instruccion.getValor(1);
                }
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
     * Descripcion: sin operandos actua sobre el AC; con un registro, sobre ese
     *              registro.
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
     *              Antes de saltar se compara el destino con la direccion base
     *              y el limite del proceso: un salto fuera de la region es un
     *              error que el sistema operativo atiende terminando el
     *              proceso.
     */
    private void saltar(Instruccion instruccion) {
        int destino = pc + instruccion.getValor(0);
        if (destino < base || destino >= base + alcance) {
            throw new DesbordamientoException("Desbordamiento: el salto \"" + instruccion
                    + "\" lleva a la direccion " + destino + ", fuera del programa (direcciones "
                    + base + " a " + (base + alcance - 1) + ")");
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
     * Restricciones: la CPU no atiende el servicio: solo lo anota
     * Descripcion: INT es una llamada al sistema: la CPU deja anotado que
     *              servicio se pidio y paso() devuelve LLAMADA_SISTEMA para que
     *              lo atienda el sistema operativo, que es quien maneja la
     *              pantalla, el teclado, los archivos y el fin del programa.
     */
    private void interrumpir(Instruccion instruccion) {
        interrupcionPendiente = instruccion.getInterrupcion();
    }


    /**
     * Nombre: limpiar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no toca la memoria ni las estadisticas
     * Descripcion: pone todos los registros en cero y deja la CPU sin
     *              contexto, como cuando el despachador ya guardo el contexto
     *              del proceso en su BCP y todavia no carga otro.
     */
    public final void limpiar() {
        registros.reset();
        pila.vaciar();
        pc = 0;
        irTexto = "";
        ir = null;
        ac = 0;
        zf = false;
        pesoActual = 0;
        restante = 0;
        direccionIr = 0;
        interrupcionPendiente = null;
        base = 0;
        alcance = 0;
    }

    /**
     * Nombre: cargarLimites
     * Entradas: base, primera direccion del proceso; alcance, cuantas
     *           posiciones ocupa
     * Salidas: ninguna
     * Restricciones: el alcance debe ser positivo
     * Descripcion: carga los registros base y limite, que es lo que hace el
     *              despachador al darle la CPU a un proceso. Tambien olvida
     *              la instruccion en curso y la interrupcion pendiente del
     *              proceso anterior.
     */
    public void cargarLimites(int base, int alcance) {
        if (alcance <= 0) {
            throw new IllegalArgumentException("El alcance debe ser positivo: " + alcance);
        }
        this.base = base;
        this.alcance = alcance;
        this.restante = 0;
        this.interrupcionPendiente = null;
    }

    /**
     * Nombre: tieneContexto
     * Entradas: ninguna
     * Salidas: true si hay un proceso cargado en la CPU
     * Restricciones: ninguna
     * Descripcion: la CPU esta libre cuando no tiene limites cargados.
     */
    public boolean tieneContexto() {
        return alcance > 0;
    }

    /**
     * Nombre: getMemoria
     * Entradas: ninguna
     * Salidas: la memoria de la que lee la CPU
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Memoria getMemoria() {
        return memoria;
    }

    /**
     * Nombre: getRegistros
     * Entradas: ninguna
     * Salidas: el banco de registros AX a DX
     * Restricciones: ninguna
     * Descripcion: el cambio de contexto lo lee y lo escribe.
     */
    public BancoRegistros getRegistros() {
        return registros;
    }

    /**
     * Nombre: getEstadisticas
     * Entradas: ninguna
     * Salidas: los contadores de la CPU
     * Restricciones: ninguna
     * Descripcion: alimenta el dialogo de estadisticas.
     */
    public Estadisticas getEstadisticas() {
        return estadisticas;
    }

    /**
     * Nombre: getPila
     * Entradas: ninguna
     * Salidas: la pila del proceso cargado
     * Restricciones: ninguna
     * Descripcion: el cambio de contexto la copia al BCP y la restaura.
     */
    public Pila getPila() {
        return pila;
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
     * Nombre: setPc
     * Entradas: pc, nueva direccion
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: lo usa el cambio de contexto al restaurar un proceso.
     */
    public void setPc(int pc) {
        this.pc = pc;
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
     * Salidas: el texto de la instruccion en curso
     * Restricciones: es cadena vacia mientras no se haya ejecutado nada
     * Descripcion: es lo que se guarda del IR en el BCP.
     */
    public String getIrTexto() {
        return irTexto;
    }

    /**
     * Nombre: setIrTexto
     * Entradas: texto, contenido del IR
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: lo usa el cambio de contexto al restaurar un proceso.
     */
    public void setIrTexto(String texto) {
        this.irTexto = texto == null ? "" : texto;
        this.ir = null;
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
     * Nombre: setAc
     * Entradas: ac, nuevo valor
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: lo usa el cambio de contexto al restaurar un proceso.
     */
    public void setAc(int ac) {
        this.ac = ac;
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
     * Nombre: setZf
     * Entradas: zf, nuevo valor de la bandera
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: lo usa el cambio de contexto al restaurar un proceso.
     */
    public void setZf(boolean zf) {
        this.zf = zf;
    }

    /**
     * Nombre: getBase
     * Entradas: ninguna
     * Salidas: el registro base
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getBase() {
        return base;
    }

    /**
     * Nombre: getAlcance
     * Entradas: ninguna
     * Salidas: el registro de alcance
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getAlcance() {
        return alcance;
    }

    /**
     * Nombre: getInterrupcionPendiente
     * Entradas: ninguna
     * Salidas: el servicio que pidio el ultimo INT, o nulo
     * Restricciones: solo tiene sentido justo despues de LLAMADA_SISTEMA
     * Descripcion: el sistema operativo la consulta para atender la llamada.
     */
    public Interrupcion getInterrupcionPendiente() {
        return interrupcionPendiente;
    }

    /**
     * Nombre: getPesoActual
     * Entradas: ninguna
     * Salidas: el peso en segundos de la instruccion del IR, o cero
     * Restricciones: ninguna
     * Descripcion: la interfaz muestra el avance, por ejemplo "ADD BX (2/3 s)".
     */
    public int getPesoActual() {
        return pesoActual;
    }

    /**
     * Nombre: getSegundosCumplidos
     * Entradas: ninguna
     * Salidas: cuantos segundos de su peso lleva la instruccion del IR
     * Restricciones: igual al peso cuando la instruccion ya termino
     * Descripcion: acompana a getPesoActual para mostrar el avance.
     */
    public int getSegundosCumplidos() {
        return pesoActual - restante;
    }

    /**
     * Nombre: getDireccionIr
     * Entradas: ninguna
     * Salidas: la direccion de donde se trajo la instruccion del IR
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getDireccionIr() {
        return direccionIr;
    }
}
