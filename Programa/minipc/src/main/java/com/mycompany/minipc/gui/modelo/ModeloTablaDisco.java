package com.mycompany.minipc.gui.modelo;

import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;

import javax.swing.table.AbstractTableModel;

/**
 * Nombre: ModeloTablaDisco
 * Entradas: el disco que se quiere reflejar y la tabla de BCP
 * Salidas: el contenido de cada celda que la tabla pida dibujar
 * Restricciones: las celdas no son editables; el modelo no guarda copia de
 *                los datos, de modo que depende de que el disco siga vivo
 * Descripcion: modelo de la tabla del disco. Lee directamente del disco cada
 *              vez que la tabla se dibuja, igual que el modelo de memoria,
 *              asi refrescar la vista se reduce a disparar
 *              fireTableDataChanged. Las columnas son la posicion, la zona a
 *              la que pertenece y el contenido legible. En la memoria virtual la
 *              zona dice que proceso suspendido ocupa la celda.
 */
public class ModeloTablaDisco extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNAS = {"Pos", "Zona", "Contenido"};

    private final Disco disco;
    private final TablaBCP tabla;

    /**
     * Nombre: ModeloTablaDisco
     * Entradas: disco, disco a reflejar en la tabla; tabla, tabla de BCP,
     *           para saber de quien es cada imagen de la memoria virtual
     * Salidas: el modelo construido
     * Restricciones: el disco no debe ser nulo y debe seguir existiendo
     *                mientras la tabla se muestre
     * Descripcion: guarda las referencias, sin copiar el contenido.
     */
    public ModeloTablaDisco(Disco disco, TablaBCP tabla) {
        this.disco = disco;
        this.tabla = tabla;
    }

    /**
     * Nombre: getRowCount
     * Entradas: ninguna
     * Salidas: cuantas posiciones tiene el disco
     * Restricciones: cambia si se reconfigura el tamano del disco
     * Descripcion: Swing la consulta para saber cuantas filas dibujar; hay
     *              una fila por posicion del disco.
     */
    @Override
    public int getRowCount() {
        return disco.getTamano();
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
     * Descripcion: el disco se modifica cargando archivos, no escribiendo
     *              sobre la tabla.
     */
    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    /**
     * Nombre: getValueAt
     * Entradas: fila, direccion del disco; columna, dato pedido
     * Salidas: la direccion, la zona o el contenido
     * Restricciones: la fila debe ser una direccion valida del disco
     * Descripcion: la fila coincide con la direccion, de modo que la tabla
     *              refleja el mapa del disco sin ninguna traduccion.
     */
    @Override
    public Object getValueAt(int fila, int columna) {
        switch (columna) {
            case 0:
                return fila;
            case 1:
                return zonaDe(fila);
            case 2:
                return disco.leer(fila);
            default:
                return "";
        }
    }

    /**
     * Nombre: zonaDe
     * Entradas: fila, direccion del disco
     * Salidas: el nombre de la zona a la que pertenece; en la memoria virtual
     *          tambien el proceso cuya imagen ocupa la celda, por ejemplo
     *          "Virtual P2"
     * Restricciones: ninguna
     * Descripcion: distingue el indice, el area de archivos y la memoria
     *              virtual, las tres zonas del disco.
     */
    private String zonaDe(int fila) {
        if (disco.esDireccionIndice(fila)) {
            return "Indice";
        }
        if (!disco.esDireccionMemoriaVirtual(fila)) {
            return "Archivos";
        }
        for (Proceso proceso : tabla.getProcesos()) {
            int base = proceso.getBase();
            if (proceso.estaEnMemoriaVirtual() && fila >= base
                    && fila < base + proceso.getAlcance()) {
                return "Virtual " + proceso;
            }
        }
        return "Virtual";
    }
}
