package com.mycompany.minipc.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import com.mycompany.minipc.gui.Tema;

/**
 * Nombre: Tablas
 * Entradas: no aplica, solo metodos de ayuda
 * Salidas: no aplica
 * Restricciones: no se instancia
 * Descripcion: el mismo estilo para todas las tablas de la ventana (letra de
 *              ancho fijo, sin lineas verticales, encabezado azul) y las
 *              leyendas de colores de la memoria y el disco.
 */
public final class Tablas {

    /**
     * Nombre: Tablas
     * Entradas: ninguna
     * Salidas: no aplica
     * Restricciones: no se instancia
     * Descripcion: clase de utilidad.
     */
    private Tablas() {
    }

    /**
     * Nombre: estilo
     * Entradas: tabla, tabla a estilizar; anchos, ancho preferido de cada
     *           columna en pixeles
     * Salidas: ninguna
     * Restricciones: si hay mas anchos que columnas, los de mas se ignoran
     * Descripcion: aplica el estilo comun.
     */
    public static void estilo(JTable tabla, int[] anchos) {
        tabla.setFont(Tema.FUENTE_MONO);
        tabla.setRowHeight(20);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(Tema.BORDE);
        tabla.setFillsViewportHeight(true);
        tabla.setBackground(Tema.TARJETA);
        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setReorderingAllowed(false);
        encabezado.setDefaultRenderer(new RenderEncabezado());
        TableColumnModel columnas = tabla.getColumnModel();
        for (int i = 0; i < anchos.length && i < columnas.getColumnCount(); i++) {
            columnas.getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    /**
     * Nombre: leyenda
     * Entradas: pares de color y texto, alternados
     * Salidas: una fila con un cuadrito de cada color y su texto
     * Restricciones: los argumentos van alternados: Color, String, ...
     * Descripcion: explica los colores de la tabla de memoria o del disco.
     */
    public static JPanel leyenda(Object... pares) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setBackground(Tema.TARJETA);
        for (int i = 0; i < pares.length; i += 2) {
            JLabel cuadro = new JLabel("    ");
            cuadro.setOpaque(true);
            cuadro.setBackground((Color) pares[i]);
            cuadro.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
            JLabel texto = new JLabel((String) pares[i + 1]);
            texto.setFont(Tema.FUENTE);
            texto.setForeground(Tema.TEXTO_SUAVE);
            panel.add(cuadro);
            panel.add(texto);
        }
        return panel;
    }

    /**
     * Nombre: RenderEncabezado
     * Entradas: no aplica
     * Salidas: no aplica
     * Restricciones: ninguna
     * Descripcion: encabezado azul con texto blanco. Se dibuja con un
     *              renderer propio porque la apariencia de Windows ignora el
     *              color de fondo del encabezado.
     */
    private static final class RenderEncabezado extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        /**
         * Nombre: getTableCellRendererComponent
         * Entradas: los datos de la celda del encabezado
         * Salidas: el componente ya pintado
         * Restricciones: ninguna
         * Descripcion: ver la descripcion de la clase.
         */
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccionada, boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            setFont(Tema.FUENTE_NEGRITA);
            setBackground(Tema.PRIMARIO);
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDE),
                    BorderFactory.createEmptyBorder(3, 6, 3, 6)));
            return this;
        }
    }
}
