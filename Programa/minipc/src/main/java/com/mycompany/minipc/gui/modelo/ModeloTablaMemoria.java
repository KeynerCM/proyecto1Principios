package com.mycompany.minipc.gui.modelo;

import com.mycompany.minipc.hardware.Memoria;

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
 *              posicion, el campo (que es y de quien es la celda, por ejemplo
 *              "P2.PC", calculado a partir de la direccion) y el valor, que es
 *              el texto guardado tal cual.
 */
public class ModeloTablaMemoria extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNAS = {"Pos", "Campo", "Valor"};

    private final Memoria memoria;
    private final MapaMemoria mapa;

    /**
     * Nombre: ModeloTablaMemoria
     * Entradas: memoria, memoria a reflejar en la tabla; mapa, quien dice que
     *           es cada celda
     * Salidas: el modelo construido
     * Restricciones: la memoria no debe ser nula y debe seguir existiendo
     *                mientras la tabla se muestre
     * Descripcion: guarda la referencia a la memoria, sin copiar su contenido.
     */
    public ModeloTablaMemoria(Memoria memoria, MapaMemoria mapa) {
        this.memoria = memoria;
        this.mapa = mapa;
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
     * Salidas: la direccion, el campo o el valor
     * Restricciones: la fila debe ser una direccion valida de la memoria
     * Descripcion: la fila coincide con la direccion, de modo que la tabla
     *              refleja el mapa de memoria sin ninguna traduccion.
     */
    @Override
    public Object getValueAt(int fila, int columna) {
        switch (columna) {
            case 0:
                return fila;
            case 1:
                return mapa.describir(fila);
            case 2:
                return contenidoDe(fila);
            default:
                return "";
        }
    }

    /**
     * Nombre: contenidoDe
     * Entradas: fila, direccion de memoria a describir
     * Salidas: el texto a mostrar en la columna de contenido
     * Restricciones: ninguna
     * Descripcion: devuelve el texto guardado en la posicion tal cual.
     */
    private String contenidoDe(int fila) {
        return memoria.leer(fila);
    }
}
