package com.mycompany.minipc.gui.modelo;

import com.mycompany.minipc.hardware.Memoria;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 * Nombre: RenderZonaMemoria
 * Entradas: la memoria que se esta dibujando y la direccion que apunta el PC
 * Salidas: el componente ya pintado que la tabla dibuja en cada celda
 * Restricciones: solo tiene sentido aplicado a la tabla de memoria, porque
 *                supone que la fila coincide con la direccion
 * Descripcion: pinta la tabla de memoria distinguiendo las zonas. La
 *              cabecera del sistema operativo va en gris; cada ranura de BCP
 *              tiene un color, y el programa del mismo proceso en la zona de
 *              usuario va en una version mas clara de ese color. Asi se ve
 *              donde y como quedo guardado cada proceso, como pide el
 *              enunciado.
 */
public class RenderZonaMemoria extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    /** Zona reservada al sistema operativo. */
    private static final Color FONDO_KERNEL = new Color(232, 232, 232);

    private static final Color TEXTO_KERNEL = new Color(120, 120, 120);

    /** Un color por ranura de BCP (P en la ranura 0 a 4). */
    private static final Color[] FONDO_BCP = {
        new Color(173, 206, 240), new Color(178, 223, 178), new Color(247, 200, 160),
        new Color(214, 188, 232), new Color(240, 180, 190)
    };

    /** Version clara de cada color, para el programa en la zona de usuario. */
    private static final Color[] FONDO_PROGRAMA = {
        new Color(222, 236, 250), new Color(224, 242, 224), new Color(252, 232, 214),
        new Color(238, 228, 245), new Color(250, 222, 228)
    };

    /** Posicion que apunta el PC en este momento. */
    private static final Color FONDO_ACTUAL = new Color(255, 235, 156);

    private static final Color TEXTO_ACTUAL = new Color(70, 50, 0);

    private final Memoria memoria;
    private final MapaMemoria mapa;
    private int direccionActual = -1;

    /**
     * Nombre: RenderZonaMemoria
     * Entradas: memoria, memoria que se esta dibujando; mapa, quien dice de
     *           que proceso es cada celda
     * Salidas: el renderer construido
     * Restricciones: la memoria no debe ser nula, porque se consulta en cada
     *                celda para saber a que zona pertenece
     * Descripcion: guarda la referencia a la memoria. Es la razon por la que
     *              este renderer lo crea el controlador y no la ventana: la
     *              ventana no conoce el nucleo.
     */
    public RenderZonaMemoria(Memoria memoria, MapaMemoria mapa) {
        this.memoria = memoria;
        this.mapa = mapa;
    }

    /**
     * Nombre: setDireccionActual
     * Entradas: direccionActual, posicion que apunta el PC, o -1 si no aplica
     * Salidas: ninguna
     * Restricciones: no redibuja la tabla; eso lo hace quien lo llama
     * Descripcion: le indica al renderer que posicion de memoria debe destacar
     *              como la siguiente a ejecutar.
     */
    public void setDireccionActual(int direccionActual) {
        this.direccionActual = direccionActual;
    }

    /**
     * Nombre: getTableCellRendererComponent
     * Entradas: tabla, valor de la celda, si esta seleccionada, si tiene el
     *           foco, y la fila y columna que se estan dibujando
     * Salidas: el componente con el formato ya aplicado
     * Restricciones: si la fila esta seleccionada se respeta el color de
     *                seleccion del sistema y no se pinta nada encima
     * Descripcion: restablece la fuente de la tabla y elige el color de fondo
     *              en este orden de prioridad: la posicion actual, la
     *              cabecera del sistema operativo, el color del proceso dueno
     *              de la celda, una ranura de BCP libre, y por ultimo el fondo
     *              normal de la tabla.
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

        if (fila == direccionActual) {
            celda.setBackground(FONDO_ACTUAL);
            celda.setForeground(TEXTO_ACTUAL);
            celda.setFont(celda.getFont().deriveFont(Font.BOLD));
        } else {
            pintarZona(celda, tabla, fila);
        }
        return celda;
    }

    /**
     * Nombre: pintarZona
     * Entradas: celda, componente a pintar; tabla, tabla dibujada; fila,
     *           direccion de memoria
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: elige el color segun la zona y el proceso dueno.
     */
    private void pintarZona(Component celda, JTable tabla, int fila) {
        int ranura = mapa.ranuraDe(fila);
        if (ranura == MapaMemoria.CABECERA) {
            celda.setBackground(FONDO_KERNEL.darker());
            celda.setForeground(Color.WHITE);
        } else if (ranura >= 0) {
            boolean esKernel = memoria.esDireccionKernel(fila);
            celda.setBackground(esKernel ? FONDO_BCP[ranura] : FONDO_PROGRAMA[ranura]);
        } else if (memoria.esDireccionKernel(fila)) {
            celda.setBackground(FONDO_KERNEL);
            celda.setForeground(TEXTO_KERNEL);
        } else {
            celda.setBackground(tabla.getBackground());
        }
    }
}
