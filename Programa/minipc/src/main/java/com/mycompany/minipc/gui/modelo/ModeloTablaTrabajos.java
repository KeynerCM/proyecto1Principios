package com.mycompany.minipc.gui.modelo;

import javax.swing.table.AbstractTableModel;

import com.mycompany.minipc.so.SistemaOperativo;
import com.mycompany.minipc.so.trabajos.ListaTrabajos;
import com.mycompany.minipc.so.trabajos.Trabajo;

/**
 * Nombre: ModeloTablaTrabajos
 * Entradas: la lista de trabajos del sistema operativo
 * Salidas: las filas de la tabla "Lista de trabajos"
 * Restricciones: solo lee; no cambia ningun trabajo
 * Descripcion: muestra la lista de trabajos y el estado de cada uno, como
 *              pide el enunciado. El estado de un trabajo admitido se lee del
 *              BCP en memoria, asi que la tabla refleja cada cambio de estado.
 */
public class ModeloTablaTrabajos extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNAS = {"PID", "Programa", "Estado", "Inicio", "Fin"};

    private final ListaTrabajos trabajos;

    /**
     * Nombre: ModeloTablaTrabajos
     * Entradas: trabajos, lista a mostrar
     * Salidas: el modelo construido
     * Restricciones: ninguna
     * Descripcion: guarda la referencia; las filas se leen al dibujar.
     */
    public ModeloTablaTrabajos(ListaTrabajos trabajos) {
        this.trabajos = trabajos;
    }

    /**
     * Nombre: getRowCount
     * Entradas: ninguna
     * Salidas: cuantos trabajos hay
     * Restricciones: ninguna
     * Descripcion: una fila por trabajo.
     */
    @Override
    public int getRowCount() {
        return trabajos.getTrabajos().size();
    }

    /**
     * Nombre: getColumnCount
     * Entradas: ninguna
     * Salidas: la cantidad de columnas
     * Restricciones: ninguna
     * Descripcion: PID, programa, estado, inicio y fin.
     */
    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    /**
     * Nombre: getColumnName
     * Entradas: columna, indice de la columna
     * Salidas: su titulo
     * Restricciones: ninguna
     * Descripcion: acceso a los titulos fijos.
     */
    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    /**
     * Nombre: isCellEditable
     * Entradas: fila y columna
     * Salidas: siempre false
     * Restricciones: ninguna
     * Descripcion: la tabla es solo de lectura.
     */
    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    /**
     * Nombre: getValueAt
     * Entradas: fila, trabajo; columna, dato pedido
     * Salidas: el valor de la celda
     * Restricciones: la fila debe existir
     * Descripcion: si el trabajo termino por un error, el estado lo dice.
     */
    @Override
    public Object getValueAt(int fila, int columna) {
        Trabajo trabajo = trabajos.getTrabajos().get(fila);
        switch (columna) {
            case 0:
                return "P" + trabajo.getPid();
            case 1:
                return trabajo.getPrograma();
            case 2:
                return trabajo.getError() == null ? trabajo.getEstado().name()
                        : trabajo.getEstado().name() + " (error)";
            case 3:
                return trabajo.getInicio() < 0 ? "-"
                        : SistemaOperativo.formatearReloj(trabajo.getInicio());
            case 4:
                return trabajo.getFin() < 0 ? "-"
                        : SistemaOperativo.formatearReloj(trabajo.getFin());
            default:
                return "";
        }
    }

    /**
     * Nombre: refrescar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: avisa a la tabla que vuelva a leer todas las filas.
     */
    public void refrescar() {
        fireTableDataChanged();
    }
}
