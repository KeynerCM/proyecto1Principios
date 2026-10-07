package com.mycompany.minipc.gui.modelo;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 * Nombre: RenderInstruccionActual
 * Entradas: la fila que apunta el PC, fijada desde afuera
 * Salidas: el componente ya pintado que la tabla dibuja en cada celda
 * Restricciones: solo tiene sentido aplicado a la tabla de instrucciones,
 *                porque supone que la fila coincide con el orden del programa
 * Descripcion: pinta la tabla de instrucciones distinguiendo tres situaciones:
 *              la instruccion que esta por ejecutarse, las que ya se
 *              ejecutaron y las que faltan. Ver de un vistazo que instruccion
 *              sigue es lo que hace entendible la ejecucion paso a paso.
 */
public class RenderInstruccionActual extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    /** Fondo de la fila que esta por ejecutarse. */
    private static final Color FONDO_ACTUAL = new Color(255, 235, 156);

    /** Texto de la fila que esta por ejecutarse. */
    private static final Color TEXTO_ACTUAL = new Color(70, 50, 0);

    /** Filas que ya se ejecutaron. */
    private static final Color FONDO_EJECUTADA = new Color(240, 240, 240);

    private static final Color TEXTO_EJECUTADA = new Color(120, 120, 120);

    private int filaActual = -1;

    /**
     * Nombre: setFilaActual
     * Entradas: filaActual, fila a resaltar, o -1 para no resaltar ninguna
     * Salidas: ninguna
     * Restricciones: no redibuja la tabla; eso lo hace quien lo llama
     * Descripcion: le indica al renderer cual es la instruccion que apunta el
     *              PC, para que la proxima vez que se dibuje la destaque.
     */
    public void setFilaActual(int filaActual) {
        this.filaActual = filaActual;
    }

    /**
     * Nombre: getFilaActual
     * Entradas: ninguna
     * Salidas: la fila que se esta resaltando, o -1 si ninguna
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al valor fijado.
     */
    public int getFilaActual() {
        return filaActual;
    }

    /**
     * Nombre: getTableCellRendererComponent
     * Entradas: tabla, valor de la celda, si esta seleccionada, si tiene el
     *           foco, y la fila y columna que se estan dibujando
     * Salidas: el componente con el formato ya aplicado
     * Restricciones: si la fila esta seleccionada se respeta el color de
     *                seleccion del sistema y no se pinta nada encima
     * Descripcion: restablece la fuente de la tabla, porque el renderer se
     *              reutiliza entre celdas, y despues elige el color de fondo
     *              segun la fila sea la actual, una ya ejecutada o una
     *              pendiente.
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

        if (fila == filaActual) {
            celda.setBackground(FONDO_ACTUAL);
            celda.setForeground(TEXTO_ACTUAL);
            celda.setFont(celda.getFont().deriveFont(Font.BOLD));
        } else if (filaActual >= 0 && fila < filaActual) {
            celda.setBackground(FONDO_EJECUTADA);
            celda.setForeground(TEXTO_EJECUTADA);
        } else {
            celda.setBackground(tabla.getBackground());
            celda.setForeground(tabla.getForeground());
        }
        return celda;
    }
}
