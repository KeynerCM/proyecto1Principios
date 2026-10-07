package com.mycompany.minipc.config;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.minipc.excepciones.ConfiguracionException;
import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.so.SistemaOperativo;
import com.mycompany.minipc.so.planificacion.FCFS;
import com.mycompany.minipc.so.planificacion.FabricaAlgoritmos;

/**
 * Nombre: Configuracion
 * Entradas: los tamanos de la memoria principal y del disco, la velocidad
 *           de la ejecucion automatica y el algoritmo de planificacion
 * Salidas: no aplica
 * Restricciones: es inmutable; si el objeto existe, todos sus valores son
 *                validos
 * Descripcion: parametros de la minicomputadora, que se guardan en un archivo
 *              externo y no en el codigo. La lectura y escritura de ese archivo
 *              la hace LectorConfiguracion; esta clase solo guarda los valores
 *              y verifica que sean coherentes. El tamano del kernel no esta
 *              aqui porque no se configura: lo calcula la tabla de BCP.
 */
public final class Configuracion {

    /** Menor pausa admitida entre segundos de CPU en la ejecucion automatica. */
    public static final int MS_POR_SEGUNDO_MINIMO = 50;

    /** Mayor pausa admitida entre segundos de CPU en la ejecucion automatica. */
    public static final int MS_POR_SEGUNDO_MAXIMO = 2000;

    /** Pausa por defecto: un segundo simulado dura un segundo real. */
    public static final int MS_POR_SEGUNDO_POR_DEFECTO = 1000;

    private final int tamanoMemoria;
    private final int tamanoDisco;
    private final int tamanoMemoriaVirtual;
    private final int msPorSegundo;
    private final String algoritmo;

    /**
     * Nombre: Configuracion
     * Entradas: tamanoMemoria, posiciones de la memoria principal;
     *           tamanoDisco, posiciones del disco; tamanoMemoriaVirtual,
     *           posiciones del disco reservadas para intercambio;
     *           msPorSegundo, milisegundos reales que dura cada segundo de
     *           CPU en la ejecucion automatica; algoritmo, nombre del
     *           algoritmo de planificacion
     * Salidas: la configuracion construida
     * Restricciones: lanza ConfiguracionException con todos los problemas
     *                encontrados si algun valor es invalido
     * Descripcion: valida con las mismas reglas que usan el sistema
     *              operativo, Disco y la fabrica de algoritmos, de modo que
     *              una configuracion aceptada siempre se puede aplicar sin
     *              errores. El nombre del algoritmo se guarda en mayusculas.
     */
    public Configuracion(int tamanoMemoria, int tamanoDisco, int tamanoMemoriaVirtual,
            int msPorSegundo, String algoritmo) throws ConfiguracionException {
        List<String> errores = new ArrayList<>();
        errores.addAll(SistemaOperativo.validarMemoria(tamanoMemoria));
        errores.addAll(Disco.validar(tamanoDisco, tamanoMemoriaVirtual));
        if (msPorSegundo < MS_POR_SEGUNDO_MINIMO || msPorSegundo > MS_POR_SEGUNDO_MAXIMO) {
            errores.add("La duracion de cada segundo de CPU debe estar entre "
                    + MS_POR_SEGUNDO_MINIMO + " y " + MS_POR_SEGUNDO_MAXIMO
                    + " ms, se recibio " + msPorSegundo);
        }
        try {
            FabricaAlgoritmos.crear(algoritmo);
        } catch (IllegalArgumentException e) {
            errores.add(e.getMessage());
        }
        if (!errores.isEmpty()) {
            throw new ConfiguracionException(errores);
        }

        this.tamanoMemoria = tamanoMemoria;
        this.tamanoDisco = tamanoDisco;
        this.tamanoMemoriaVirtual = tamanoMemoriaVirtual;
        this.msPorSegundo = msPorSegundo;
        this.algoritmo = algoritmo == null ? "" : algoritmo.trim().toUpperCase();
    }

    /**
     * Nombre: porDefecto
     * Entradas: ninguna
     * Salidas: la configuracion con los valores por defecto
     * Restricciones: ninguna
     * Descripcion: memoria de 256, disco de 512 con 64 de memoria virtual,
     *              un segundo real por segundo de CPU y FCFS. Es la que se
     *              escribe cuando el archivo todavia no existe.
     */
    public static Configuracion porDefecto() {
        try {
            return new Configuracion(Memoria.TAMANO_POR_DEFECTO, Disco.TAMANO_POR_DEFECTO,
                    Disco.MEMORIA_VIRTUAL_POR_DEFECTO, MS_POR_SEGUNDO_POR_DEFECTO, FCFS.NOMBRE);
        } catch (ConfiguracionException e) {
            // Los valores por defecto son constantes validas; si esto falla,
            // es un error de programacion y no algo que el usuario pueda corregir.
            throw new IllegalStateException("Los valores por defecto no son validos", e);
        }
    }

    /**
     * Nombre: getTamanoMemoria
     * Entradas: ninguna
     * Salidas: posiciones de la memoria principal
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getTamanoMemoria() {
        return tamanoMemoria;
    }

    /**
     * Nombre: getTamanoDisco
     * Entradas: ninguna
     * Salidas: posiciones del disco
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getTamanoDisco() {
        return tamanoDisco;
    }

    /**
     * Nombre: getTamanoMemoriaVirtual
     * Entradas: ninguna
     * Salidas: posiciones del disco reservadas para intercambio
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getTamanoMemoriaVirtual() {
        return tamanoMemoriaVirtual;
    }

    /**
     * Nombre: getMsPorSegundo
     * Entradas: ninguna
     * Salidas: milisegundos reales que dura un segundo de CPU en automatico
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getMsPorSegundo() {
        return msPorSegundo;
    }

    /**
     * Nombre: getAlgoritmo
     * Entradas: ninguna
     * Salidas: el nombre del algoritmo de planificacion, en mayusculas
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public String getAlgoritmo() {
        return algoritmo;
    }
}
