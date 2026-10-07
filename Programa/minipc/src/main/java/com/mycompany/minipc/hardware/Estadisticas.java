package com.mycompany.minipc.hardware;

import com.mycompany.minipc.isa.OpCode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Nombre: Estadisticas
 * Entradas: los eventos que el procesador va anotando durante la ejecucion
 * Salidas: los totales acumulados
 * Restricciones: el cronometro arranca al construir o al hacer reset, no al
 *                ejecutar la primera instruccion
 * Descripcion: contabilidad de una ejecucion: cuantas instrucciones se
 *              ejecutaron, de que tipo, cuantos accesos hubo y cuanto tardo.
 *              Es el origen de los datos que muestra el dialogo de
 *              estadisticas.
 */
public class Estadisticas {

    private int totalInstrucciones;
    private final Map<OpCode, Integer> conteoPorOperacion;
    private int accesosLectura;
    private int accesosEscritura;
    private int posicionesUsadas;
    private long inicioMs;
    private long tiempoTotalMs;

    /**
     * Nombre: Estadisticas
     * Entradas: ninguna
     * Salidas: el objeto construido, con todo en cero
     * Restricciones: ninguna
     * Descripcion: crea el contador delegando en reset(), de modo que el
     *              estado inicial y el estado tras reiniciar son el mismo.
     */
    public Estadisticas() {
        conteoPorOperacion = new EnumMap<>(OpCode.class);
        reset();
    }

    /**
     * Nombre: reset
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: es final porque el constructor la invoca
     * Descripcion: deja la contabilidad en cero y vuelve a arrancar el
     *              cronometro. Inicializa el conteo de todas las operaciones
     *              en cero para que el dialogo muestre las cinco aunque
     *              alguna no se haya usado.
     */
    public final void reset() {
        totalInstrucciones = 0;
        conteoPorOperacion.clear();
        for (OpCode op : OpCode.values()) {
            conteoPorOperacion.put(op, 0);
        }
        accesosLectura = 0;
        accesosEscritura = 0;
        posicionesUsadas = 0;
        inicioMs = System.currentTimeMillis();
        tiempoTotalMs = 0;
    }

    /**
     * Nombre: registrar
     * Entradas: opcode, operacion que se acaba de ejecutar
     * Salidas: ninguna
     * Restricciones: el opcode no debe ser nulo
     * Descripcion: anota una instruccion ejecutada, incrementa el conteo de
     *              su tipo y actualiza el tiempo transcurrido, de modo que el
     *              tiempo refleja hasta la ultima instruccion y no hasta que
     *              se consulte.
     */
    public void registrar(OpCode opcode) {
        totalInstrucciones++;
        conteoPorOperacion.merge(opcode, 1, Integer::sum);
        tiempoTotalMs = System.currentTimeMillis() - inicioMs;
    }

    /**
     * Nombre: registrarLectura
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: anota una lectura de memoria. Ocurre una por instruccion,
     *              en la etapa de fetch.
     */
    public void registrarLectura() {
        accesosLectura++;
    }

    /**
     * Nombre: registrarEscritura
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: anota una escritura en un registro. En este juego de
     *              instrucciones solo MOV y STORE escriben, y lo hacen en
     *              registros, nunca en memoria.
     */
    public void registrarEscritura() {
        accesosEscritura++;
    }

    /**
     * Nombre: getTotalInstrucciones
     * Entradas: ninguna
     * Salidas: cuantas instrucciones se ejecutaron
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al acumulado.
     */
    public int getTotalInstrucciones() {
        return totalInstrucciones;
    }

    /**
     * Nombre: getConteo
     * Entradas: opcode, operacion a consultar
     * Salidas: cuantas veces se ejecuto esa operacion
     * Restricciones: devuelve cero si nunca se ejecuto, nunca falla
     * Descripcion: alimenta las barras por operacion del dialogo de
     *              estadisticas.
     */
    public int getConteo(OpCode opcode) {
        return conteoPorOperacion.getOrDefault(opcode, 0);
    }

    /**
     * Nombre: getConteoPorOperacion
     * Entradas: ninguna
     * Salidas: el conteo completo, en el orden de declaracion del enum
     * Restricciones: el mapa devuelto es de solo lectura
     * Descripcion: permite recorrer todas las operaciones sin preguntar una
     *              por una.
     */
    public Map<OpCode, Integer> getConteoPorOperacion() {
        return Collections.unmodifiableMap(conteoPorOperacion);
    }

    /**
     * Nombre: getAccesosLectura
     * Entradas: ninguna
     * Salidas: cuantas lecturas de memoria hubo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al acumulado.
     */
    public int getAccesosLectura() {
        return accesosLectura;
    }

    /**
     * Nombre: getAccesosEscritura
     * Entradas: ninguna
     * Salidas: cuantas escrituras en registros hubo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al acumulado.
     */
    public int getAccesosEscritura() {
        return accesosEscritura;
    }

    /**
     * Nombre: getPosicionesUsadas
     * Entradas: ninguna
     * Salidas: cuantas posiciones de memoria ocupa el programa
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al valor que fijo el procesador al
     *              cargar el programa.
     */
    public int getPosicionesUsadas() {
        return posicionesUsadas;
    }

    /**
     * Nombre: setPosicionesUsadas
     * Entradas: posicionesUsadas, cantidad de celdas ocupadas
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: lo fija el procesador al cargar, porque este dato no
     *              proviene de un evento acumulable sino del estado de la
     *              memoria en ese instante.
     */
    public void setPosicionesUsadas(int posicionesUsadas) {
        this.posicionesUsadas = posicionesUsadas;
    }

    /**
     * Nombre: getTiempoTotalMs
     * Entradas: ninguna
     * Salidas: milisegundos transcurridos hasta la ultima instruccion
     * Restricciones: incluye las pausas de la ejecucion automatica, ya que
     *                mide tiempo de reloj y no tiempo de calculo
     * Descripcion: acceso de solo lectura al tiempo acumulado.
     */
    public long getTiempoTotalMs() {
        return tiempoTotalMs;
    }
}
