package com.mycompany.minipc.gui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.mycompany.minipc.gui.Tema;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: PanelColas
 * Entradas: la lista de procesos leida de memoria
 * Salidas: el panel "Colas"
 * Restricciones: solo muestra
 * Descripcion: muestra la estructura de lista de procesos como la guarda la
 *              memoria: la cabecera del sistema operativo apunta al primer BCP
 *              y cada BCP al siguiente. Debajo, la misma lista agrupada por
 *              estado: quien tiene la CPU, la cola de listos que usa FCFS y
 *              los que esperan. Cubre "despachador, planificador, lista de
 *              trabajos y cambio de contexto" de la rubrica.
 */
public class PanelColas extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel enlaces = new JLabel();
    private final JPanel enCpu = fila();
    private final JPanel preparados = fila();
    private final JPanel enEspera = fila();
    private final JPanel suspendidos = fila();

    /**
     * Nombre: PanelColas
     * Entradas: ninguna
     * Salidas: el panel construido, vacio
     * Restricciones: ninguna
     * Descripcion: una fila por cola, con su rotulo a la izquierda.
     */
    public PanelColas() {
        super(new BorderLayout(0, 6));
        setBackground(Tema.TARJETA);

        JPanel lista = new JPanel(new BorderLayout(0, 2));
        lista.setBackground(Tema.TARJETA);
        lista.add(texto("Lista en memoria (cabecera y enlaces SIGUIENTE):"), BorderLayout.NORTH);
        enlaces.setFont(Tema.FUENTE);
        lista.add(enlaces, BorderLayout.CENTER);

        JPanel colas = new JPanel(new GridLayout(0, 1, 0, 2));
        colas.setBackground(Tema.TARJETA);
        colas.add(conRotulo("En CPU", enCpu));
        colas.add(conRotulo("Preparados (FCFS)", preparados));
        colas.add(conRotulo("En espera (teclado)", enEspera));
        colas.add(conRotulo("Suspendidos", suspendidos));

        add(lista, BorderLayout.NORTH);
        add(colas, BorderLayout.CENTER);
        mostrar(List.of());
    }

    /**
     * Nombre: mostrar
     * Entradas: procesos, la lista de procesos en el orden de los enlaces
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: vuelve a dibujar todas las filas.
     */
    public void mostrar(List<Proceso> procesos) {
        // Texto HTML con ancho fijo para que la lista se parta en varias lineas
        // si no cabe; cada proceso con su color y la direccion de su BCP.
        StringBuilder html = new StringBuilder("<html><div style='width:230px'>SO");
        for (Proceso proceso : procesos) {
            Color color = Tema.colorProceso(proceso.getRanura());
            html.append(" &rarr; <span style='background-color:")
                    .append(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(),
                            color.getBlue()))
                    .append("'>&nbsp;<b>").append(proceso).append("</b> en ")
                    .append(proceso.getDireccionBCP()).append("&nbsp;</span>");
        }
        enlaces.setText(html.append(" &rarr; fin</div></html>").toString());

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
     * Salidas: el rotulo a la izquierda y la cola a la derecha
     * Restricciones: ninguna
     * Descripcion: todas las filas con el rotulo del mismo ancho.
     */
    private static JPanel conRotulo(String rotulo, JPanel contenido) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Tema.TARJETA);
        JLabel etiqueta = texto(rotulo);
        etiqueta.setPreferredSize(new Dimension(125, 22));
        panel.add(etiqueta, BorderLayout.WEST);
        panel.add(contenido, BorderLayout.CENTER);
        return panel;
    }
}
