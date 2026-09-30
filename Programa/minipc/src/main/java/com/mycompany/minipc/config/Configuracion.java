package com.mycompany.minipc.config;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.excepciones.ConfiguracionException;

/**
 * Nombre: Configuracion
 * Entradas: los tamanos de la memoria principal y del disco, y la velocidad
 *           de la ejecucion automatica
 * Salidas: no aplica
 * Restricciones: es inmutable; si el objeto existe, todos sus valores son
 *                validos
 * Descripcion: parametros de la minicomputadora que el enunciado pide que no
 *              queden en el codigo, sino en un archivo externo. La lectura y
 *              escritura de ese archivo la hace LectorConfiguracion; esta
 *              clase solo guarda los valores y verifica que sean coherentes.
 */
public final class Configuracion {

    /** Menor pausa admitida entre segundos de CPU en la ejecucion automatica. */
    public static final int MS_POR_SEGUNDO_MINIMO = 50;

    /** Mayor pausa admitida entre segundos de CPU en la ejecucion automatica. */
    public static final int MS_POR_SEGUNDO_MAXIMO = 2000;

    /** Pausa por defecto: un segundo simulado dura un segundo real. */
    public static final int MS_POR_SEGUNDO_POR_DEFECTO = 1000;

    private final int tamanoMemoria;
    private final int limiteKernel;
    private final int tamanoDisco;
    private final int tamanoMemoriaVirtual;
    private final int msPorSegundo;

    /**
     * Nombre: Configuracion
     * Entradas: tamanoMemoria, posiciones de la memoria principal;
     *           limiteKernel, primera direccion de la zona de usuario;
     *           tamanoDisco, posiciones del disco; tamanoMemoriaVirtual,
     *           posiciones del disco reservadas para intercambio;
     *           msPorSegundo, milisegundos reales que dura cada segundo de
     *           CPU en la ejecucion automatica
     * Salidas: la configuracion construida
     * Restricciones: lanza ConfiguracionException con todos los problemas
     *                encontrados si algun valor es invalido
     * Descripcion: valida con las mismas reglas que usan Memoria y Disco, de
     *              modo que una configuracion aceptada siempre se puede
     *              aplicar sin errores.
     */
    public Configuracion(int tamanoMemoria, int limiteKernel, int tamanoDisco,
            int tamanoMemoriaVirtual, int msPorSegundo) throws ConfiguracionException {
        List<String> errores = new ArrayList<>();
        errores.addAll(Memoria.validar(tamanoMemoria, limiteKernel));
        errores.addAll(Disco.validar(tamanoDisco, tamanoMemoriaVirtual));
        if (msPorSegundo < MS_POR_SEGUNDO_MINIMO || msPorSegundo > MS_POR_SEGUNDO_MAXIMO) {
            errores.add("La duracion de cada segundo de CPU debe estar entre "
                    + MS_POR_SEGUNDO_MINIMO + " y " + MS_POR_SEGUNDO_MAXIMO
                    + " ms, se recibio " + msPorSegundo);
        }
        if (!errores.isEmpty()) {
            throw new ConfiguracionException(errores);
        }

        this.tamanoMemoria = tamanoMemoria;
        this.limiteKernel = limiteKernel;
        this.tamanoDisco = tamanoDisco;
        this.tamanoMemoriaVirtual = tamanoMemoriaVirtual;
        this.msPorSegundo = msPorSegundo;
    }

    /**
     * Nombre: porDefecto
     * Entradas: ninguna
     * Salidas: la configuracion con los valores por defecto del enunciado
     * Restricciones: ninguna
     * Descripcion: memoria de 256, kernel de 0 a 63, disco de 512 con 64 de
     *              memoria virtual y un segundo real por segundo de CPU. Es la
     *              que se escribe cuando el archivo todavia no existe.
     */
    public static Configuracion porDefecto() {
        try {
            return new Configuracion(Memoria.TAMANO_POR_DEFECTO,
                    Memoria.LIMITE_KERNEL_POR_DEFECTO, Disco.TAMANO_POR_DEFECTO,
                    Disco.MEMORIA_VIRTUAL_POR_DEFECTO, MS_POR_SEGUNDO_POR_DEFECTO);
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
     * Nombre: getLimiteKernel
     * Entradas: ninguna
     * Salidas: primera direccion de la zona de usuario
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getLimiteKernel() {
        return limiteKernel;
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
}
