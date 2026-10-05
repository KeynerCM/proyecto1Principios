package com.mycompany.minipc.gui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.mycompany.minipc.gui.Tema;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;

/**
 * Nombre: PanelColas
 * Entradas: la lista de procesos leida de memoria
 * Salidas: el panel "Colas"
 * Restricciones: solo muestra
 * Descripcion: muestra la estructura de lista de procesos como la guarda la
 *              memoria: la cabecera del sistema operativo apunta al primer BCP
 *              y cada BCP al siguiente, en una tabla que dice donde esta cada
 *              BCP y cada programa. Debajo, la misma lista agrupada por
 *              estado: quien tiene la CPU, la cola de listos que usa FCFS y
 *              los que esperan. Cubre "despachador, planificador, lista de
 *              trabajos y cambio de contexto" de la rubrica.
 */
public class PanelColas extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Alto de cada fila de la tabla de la lista. */
    private static final int ALTO_FILA = 17;

    private final ModeloLista modeloLista = new ModeloLista();
    private final JPanel enCpu = fila();
    private final JPanel preparados = fila();
    private final JPanel enEspera = fila();
    private final JPanel suspendidos = fila();

    /**
     * Nombre: PanelColas
     * Entradas: ninguna
     * Salidas: el panel construido, vacio
     * Restricciones: ninguna
     * Descripcion: arriba la tabla de la lista enlazada, con lugar para los
     *              cinco BCP para que no cambie de alto; abajo las cuatro
     *              colas en dos columnas, cada una con su rotulo arriba.
     */
    public PanelColas() {
        super(new BorderLayout(0, 6));
        setBackground(Tema.TARJETA);

        JTable tabla = new JTable(modeloLista);
        Tablas.estilo(tabla, new int[]{50, 100, 165, 110});
        tabla.setRowHeight(ALTO_FILA);
        tabla.setRowSelectionAllowed(false);
        tabla.setFocusable(false);
        tabla.getColumnModel().getColumn(0).setCellRenderer(new RenderProceso());
        JPanel cuerpoTabla = new JPanel(new BorderLayout());
        cuerpoTabla.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        cuerpoTabla.add(tabla.getTableHeader(), BorderLayout.NORTH);
        cuerpoTabla.add(tabla, BorderLayout.CENTER);

        tabla.getTableHeader().setToolTipText("La lista de procesos en memoria: la cabecera del"
                + " SO apunta al primer BCP y cada BCP al siguiente (campo SIGUIENTE)");

        JPanel colas = new JPanel(new GridLayout(2, 2, 8, 2));
        colas.setBackground(Tema.TARJETA);
        colas.add(conRotulo("En CPU", enCpu));
        colas.add(conRotulo("Preparados (FCFS)", preparados));
        colas.add(conRotulo("En espera (teclado)", enEspera));
        colas.add(conRotulo("Suspendidos", suspendidos));

        add(cuerpoTabla, BorderLayout.NORTH);
        add(colas, BorderLayout.CENTER);
        mostrar(List.of());
    }

    /**
     * Nombre: mostrar
     * Entradas: procesos, la lista de procesos en el orden de los enlaces
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: vuelve a dibujar la tabla y todas las filas.
     */
    public void mostrar(List<Proceso> procesos) {
        modeloLista.mostrar(procesos);

        llenar(enCpu, procesos, EstadoProceso.EJECUCION);
        llenar(preparados, procesos, EstadoProceso.PREPARADO);
        llenar(enEspera, procesos, EstadoProceso.EN_ESPERA);
        llenar(suspendidos, procesos, EstadoProceso.SUSPENDIDO_PREPARADO,
                EstadoProceso.SUSPENDIDO_EN_ESPERA);
        revalidate();
        repaint();
    }

    /**
     * Nombre: llenar
     * Entradas: fila, donde dibujar; procesos, lista completa; estados, los
     *           que van en esa fila
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: los procesos se muestran en el orden de la lista, que para
     *              los PREPARADO es el orden de llegada a la cola de listos.
     */
    private static void llenar(JPanel fila, List<Proceso> procesos, EstadoProceso... estados) {
        fila.removeAll();
        boolean alguno = false;
        for (Proceso proceso : procesos) {
            for (EstadoProceso estado : estados) {
                if (proceso.getEstado() == estado) {
                    fila.add(chip(proceso, ""));
                    alguno = true;
                }
            }
        }
        if (!alguno) {
            fila.add(texto("ninguno"));
        }
    }

    /**
     * Nombre: chip
     * Entradas: proceso; extra, texto adicional
     * Salidas: una etiqueta con el color del proceso
     * Restricciones: ninguna
     * Descripcion: el mismo color que el proceso tiene en la memoria.
     */
    private static JLabel chip(Proceso proceso, String extra) {
        JLabel etiqueta = new JLabel(" " + proceso + extra + " ");
        etiqueta.setOpaque(true);
        etiqueta.setBackground(Tema.colorProceso(proceso.getRanura()));
        etiqueta.setFont(Tema.FUENTE_NEGRITA);
        etiqueta.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        etiqueta.setToolTipText(proceso.getPrograma() + " - " + proceso.getEstado());
        return etiqueta;
    }

    /**
     * Nombre: texto
     * Entradas: contenido
     * Salidas: una etiqueta gris
     * Restricciones: ninguna
     * Descripcion: para las flechas y "ninguno".
     */
    private static JLabel texto(String contenido) {
        JLabel etiqueta = new JLabel(contenido);
        etiqueta.setFont(Tema.FUENTE);
        etiqueta.setForeground(Tema.TEXTO_SUAVE);
        // Un margen a la derecha para que la ultima letra no se recorte.
        etiqueta.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
        return etiqueta;
    }

    /**
     * Nombre: fila
     * Entradas: ninguna
     * Salidas: un panel de flujo para una cola
     * Restricciones: ninguna
     * Descripcion: los chips se acomodan de izquierda a derecha.
     */
    private static JPanel fila() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        panel.setBackground(Tema.TARJETA);
        return panel;
    }

    /**
     * Nombre: conRotulo
     * Entradas: rotulo; contenido
     * Salidas: el rotulo arriba y la cola debajo
     * Restricciones: ninguna
     * Descripcion: el rotulo va arriba para que toda la mitad del ancho quede
     *              para los procesos de la cola.
     */
    private static JPanel conRotulo(String rotulo, JPanel contenido) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Tema.TARJETA);
        panel.add(texto(rotulo), BorderLayout.NORTH);
        panel.add(contenido, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Nombre: ModeloLista
     * Entradas: no aplica
     * Salidas: no aplica
     * Restricciones: siempre tiene cinco filas, una por ranura de BCP; las
     *                que sobran quedan vacias
     * Descripcion: la lista enlazada de BCP como tabla: el proceso, donde
     *              esta su BCP en el kernel (no se mueve nunca), donde esta
     *              su programa (cambia cuando el intercambio lo mueve; si esta
     *              suspendido, es una direccion del disco) y a quien apunta su
     *              campo SIGUIENTE.
     */
    private static final class ModeloLista extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private static final String[] COLUMNAS = {"Proc", "BCP en", "Programa en", "Siguiente"};

        private final List<Proceso> procesos = new ArrayList<>();

        /**
         * Nombre: mostrar
         * Entradas: lista, los procesos en el orden de los enlaces
         * Salidas: ninguna
         * Restricciones: ninguna
         * Descripcion: guarda la lista y refresca la tabla.
         */
        void mostrar(List<Proceso> lista) {
            procesos.clear();
            procesos.addAll(lista);
            fireTableDataChanged();
        }

        /**
         * Nombre: proceso
         * Entradas: fila
         * Salidas: el proceso de esa fila, o nulo si la fila esta vacia
         * Restricciones: ninguna
         * Descripcion: lo usa el renderer para el color.
         */
        Proceso proceso(int fila) {
            return fila < procesos.size() ? procesos.get(fila) : null;
        }

        /**
         * Nombre: getRowCount
         * Entradas: ninguna
         * Salidas: cinco, una fila por ranura de BCP
         * Restricciones: ninguna
         * Descripcion: el alto de la tabla no cambia.
         */
        @Override
        public int getRowCount() {
            return TablaBCP.MAX_PROCESOS;
        }

        /**
         * Nombre: getColumnCount
         * Entradas: ninguna
         * Salidas: cuatro columnas
         * Restricciones: ninguna
         * Descripcion: ver COLUMNAS.
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
         * Descripcion: lo usa el encabezado.
         */
        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        /**
         * Nombre: isCellEditable
         * Entradas: fila y columna
         * Salidas: false
         * Restricciones: ninguna
         * Descripcion: la tabla es de solo lectura.
         */
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }

        /**
         * Nombre: getValueAt
         * Entradas: fila y columna
         * Salidas: el texto de la celda, vacio en las filas sin proceso
         * Restricciones: ninguna
         * Descripcion: lee el BCP en memoria cada vez que se dibuja.
         */
        @Override
        public Object getValueAt(int fila, int columna) {
            Proceso proceso = proceso(fila);
            if (proceso == null) {
                return "";
            }
            switch (columna) {
                case 0:
                    return proceso.toString();
                case 1:
                    return proceso.getDireccionBCP() + ".." + proceso.getDireccionFinBCP();
                case 2:
                    String rango = proceso.getBase() + ".."
                            + (proceso.getBase() + proceso.getAlcance() - 1);
                    return proceso.estaEnMemoriaVirtual() ? "disco " + rango : rango;
                default:
                    Proceso siguiente = proceso.getSiguiente();
                    return siguiente == null ? "fin" : siguiente.toString();
            }
        }
    }

    /**
     * Nombre: RenderProceso
     * Entradas: no aplica
     * Salidas: no aplica
     * Restricciones: ninguna
     * Descripcion: pinta la columna del proceso con su color, el mismo que
     *              tiene en la memoria y en las colas.
     */
    private static final class RenderProceso extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        /**
         * Nombre: getTableCellRendererComponent
         * Entradas: los datos de la celda
         * Salidas: el componente ya pintado
         * Restricciones: ninguna
         * Descripcion: ver la descripcion de la clase.
         */
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccionada, boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            Proceso proceso = ((ModeloLista) tabla.getModel()).proceso(fila);
            setBackground(proceso == null ? Tema.TARJETA : Tema.colorProceso(proceso.getRanura()));
            setFont(Tema.FUENTE_NEGRITA);
            return this;
        }
    }
}
