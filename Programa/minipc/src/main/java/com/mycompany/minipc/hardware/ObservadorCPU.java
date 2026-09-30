package com.mycompany.minipc.hardware;

/**
 * Nombre: ObservadorCPU
 * Entradas: no aplica, es una interfaz
 * Salidas: no aplica
 * Restricciones: quien la implemente no debe bloquear ni hacer trabajo largo
 *                dentro del aviso, porque el procesador queda esperando
 * Descripcion: es el patron Observer. Sirve para que el procesador no tenga
 *              que conocer a Swing: el nucleo avisa que algo cambio y la
 *              interfaz decide como se dibuja. Asi el simulador se puede
 *              probar entero sin levantar ventanas.
 */
public interface ObservadorCPU {

    /**
     * Nombre: alCambiarEstado
     * Entradas: cpu, el procesador que cambio de estado; fase, en que momento
     *           del ciclo esta avisando
     * Salidas: ninguna
     * Restricciones: se invoca desde el hilo que ejecuta la instruccion, de
     *                modo que la implementacion debe limitarse a refrescar
     * Descripcion: el procesador lo llama despues de cada etapa relevante
     *              para que el observador lea el estado actual y lo muestre.
     */
    void alCambiarEstado(Procesador cpu, Fase fase);
}
