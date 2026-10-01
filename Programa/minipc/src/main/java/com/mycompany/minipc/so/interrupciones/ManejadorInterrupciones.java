package com.mycompany.minipc.so.interrupciones;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.mycompany.minipc.hardware.Pantalla;
import com.mycompany.minipc.hardware.Procesador;
import com.mycompany.minipc.isa.Interrupcion;
import com.mycompany.minipc.isa.RegistroID;
import com.mycompany.minipc.so.archivos.SistemaArchivos;
import com.mycompany.minipc.so.despacho.Despachador;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.ListaProcesos;
import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: ManejadorInterrupciones
 * Entradas: la CPU, el despachador, la lista de procesos, la pantalla, el
 *           sistema de archivos y la bitacora
 * Salidas: no aplica
 * Restricciones: solo INT 09H bloquea al proceso; los demas servicios se
 *                atienden sin quitarle la CPU
 * Descripcion: atiende las dos clases de interrupcion que usa el Mini PC
 *              (Stallings, tablas 1.1 y 3.8):
 *
 *                llamadas al sistema (INT, "supervisor call"):
 *                  INT 20H  fin del programa
 *                  INT 10H  imprime DX en la pantalla
 *                  INT 09H  pide un valor al teclado: el proceso pasa a
 *                           EN_ESPERA y la CPU queda para otro
 *                  INT 21H  manejo de archivos (SistemaArchivos)
 *
 *                interrupcion de entrada y salida:
 *                  el ENTER del teclado entrega el valor al proceso que
 *                  esperaba, que vuelve a PREPARADO
 */
public class ManejadorInterrupciones {

    /** Lo que acepta el teclado: un numero de 1 a 3 digitos. */
    private static final Pattern VALOR_TECLADO = Pattern.compile("\\d{1,3}");

    /** Mayor valor que acepta el teclado. */
    public static final int MAXIMO_TECLADO = 255;

    /** Aviso que INT 09H escribe en la pantalla. */
    public static final String AVISO_TECLADO = ">> Ingresar valor:";

    private final Procesador cpu;
    private final Despachador despachador;
    private final ListaProcesos procesos;
    private final Pantalla pantalla;
    private final SistemaArchivos archivos;
    private final Consumer<String> bitacora;

    /**
     * Nombre: ManejadorInterrupciones
     * Entradas: cpu, procesador; despachador, para sacar al proceso que se
     *           bloquea; procesos, lista de procesos; pantalla, monitor;
     *           archivos, sistema de archivos; bitacora, donde se anota
     * Salidas: el manejador construido
     * Restricciones: ninguna
     * Descripcion: recibe todo lo que necesita, sin crear nada propio.
     */
    public ManejadorInterrupciones(Procesador cpu, Despachador despachador,
            ListaProcesos procesos, Pantalla pantalla, SistemaArchivos archivos,
            Consumer<String> bitacora) {
        this.cpu = cpu;
        this.despachador = despachador;
        this.procesos = procesos;
        this.pantalla = pantalla;
        this.archivos = archivos;
        this.bitacora = bitacora;
    }

    /**
     * Nombre: atenderLlamada
     * Entradas: proceso, el que ejecuto INT; interrupcion, servicio pedido
     * Salidas: true si el proceso debe terminar (INT 20H)
     * Restricciones: lanza EjecucionException si el servicio falla, por
     *                ejemplo un archivo inexistente
     * Descripcion: la CPU ya cumplio el peso del INT; aqui se hace el trabajo
     *              del sistema operativo.
     */
    public boolean atenderLlamada(Proceso proceso, Interrupcion interrupcion) {
        switch (interrupcion) {
            case FIN_PROGRAMA:
                bitacora.accept("Llamada al sistema INT 20H: " + proceso + " pide terminar.");
                return true;
            case PANTALLA:
                int dx = cpu.getRegistros().leer(RegistroID.DX);
                pantalla.escribir(String.valueOf(dx));
                bitacora.accept("Llamada al sistema INT 10H: " + proceso
                        + " imprime DX = " + dx + " en la pantalla.");
                return false;
            case TECLADO:
                pantalla.escribir(AVISO_TECLADO);
                bitacora.accept("Llamada al sistema INT 09H: " + proceso
                        + " espera un valor del teclado.");
                despachador.sacar(EstadoProceso.EN_ESPERA);
                return false;
            default:
                String hecho = archivos.atender(cpu, proceso);
                bitacora.accept("Llamada al sistema INT 21H: " + proceso + " " + hecho + ".");
                return false;
        }
    }

    /**
     * Nombre: hayEsperaTeclado
     * Entradas: ninguna
     * Salidas: true si algun proceso espera un valor del teclado
     * Restricciones: ninguna
     * Descripcion: la interfaz habilita el teclado solo en ese caso. Como
     *              INT 09H es lo unico que bloquea, EN_ESPERA significa
     *              esperando el teclado.
     */
    public boolean hayEsperaTeclado() {
        return !procesos.conEstado(EstadoProceso.EN_ESPERA).isEmpty();
    }

    /**
     * Nombre: entradaTeclado
     * Entradas: texto, lo que se escribio antes de presionar ENTER
     * Salidas: el proceso que recibio el valor
     * Restricciones: lanza IllegalArgumentException si no es un numero de 0 a
     *                255, e IllegalStateException si nadie espera el teclado
     * Descripcion: la interrupcion de entrada y salida del teclado. El valor
     *              va al DX del BCP del primer proceso que espera, que vuelve
     *              a PREPARADO al final de la cola de listos. El valor se
     *              muestra en la pantalla como eco.
     */
    public Proceso entradaTeclado(String texto) {
        String valorTexto = texto == null ? "" : texto.trim();
        if (!VALOR_TECLADO.matcher(valorTexto).matches()
                || Integer.parseInt(valorTexto) > MAXIMO_TECLADO) {
            throw new IllegalArgumentException("El teclado solo acepta numeros enteros de 0 a "
                    + MAXIMO_TECLADO + ", se recibio \"" + valorTexto + "\"");
        }
        List<Proceso> esperando = procesos.conEstado(EstadoProceso.EN_ESPERA);
        if (esperando.isEmpty()) {
            throw new IllegalStateException("Ningun proceso esta esperando un valor del teclado");
        }
        int valor = Integer.parseInt(valorTexto);
        Proceso proceso = esperando.get(0);
        proceso.setRegistro(RegistroID.DX, valor);
        proceso.setEstado(EstadoProceso.PREPARADO);
        procesos.moverAlFinal(proceso);
        pantalla.escribir(String.valueOf(valor));
        bitacora.accept("Interrupcion de E/S (teclado): " + proceso + " recibe " + valor
                + " en DX y pasa a PREPARADO.");
        return proceso;
    }
}
