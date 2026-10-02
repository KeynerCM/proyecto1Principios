package com.mycompany.minipc.gui;

import com.mycompany.minipc.so.procesos.Proceso;

import java.io.File;
import java.util.List;

/**
 * Nombre: VistaPrincipal
 * Entradas: no aplica, es una interfaz
 * Salidas: no aplica
 * Restricciones: quien la implemente solo debe dibujar; ninguna de estas
 *                operaciones debe contener logica de simulacion
 * Descripcion: contrato entre el controlador y la ventana. El controlador
 *              programa siempre contra esta interfaz y nunca contra la clase
 *              concreta, de modo que la logica de la aplicacion no depende de
 *              como esta construida la ventana y se puede sustituir por una
 *              implementacion de prueba sin levantar Swing.
 */
public interface VistaPrincipal {

    /**
     * Nombre: mostrarInstrucciones
     * Entradas: programa, texto de cada instruccion en orden
     * Salidas: ninguna
     * Restricciones: el modelo de la tabla lo mantiene el controlador, de modo
     *                que la vista solo debe ocuparse de la presentacion
     * Descripcion: avisa a la vista de que la tabla de instrucciones tiene
     *              contenido nuevo que mostrar.
     */
    void mostrarInstrucciones(List<String> programa);

    /**
     * Nombre: resaltarInstruccion
     * Entradas: indiceFila, fila a resaltar, o -1 para quitar el resaltado
     * Salidas: ninguna
     * Restricciones: un indice fuera de rango no debe provocar un fallo
     * Descripcion: marca cual instruccion esta por ejecutarse y desplaza la
     *              tabla para que quede a la vista.
     */
    void resaltarInstruccion(int indiceFila);

    /**
     * Nombre: refrescarMemoria
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: vuelve a dibujar la tabla de memoria, cuyo contenido
     *              cambio. No recibe datos porque el modelo lee directamente
     *              de la memoria del procesador.
     */
    void refrescarMemoria();

    /**
     * Nombre: refrescarDisco
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: vuelve a dibujar la tabla del disco, cuyo contenido
     *              cambio. Igual que la memoria, el modelo lee directamente
     *              del disco, por eso no recibe datos.
     */
    void refrescarDisco();

    /**
     * Nombre: mostrarBCP
     * Entradas: proceso, proceso en ejecucion, o nulo si la CPU esta libre
     * Salidas: ninguna
     * Restricciones: debe tolerar el valor nulo
     * Descripcion: vuelca los campos del BCP en el panel correspondiente. El
     *              proceso no guarda datos: cada valor se lee de las celdas
     *              de su BCP en la memoria del kernel.
     */
    void mostrarBCP(Proceso proceso);

    /**
     * Nombre: escribirEnConsola
     * Entradas: mensaje, texto a mostrar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: agrega una linea al registro de actividad, precedida de la
     *              hora, y deja la vista al final del texto.
     */
    void escribirEnConsola(String mensaje);

    /**
     * Nombre: limpiarConsola
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: vacia el registro de actividad, como parte de la accion de
     *              limpiar toda la maquina.
     */
    void limpiarConsola();

    /**
     * Nombre: mostrarErrores
     * Entradas: titulo, encabezado del cuadro; mensajes, errores a mostrar
     * Salidas: ninguna
     * Restricciones: la lista puede tener un solo elemento o varios
     * Descripcion: muestra los errores juntos en un cuadro de dialogo, uno por
     *              linea, de modo que el usuario los corrija en una pasada.
     */
    void mostrarErrores(String titulo, List<String> mensajes);

    /**
     * Nombre: actualizarBotones
     * Entradas: hayPrograma, si hay un programa cargado; enEjecucion, si la
     *           ejecucion automatica esta en marcha; termino, si el programa
     *           llego al final o quedo bloqueado
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: habilita o deshabilita los botones segun la situacion
     *              actual. Deshabilitar lo que no aplica es mejor practica de
     *              interfaz que permitir el clic y despues reclamar.
     */
    void actualizarBotones(boolean hayPrograma, boolean enEjecucion, boolean termino);

    /**
     * Nombre: actualizarBarraContexto
     * Entradas: nombreArchivo, archivo cargado; estado, estado del proceso
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: actualiza la linea de contexto que aparece bajo la barra de
     *              herramientas.
     */
    void actualizarBarraContexto(String nombreArchivo, String estado);

    /**
     * Nombre: actualizarUsoMemoria
     * Entradas: porcentaje, ocupacion de la zona de usuario de 0 a 100
     * Salidas: ninguna
     * Restricciones: el valor debe venir ya calculado; la vista no consulta la
     *                memoria por su cuenta
     * Descripcion: actualiza el indicador de ocupacion de la zona de usuario.
     */
    void actualizarUsoMemoria(int porcentaje);

    /**
     * Nombre: seleccionarArchivosAsm
     * Entradas: ninguna
     * Salidas: los archivos elegidos, o una lista vacia si el usuario cancelo
     * Restricciones: debe filtrar por la extension .asm y permitir elegir
     *                varios archivos a la vez
     * Descripcion: pide al usuario que elija uno o varios archivos de codigo
     *              ensamblador, como pide el enunciado. Devolver una lista
     *              vacia al cancelar permite al controlador distinguir esa
     *              situacion de un error real.
     */
    List<File> seleccionarArchivosAsm();

    /**
     * Nombre: mostrarPantalla
     * Entradas: lineas, contenido de la pantalla del Mini PC
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: dibuja la salida de INT 10H, el aviso de INT 09H y el eco
     *              del teclado.
     */
    void mostrarPantalla(List<String> lineas);

    /**
     * Nombre: habilitarTeclado
     * Entradas: habilitado, true si algun proceso espera un valor
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: el teclado solo se puede usar cuando un proceso ejecuto
     *              INT 09H y esta EN_ESPERA.
     */
    void habilitarTeclado(boolean habilitado);

    /**
     * Nombre: mostrarReloj
     * Entradas: reloj, tiempo simulado como hora:minuto:segundo
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: muestra el tiempo de ejecucion, que pide el enunciado.
     */
    void mostrarReloj(String reloj);

    /**
     * Nombre: mostrarColas
     * Entradas: procesos, la lista de procesos en el orden de sus enlaces en
     *           memoria
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: muestra la estructura de lista de procesos y las colas que
     *              salen de ella (en CPU, preparados, en espera).
     */
    void mostrarColas(List<Proceso> procesos);

    /**
     * Nombre: mostrarResumen
     * Entradas: usoDisco, porcentaje ocupado del area de archivos del disco;
     *           admitidos, procesos con BCP en este momento
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: datos de la barra de estado.
     */
    void mostrarResumen(int usoDisco, int admitidos);

    /**
     * Nombre: mostrarEstadisticas
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: se llama una vez, cuando finaliza el ultimo trabajo
     * Descripcion: el enunciado pide las estadisticas "al final de la
     *              ejecucion de los procesos".
     */
    void mostrarEstadisticas();
}
