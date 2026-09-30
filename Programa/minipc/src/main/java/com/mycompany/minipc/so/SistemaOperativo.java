package com.mycompany.minipc.so;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mycompany.minipc.excepciones.EjecucionException;
import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.hardware.EntradaIndice;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.hardware.Procesador;
import com.mycompany.minipc.so.despacho.CambioContexto;
import com.mycompany.minipc.so.despacho.Despachador;
import com.mycompany.minipc.so.memoria.GestorMemoria;
import com.mycompany.minipc.so.planificacion.AlgoritmoPlanificacion;
import com.mycompany.minipc.so.planificacion.PlanificadorProcesos;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.ListaProcesos;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;
import com.mycompany.minipc.so.trabajos.ListaTrabajos;
import com.mycompany.minipc.so.trabajos.PlanificadorTrabajos;
import com.mycompany.minipc.so.trabajos.Trabajo;

/**
 * Nombre: SistemaOperativo
 * Entradas: los tamanos de la memoria y del disco, y el algoritmo de
 *           planificacion
 * Salidas: no aplica
 * Restricciones: es una fachada: coordina a las demas piezas, pero cada
 *                decision la toma la pieza que corresponde
 * Descripcion: el sistema operativo del Mini PC. Cada llamada a tick() es un
 *              segundo de CPU, que es lo que significa presionar "Siguiente":
 *
 *                1. el planificador de trabajos admite trabajos de la lista
 *                   de trabajos mientras haya menos de 5 procesos;
 *                2. si la CPU esta libre, el planificador de procesos elige
 *                   un proceso PREPARADO y el despachador se lo da a la CPU
 *                   (cambio de contexto);
 *                3. la CPU ejecuta un segundo;
 *                4. si el proceso termina o falla, el despachador guarda su
 *                   contexto, se libera su memoria y su BCP, y en el
 *                   siguiente segundo se despacha otro.
 */
public class SistemaOperativo {

    private final Memoria memoria;
    private final Disco disco;
    private final Procesador cpu;
    private final TablaBCP tabla;
    private final ListaProcesos listaProcesos;
    private final ListaTrabajos listaTrabajos;
    private final GestorMemoria gestorMemoria;
    private final PlanificadorTrabajos planificadorTrabajos;
    private final PlanificadorProcesos planificadorProcesos;
    private final Despachador despachador;

    private final List<String> errores;
    private Consumer<String> bitacora;
    private int reloj;
    private int siguientePid;

    /**
     * Nombre: SistemaOperativo
     * Entradas: tamanoMemoria, posiciones de la memoria principal;
     *           tamanoDisco y memoriaVirtual, configuracion del disco;
     *           algoritmo, algoritmo de planificacion
     * Salidas: el sistema construido, sin trabajos
     * Restricciones: lanza IllegalArgumentException si la memoria no alcanza
     *                para el kernel mas 32 posiciones, o si el disco no es
     *                valido
     * Descripcion: el kernel ocupa K = C + P x B posiciones, calculadas por la
     *              tabla de BCP; el resto de la memoria es para los programas.
     */
    public SistemaOperativo(int tamanoMemoria, int tamanoDisco, int memoriaVirtual,
            AlgoritmoPlanificacion algoritmo) {
        this.memoria = new Memoria(tamanoMemoria, TablaBCP.TAMANO_KERNEL);
        this.disco = new Disco(tamanoDisco, memoriaVirtual);
        this.cpu = new Procesador(memoria);
        this.tabla = new TablaBCP(memoria);
        this.listaProcesos = new ListaProcesos(tabla);
        this.listaTrabajos = new ListaTrabajos();
        this.gestorMemoria = new GestorMemoria(memoria);
        this.errores = new ArrayList<>();
        this.bitacora = mensaje -> { };

        Consumer<String> conHora = mensaje -> bitacora.accept("[" + formatearReloj(reloj) + "] "
                + mensaje);
        this.planificadorTrabajos = new PlanificadorTrabajos(listaTrabajos, listaProcesos, tabla,
                gestorMemoria, disco, conHora);
        this.planificadorProcesos = new PlanificadorProcesos(listaProcesos, algoritmo);
        this.despachador = new Despachador(cpu, tabla, listaProcesos, new CambioContexto(),
                conHora);
        this.siguientePid = 1;
    }

    /**
     * Nombre: validarMemoria
     * Entradas: tamano, posiciones de la memoria principal
     * Salidas: los problemas encontrados, vacia si el tamano sirve
     * Restricciones: ninguna
     * Descripcion: la memoria debe tener el kernel calculado mas 32
     *              posiciones para programas. La usa la configuracion.
     */
    public static List<String> validarMemoria(int tamano) {
        return Memoria.validar(tamano, TablaBCP.TAMANO_KERNEL);
    }

    /**
     * Nombre: formatearReloj
     * Entradas: segundos, tiempo simulado
     * Salidas: el tiempo como hora:minuto:segundo, por ejemplo "00:01:05"
     * Restricciones: ninguna
     * Descripcion: el reloj simulado empieza en 00:00:00.
     */
    public static String formatearReloj(int segundos) {
        return String.format("%02d:%02d:%02d", segundos / 3600, (segundos / 60) % 60,
                segundos % 60);
    }

    /**
     * Nombre: agregarTrabajo
     * Entradas: programa, nombre de un archivo ya guardado en el disco
     * Salidas: el trabajo creado, en estado NUEVO
     * Restricciones: lanza IllegalStateException si la lista de trabajos esta
     *                llena, e IllegalArgumentException si el programa no
     *                existe o es mas grande que toda la memoria de usuario
     * Descripcion: le asigna el siguiente PID y lo pone al final de la lista
     *              de trabajos. No lo admite todavia: eso lo hace admitir() o
     *              el siguiente tick().
     */
    public Trabajo agregarTrabajo(String programa) {
        EntradaIndice entrada = disco.buscar(programa);
        if (entrada == null) {
            throw new IllegalArgumentException("El programa \"" + programa
                    + "\" no esta en el disco");
        }
        if (!gestorMemoria.cabeAlgunaVez(entrada.getTamano())) {
            throw new IllegalArgumentException("El programa \"" + programa + "\" ocupa "
                    + entrada.getTamano() + " posiciones y la memoria de usuario solo tiene "
                    + memoria.getEspacioUsuario());
        }
        if (listaTrabajos.estaLlena()) {
            throw new IllegalStateException("La lista de trabajos esta llena ("
                    + ListaTrabajos.CAPACIDAD + " trabajos)");
        }
        Trabajo trabajo = new Trabajo(siguientePid++, programa);
        listaTrabajos.agregar(trabajo);
        anotar("Lista de trabajos: P" + trabajo.getPid() + " (" + programa
                + ") entra como NUEVO.");
        return trabajo;
    }

    /**
     * Nombre: admitir
     * Entradas: ninguna
     * Salidas: cuantos trabajos se admitieron
     * Restricciones: ninguna
     * Descripcion: le pide al planificador de trabajos que admita lo que
     *              pueda, sin gastar tiempo de CPU. Se llama al cargar
     *              archivos para que los programas se vean en memoria.
     */
    public int admitir() {
        return planificadorTrabajos.admitir(reloj);
    }

    /**
     * Nombre: tick
     * Entradas: ninguna
     * Salidas: true si todavia quedan trabajos sin finalizar
     * Restricciones: no avanza el reloj si no hay ningun proceso que ejecutar
     * Descripcion: un segundo de CPU (ver la descripcion de la clase). Un
     *              error de ejecucion no se propaga: el proceso termina, el
     *              motivo queda en la bitacora y en tomarErrores().
     */
    public boolean tick() {
        planificadorTrabajos.admitir(reloj);
        Proceso actual = tabla.getEnEjecucion();
        if (actual == null) {
            actual = planificadorProcesos.elegir();
            if (actual == null) {
                return listaTrabajos.hayPendientes();
            }
            despachador.despachar(actual);
        }

        reloj++;
        try {
            Procesador.Resultado resultado = cpu.paso();
            actual.sumarTiempoEmpleado(1);
            despachador.actualizarBCP();
            if (resultado == Procesador.Resultado.TERMINO) {
                terminar(actual, null);
            }
        } catch (EjecucionException e) {
            actual.sumarTiempoEmpleado(1);
            String detalle = actual + " (" + actual.getPrograma() + "): " + e.getMessage();
            errores.add(detalle);
            anotar("Interrupcion de programa en " + detalle);
            terminar(actual, e.getMessage());
        }
        planificadorTrabajos.admitir(reloj);
        return listaTrabajos.hayPendientes();
    }

    /**
     * Nombre: terminar
     * Entradas: proceso, el que estaba en ejecucion; error, motivo si fallo,
     *           o nulo si termino normalmente
     * Salidas: ninguna
     * Restricciones: el proceso debe estar en la CPU
     * Descripcion: el despachador guarda su contexto y lo pasa a FINALIZADO;
     *              despues se anotan sus tiempos en la lista de trabajos y se
     *              liberan su memoria, su lugar en la lista de procesos y su BCP.
     */
    private void terminar(Proceso proceso, String error) {
        despachador.sacar(EstadoProceso.FINALIZADO);
        Trabajo trabajo = listaTrabajos.buscar(proceso.getPid());
        int base = proceso.getBase();
        int alcance = proceso.getAlcance();
        trabajo.finalizar(reloj, proceso.getTiempoEmpleado(), error);
        gestorMemoria.liberar(base, alcance);
        listaProcesos.quitar(proceso);
        anotar("P" + trabajo.getPid() + " (" + trabajo.getPrograma() + ") finalizo"
                + (error == null ? "" : " por un error") + " con " + trabajo.getTiempoCpu()
                + " s de CPU. Se liberan su BCP y las posiciones " + base + ".."
                + (base + alcance - 1) + ".");
        tabla.liberar(proceso);
    }

    /**
     * Nombre: reiniciar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: conserva la lista de trabajos y el disco
     * Descripcion: descarga todos los procesos, pone el reloj en cero y
     *              devuelve todos los trabajos a NUEVO, para ejecutarlos otra
     *              vez desde el principio.
     */
    public void reiniciar() {
        descargarTodo();
        listaTrabajos.reiniciar();
        anotar("Sistema reiniciado: todos los trabajos vuelven a NUEVO.");
    }

    /**
     * Nombre: limpiar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no toca el disco; eso lo decide quien llama
     * Descripcion: descarga todos los procesos y vacia la lista de trabajos.
     */
    public void limpiar() {
        descargarTodo();
        listaTrabajos.vaciar();
        siguientePid = 1;
    }

    /**
     * Nombre: reconfigurar
     * Entradas: tamanoMemoria, tamanoDisco y memoriaVirtual, nuevos tamanos;
     *           algoritmo, algoritmo de planificacion
     * Salidas: ninguna
     * Restricciones: lanza IllegalArgumentException si los tamanos no son
     *                validos; descarta los procesos, los trabajos y el disco
     * Descripcion: redimensionar invalida todas las direcciones asignadas,
     *              por eso se empieza de cero.
     */
    public void reconfigurar(int tamanoMemoria, int tamanoDisco, int memoriaVirtual,
            AlgoritmoPlanificacion algoritmo) {
        memoria.redimensionar(tamanoMemoria, TablaBCP.TAMANO_KERNEL);
        disco.redimensionar(tamanoDisco, memoriaVirtual);
        planificadorProcesos.setAlgoritmo(algoritmo);
        limpiar();
    }

    /**
     * Nombre: descargarTodo
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: deja la memoria, la CPU, el reloj y los errores como al
     *              arrancar.
     */
    private void descargarTodo() {
        tabla.formatear();
        memoria.limpiarZonaUsuario();
        cpu.limpiar();
        cpu.getEstadisticas().reset();
        errores.clear();
        reloj = 0;
    }

    /**
     * Nombre: tomarErrores
     * Entradas: ninguna
     * Salidas: los errores de ejecucion ocurridos desde la ultima consulta
     * Restricciones: la lista interna queda vacia
     * Descripcion: permite a la interfaz mostrar cada error una sola vez.
     */
    public List<String> tomarErrores() {
        List<String> copia = new ArrayList<>(errores);
        errores.clear();
        return copia;
    }

    /**
     * Nombre: setBitacora
     * Entradas: bitacora, quien recibe los mensajes del sistema operativo
     * Salidas: ninguna
     * Restricciones: un valor nulo descarta los mensajes
     * Descripcion: la interfaz la conecta a su consola.
     */
    public void setBitacora(Consumer<String> bitacora) {
        this.bitacora = bitacora == null ? mensaje -> { } : bitacora;
    }

    /**
     * Nombre: anotar
     * Entradas: mensaje, texto a registrar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: agrega la hora del reloj simulado y lo envia a la bitacora.
     */
    private void anotar(String mensaje) {
        bitacora.accept("[" + formatearReloj(reloj) + "] " + mensaje);
    }

    /**
     * Nombre: hayPendientes
     * Entradas: ninguna
     * Salidas: true si algun trabajo no ha finalizado
     * Restricciones: ninguna
     * Descripcion: la ejecucion automatica sigue mientras sea true.
     */
    public boolean hayPendientes() {
        return listaTrabajos.hayPendientes();
    }

    /**
     * Nombre: getEnEjecucion
     * Entradas: ninguna
     * Salidas: el proceso que tiene la CPU, o nulo
     * Restricciones: ninguna
     * Descripcion: se lee de la cabecera del kernel.
     */
    public Proceso getEnEjecucion() {
        return tabla.getEnEjecucion();
    }

    /**
     * Nombre: getReloj
     * Entradas: ninguna
     * Salidas: los segundos simulados transcurridos
     * Restricciones: ninguna
     * Descripcion: cada segundo de CPU suma uno.
     */
    public int getReloj() {
        return reloj;
    }

    /**
     * Nombre: getListaTrabajos
     * Entradas: ninguna
     * Salidas: la lista de trabajos
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public ListaTrabajos getListaTrabajos() {
        return listaTrabajos;
    }

    /**
     * Nombre: getListaProcesos
     * Entradas: ninguna
     * Salidas: la lista de procesos en memoria
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public ListaProcesos getListaProcesos() {
        return listaProcesos;
    }

    /**
     * Nombre: getTablaBCP
     * Entradas: ninguna
     * Salidas: la tabla de BCP
     * Restricciones: ninguna
     * Descripcion: la interfaz la usa para nombrar las celdas del kernel.
     */
    public TablaBCP getTablaBCP() {
        return tabla;
    }

    /**
     * Nombre: getAlgoritmo
     * Entradas: ninguna
     * Salidas: el algoritmo de planificacion en uso
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al dato correspondiente.
     */
    public AlgoritmoPlanificacion getAlgoritmo() {
        return planificadorProcesos.getAlgoritmo();
    }

    /**
     * Nombre: getMemoria
     * Entradas: ninguna
     * Salidas: la memoria principal
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Memoria getMemoria() {
        return memoria;
    }

    /**
     * Nombre: getDisco
     * Entradas: ninguna
     * Salidas: el disco
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Disco getDisco() {
        return disco;
    }

    /**
     * Nombre: getCpu
     * Entradas: ninguna
     * Salidas: el procesador
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Procesador getCpu() {
        return cpu;
    }
}
