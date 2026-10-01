package com.mycompany.minipc.gui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;

import com.mycompany.minipc.gui.Tema;
import com.mycompany.minipc.gui.modelo.ModeloTablaTrabajos;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.trabajos.ListaTrabajos;

/**
 * Nombre: PanelListaTrabajos
 * Entradas: el modelo de la tabla de la lista de trabajos
 * Salidas: la tabla "Procesos / Estados" de la maqueta
 * Restricciones: solo muestra; no cambia ningun trabajo
 * Descripcion: la lista de trabajos con el estado de cada uno, en color.
 *              Cuando un trabajo cambia de estado, su fila destella un momento
 *              para que el cambio se note, como pide el enunciado: "visualizar
 *              la lista de trabajos y su estado, y como va cambiando".
 */
public class PanelListaTrabajos extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Milisegundos que dura el destello de una fila que cambio de estado. */
    private static final long DURACION_DESTELLO = 1200;

    private final ModeloTablaTrabajos modelo;
    private final JTable tabla;
    private final JLabel contador;

    /** Ultimo estado visto de cada fila, para detectar cambios. */
    private final Map<Integer, String> ultimoEstado = new HashMap<>();

    /** Momento en que cada fila cambio de estado por ultima vez. */
    private final Map<Integer, Long> cambio = new HashMap<>();

    /** Redibuja mientras haya destellos activos. */
    private final Timer destello;

    /**
     * Nombre: PanelListaTrabajos
     * Entradas: modelo, modelo de la tabla de trabajos
     * Salidas: el panel construido
     * Restricciones: ninguna
     * Descripcion: arma la tabla con su renderer de estados y el contador
     *              "n de 20".
     */
    public PanelListaTrabajos(ModeloTablaTrabajos modelo) {
        super(new BorderLayout(0, 4));
        this.modelo = modelo;
        setBackground(Tema.TARJETA);

        tabla = new JTable(modelo);
        Tablas.estilo(tabla, new int[]{30, 115, 120, 58, 58});
        tabla.setFont(Tema.FUENTE);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setDefaultRenderer(Object.class, new RenderEstado());

        contador = new JLabel();
        contador.setFont(Tema.FUENTE);
        contador.setForeground(Tema.TEXTO_SUAVE);

        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(contador, BorderLayout.SOUTH);

        destello = new Timer(150, e -> {
            tabla.repaint();
            if (!hayDestellos()) {
                ((Timer) e.getSource()).stop();
            }
        });
        refrescar();
    }

    /**
     * Nombre: refrescar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: detecta los cambios de estado, arranca el destello y
     *              redibuja la tabla.
     */
    public void refrescar() {
        long ahora = System.currentTimeMillis();
        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            String estado = String.valueOf(modelo.getValueAt(fila, 2));
            String anterior = ultimoEstado.put(fila, estado);
            if (anterior != null && !anterior.equals(estado)) {
                cambio.put(fila, ahora);
            }
        }
        if (modelo.getRowCount() < ultimoEstado.size()) {
            ultimoEstado.clear();
            cambio.clear();
        }
        contador.setText(modelo.getRowCount() + " de " + ListaTrabajos.CAPACIDAD + " trabajos");
        if (hayDestellos() && !destello.isRunning()) {
            destello.start();
        }
        tabla.repaint();
    }

    /**
     * Nombre: hayDestellos
     * Entradas: ninguna
     * Salidas: true si alguna fila sigue destellando
     * Restricciones: ninguna
     * Descripcion: el destello dura DURACION_DESTELLO milisegundos.
     */
    private boolean hayDestellos() {
        long ahora = System.currentTimeMillis();
        return cambio.values().stream().anyMatch(t -> ahora - t < DURACION_DESTELLO);
    }

    /**
     * Nombre: RenderEstado
     * Entradas: no aplica
     * Salidas: no aplica
     * Restricciones: ninguna
     * Descripcion: pinta la columna Estado con el color del estado y el
     *              fondo de la fila que acaba de cambiar.
     */
    private final class RenderEstado extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        /**
         * Nombre: getTableCellRendererComponent
         * Entradas: los datos de la celda que Swing pide dibujar
         * Salidas: el componente ya pintado
         * Restricciones: respeta el color de seleccion
         * Descripcion: ver la descripcion de la clase.
         */
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor,
                boolean seleccionada, boolean foco, int fila, int columna) {
            Component celda = super.getTableCellRendererComponent(t, valor, seleccionada, foco,
                    fila, columna);
            celda.setFont(Tema.FUENTE);
            if (seleccionada) {
                return celda;
            }
            Long momento = cambio.get(fila);
            boolean destella = momento != null
                    && System.currentTimeMillis() - momento < DURACION_DESTELLO;
            celda.setBackground(destella ? Tema.DESTELLO : Tema.TARJETA);
            celda.setForeground(Tema.TEXTO);
            if (columna == 2) {
                String texto = String.valueOf(valor);
                celda.setFont(Tema.FUENTE.deriveFont(Font.BOLD));
                celda.setForeground(texto.endsWith("(error)") ? Tema.colorError()
                        : Tema.colorEstado(EstadoProceso.valueOf(texto.split(" ")[0])));
            }
            return celda;
        }
    }
}
