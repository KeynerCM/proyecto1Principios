package com.mycompany.minipc.gui.modelo;

import com.mycompany.minipc.core.Disco;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Nombre: RenderZonaDisco
 * Entradas: el disco que se esta dibujando
 * Salidas: el componente ya pintado que la tabla dibuja en cada celda
 * Restricciones: solo tiene sentido aplicado a la tabla del disco, porque
 *                supone que la fila coincide con la direccion
 * Descripcion: pinta la tabla del disco distinguiendo sus zonas, con la misma
 *              logica que la tabla de memoria: el indice, las celdas ocupadas
 *              por archivos, la memoria virtual y lo libre.
 */
public class RenderZonaDisco extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    /** Celdas del indice que ya tienen una entrada. */
    private static final Color FONDO_INDICE = new Color(255, 228, 196);

    /** Celdas del indice todavia libres. */
    private static final Color FONDO_INDICE_LIBRE = new Color(255, 244, 230);

    /** Celdas del area de archivos ocupadas por un programa. */
    private static final Color FONDO_PROGRAMA = new Color(214, 234, 248);

    /** Area de memoria virtual. */
    private static final Color FONDO_VIRTUAL = new Color(232, 232, 232);

    private static final Color TEXTO_VIRTUAL = new Color(120, 120, 120);

    private final Disco disco;

    /**
     * Nombre: RenderZonaDisco
     * Entradas: disco, disco que se esta dibujando
     * Salidas: el renderer construido
     * Restricciones: el disco no debe ser nulo, porque se consulta en cada
     *                celda para saber a que zona pertenece
     * Descripcion: guarda la referencia al disco. Lo crea el controlador, que
     *              es quien conoce el disco, igual que el renderer de memoria.
     */
    public RenderZonaDisco(Disco disco) {
        this.disco = disco;
    }

    /**
     * Nombre: getTableCellRendererComponent
     * Entradas: tabla, valor de la celda, si esta seleccionada, si tiene el
     *           foco, y la fila y columna que se estan dibujando
     * Salidas: el componente con el formato ya aplicado
     * Restricciones: si la fila esta seleccionada se respeta el color de
     *                seleccion del sistema y no se pinta nada encima
     * Descripcion: restablece la fuente y elige el color de fondo segun la
     *              zona de la celda y si esta ocupada.
     */
    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor,
            boolean seleccionada, boolean tieneFoco, int fila, int columna) {

        Component celda = super.getTableCellRendererComponent(
                tabla, valor, seleccionada, tieneFoco, fila, columna);
        celda.setFont(tabla.getFont());

        if (seleccionada) {
            return celda;
        }

        celda.setForeground(tabla.getForeground());
        boolean ocupada = !disco.estaLibre(fila);

        if (disco.esDireccionIndice(fila)) {
            celda.setBackground(ocupada
                    ? FONDO_INDICE : FONDO_INDICE_LIBRE);
        } else if (disco.esDireccionMemoriaVirtual(fila)) {
            celda.setBackground(FONDO_VIRTUAL);
            celda.setForeground(TEXTO_VIRTUAL);
        } else if (ocupada) {
            celda.setBackground(FONDO_PROGRAMA);
        } else {
            celda.setBackground(tabla.getBackground());
        }
        return celda;
    }
}
