package com.mycompany.minipc.gui.modelo;

import com.mycompany.minipc.core.CeldaMemoria;
import com.mycompany.minipc.core.Memoria;

import javax.swing.table.AbstractTableModel;

/**
 * Nombre: ModeloTablaMemoria
 * Entradas: la memoria del procesador que se quiere reflejar
 * Salidas: el contenido de cada celda que la tabla pida dibujar
 * Restricciones: las celdas no son editables; el modelo no guarda copia de
 *                los datos, de modo que depende de que la memoria siga viva
 * Descripcion: modelo de la tabla de memoria. Lee directamente de la memoria
 *              del procesador cada vez que la tabla se dibuja, asi nunca
 *              queda desfasado respecto al estado real y refrescar la vista
 *              se reduce a disparar fireTableDataChanged. Las columnas son la
 *              posicion, la zona a la que pertenece y el contenido legible.
 */
public class ModeloTablaMemoria extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNAS = {"Pos", "Zona", "Contenido"};

    private final Memoria memoria;

    /**
     * Nombre: ModeloTablaMemoria
     * Entradas: memoria, memoria a reflejar en la tabla
     * Salidas: el modelo construido
     * Restricciones: la memoria no debe ser nula y debe seguir existiendo
     *                mientras la tabla se muestre
     * Descripcion: guarda la referencia a la memoria, sin copiar su contenido.
     */
    public ModeloTablaMemoria(Memoria memoria) {
        this.memoria = memoria;
    }

    /**
     * Nombre: getRowCount
     * Entradas: ninguna
     * Salidas: cuantas posiciones tiene la memoria
     * Restricciones: cambia si se reconfigura el tamano de la memoria
     * Descripcion: Swing la consulta para saber cuantas filas dibujar; hay
     *              una fila por posicion de memoria.
     */
    @Override
    public int getRowCount() {
        return memoria.getTamano();
    }

    /**
     * Nombre: getColumnCount
     * Entradas: ninguna
     * Salidas: cuantas columnas tiene la tabla, siempre tres
     * Restricciones: ninguna
     * Descripcion: Swing la consulta para saber cuantas columnas dibujar.
     */
    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    /**
     * Nombre: getColumnName
     * Entradas: columna, indice de la columna desde cero
     * Salidas: el titulo que se muestra en el encabezado
     * Restricciones: el indice debe estar dentro del rango de columnas
     * Descripcion: devuelve el encabezado correspondiente.
     */
    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    /**
     * Nombre: isCellEditable
     * Entradas: fila y columna de la celda consultada
     * Salidas: siempre false
     * Restricciones: ninguna
     * Descripcion: la memoria se modifica ejecutando el programa, no
     *              escribiendo sobre la tabla.
     */
    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    /**
     * Nombre: getValueAt
     * Entradas: fila, direccion de memoria; columna, dato pedido
     * Salidas: la direccion, la zona o el contenido
     * Restricciones: la fila debe ser una direccion valida de la memoria
     * Descripcion: la fila coincide con la direccion, de modo que la tabla
     *              refleja el mapa de memoria sin ninguna traduccion.
     */
    @Override
    public Object getValueAt(int fila, int columna) {
        CeldaMemoria celda = memoria.leer(fila);
        switch (columna) {
            case 0:
                return fila;
            case 1:
                return memoria.esDireccionKernel(fila) ? "Kernel" : "Usuario";
            case 2:
                return contenidoDe(celda, fila);
            default:
                return "";
        }
    }

    /**
     * Nombre: contenidoDe
     * Entradas: celda, posicion de memoria a describir; fila, su direccion
     * Salidas: el texto a mostrar en la columna de contenido
     * Restricciones: ninguna
     * Descripcion: devuelve la etiqueta de la instruccion si la celda guarda
     *              una, la marca "[reservada]" si pertenece al kernel, y texto
     *              vacio si esta libre. Distinguir reservada de libre importa:
     *              una celda del kernel no esta disponible aunque no tenga
     *              nada escrito.
     */
    private String contenidoDe(CeldaMemoria celda, int fila) {
        if (!celda.estaLibre() && celda.getTipo() != CeldaMemoria.Tipo.RESERVADA_KERNEL) {
            return celda.getEtiqueta();
        }
        return memoria.esDireccionKernel(fila) ? "[reservada]" : "";
    }
}
