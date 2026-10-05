package com.mycompany.minipc.gui;

import java.awt.Component;
import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.mycompany.minipc.gui.modelo.ModeloTablaDuraciones;
import com.mycompany.minipc.gui.modelo.ModeloTablaEstadisticas;
import com.mycompany.minipc.gui.panel.Tablas;
import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.hardware.Estadisticas;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.so.SistemaOperativo;
import com.mycompany.minipc.so.procesos.TablaBCP;
import com.mycompany.minipc.so.trabajos.Trabajo;

/**
 * Nombre: DialogoEstadisticas
 * Entradas: la ventana padre y el controlador del que se leen los datos
 * Salidas: la presentacion en pantalla de las estadisticas de la ejecucion
 * Restricciones: toma una foto de los datos al abrirse; si la ejecucion
 *                continua despues, hay que volver a abrirlo para verla
 * Descripcion: las estadisticas que pide el enunciado al final de la
 *              ejecucion: por cada proceso, su hora de inicio, su hora final
 *              y su duracion en segundos, medidas con el reloj simulado (un
 *              segundo por unidad de peso, como lo confirmo el profesor).
 *              Arriba va un resumen (trabajos, reloj, uso de la CPU y de la
 *              memoria) y abajo los contadores de la CPU.
 */
public class DialogoEstadisticas extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * Nombre: DialogoEstadisticas
     * Entradas: padre, ventana sobre la que se muestra; modal, true para
     *           bloquear la ventana de atras; controlador, del que se toman
     *           los datos
     * Salidas: el dialogo construido y ya poblado
     * Restricciones: el controlador no debe ser nulo
     * Descripcion: arma los componentes y llena las tres secciones con los
     *              datos actuales del sistema operativo.
     */
    public DialogoEstadisticas(java.awt.Frame padre, boolean modal,
            ControladorPrincipal controlador) {
        super(padre, modal);
        initComponents();

        SistemaOperativo so = controlador.getSistemaOperativo();
        List<Trabajo> trabajos = so.getListaTrabajos().getTrabajos();
        int cpuOcupada = 0;
        for (Trabajo trabajo : trabajos) {
            cpuOcupada += ModeloTablaDuraciones.tiempoCpu(trabajo);
        }

        llenarResumen(so, trabajos, cpuOcupada);
        llenarProcesos(trabajos);
        llenarContadores(so, controlador.obtenerEstadisticas(), trabajos, cpuOcupada);

        Tema.pintarBoton(btnCerrar, Tema.BOTON_UTILIDAD);
        getRootPane().setDefaultButton(btnCerrar);
        pack();
        setLocationRelativeTo(padre);
    }

    /**
     * Nombre: llenarResumen
     * Entradas: so, sistema operativo; trabajos, los de la lista de trabajos;
     *           cpuOcupada, segundos en que la CPU ejecuto algun proceso
     * Salidas: ninguna; actualiza las etiquetas y las barras del resumen
     * Restricciones: ninguna
     * Descripcion: cuantos trabajos hay y como terminaron, el reloj simulado,
     *              el porcentaje del tiempo en que la CPU estuvo ocupada y la
     *              ocupacion maxima de la zona de usuario. Se usa el maximo y
     *              no la ocupacion actual porque al terminar todos los
     *              procesos ya liberaron su memoria y la actual es cero.
     */
    private void llenarResumen(SistemaOperativo so, List<Trabajo> trabajos, int cpuOcupada) {
        int finalizados = 0;
        int conError = 0;
        for (Trabajo trabajo : trabajos) {
            if (trabajo.getEstado().esFinal()) {
                finalizados++;
            }
            if (trabajo.getError() != null) {
                conError++;
            }
        }
        lblTrabajosValor.setText(trabajos.size() + " (" + finalizados + " finalizados"
                + (conError == 0 ? "" : ", " + conError + " con error") + ")");
        int reloj = so.getReloj();
        lblRelojValor.setText(SistemaOperativo.formatearReloj(reloj) + "  (" + reloj + " s)");
        barra(pbCpu, cpuOcupada, reloj, cpuOcupada + " de " + reloj + " s");

        Memoria memoria = so.getMemoria();
        int maxima = so.getOcupacionMaxima();
        barra(pbUsoMemoria, maxima, memoria.getEspacioUsuario(),
                maxima + " de " + memoria.getEspacioUsuario() + " posiciones a la vez");
    }

    /**
     * Nombre: barra
     * Entradas: barra, indicador a ajustar; valor y total, la proporcion;
     *           detalle, texto que acompana al porcentaje
     * Salidas: ninguna
     * Restricciones: si el total es cero el maximo se fuerza a uno, porque un
     *                JProgressBar con maximo cero se dibuja mal
     * Descripcion: ajusta la barra y escribe el porcentaje y el detalle.
     */
    private static void barra(JProgressBar barra, int valor, int total, String detalle) {
        barra.setMaximum(Math.max(total, 1));
        barra.setValue(valor);
        int porcentaje = total == 0 ? 0 : (valor * 100) / total;
        barra.setString(porcentaje + " %  (" + detalle + ")");
    }

    /**
     * Nombre: llenarProcesos
     * Entradas: trabajos, los de la lista de trabajos
     * Salidas: ninguna; llena la tabla de duraciones
     * Restricciones: ninguna
     * Descripcion: una fila por proceso con inicio, fin, duracion, tiempo de
     *              CPU, espera y resultado. Los errores se pintan en rojo.
     */
    private void llenarProcesos(List<Trabajo> trabajos) {
        tblProcesos.setModel(new ModeloTablaDuraciones(trabajos));
        Tablas.estilo(tblProcesos, new int[]{60, 170, 75, 75, 90, 60, 75, 255});
        tblProcesos.getColumnModel().getColumn(7).setCellRenderer(new RenderResultado());
    }

    /**
     * Nombre: llenarContadores
     * Entradas: so, sistema operativo; datos, contadores de la CPU;
     *           trabajos, para el promedio; cpuOcupada, segundos de CPU usados
     * Salidas: ninguna; llena la tabla de contadores
     * Restricciones: llama a refrescar una sola vez al final
     * Descripcion: lo que conto la CPU (instrucciones, lecturas y escrituras),
     *              el tiempo ocupado y ocioso, la duracion promedio y como
     *              quedo repartida la memoria.
     */
    private void llenarContadores(SistemaOperativo so, Estadisticas datos,
            List<Trabajo> trabajos, int cpuOcupada) {
        ModeloTablaEstadisticas modelo = new ModeloTablaEstadisticas();
        int suma = 0;
        int terminados = 0;
        for (Trabajo trabajo : trabajos) {
            if (trabajo.getFin() >= 0) {
                suma += trabajo.getFin() - trabajo.getInicio();
                terminados++;
            }
        }
        Memoria memoria = so.getMemoria();
        Disco disco = so.getDisco();
        modelo.agregar("Instrucciones ejecutadas", datos.getTotalInstrucciones());
        modelo.agregar("Lecturas de memoria (fetch)", datos.getAccesosLectura());
        modelo.agregar("Escrituras en registros", datos.getAccesosEscritura());
        modelo.agregar("CPU ocupada / ociosa", cpuOcupada + " s / "
                + Math.max(0, so.getReloj() - cpuOcupada) + " s");
        modelo.agregar("Duracion promedio", terminados == 0 ? "-"
                : String.format("%.1f s (%d procesos finalizados)",
                        (double) suma / terminados, terminados));
        modelo.agregar("Kernel", "0 a " + (memoria.getLimiteKernel() - 1) + ": "
                + TablaBCP.describirFormula());
        modelo.agregar("Zona de usuario", memoria.getLimiteKernel() + " a "
                + (memoria.getTamano() - 1));
        modelo.agregar("Memoria virtual (disco)", disco.getTamanoMemoriaVirtual() == 0
                ? "ninguna" : disco.getInicioMemoriaVirtual() + " a " + (disco.getTamano() - 1)
                + ", uso maximo " + so.getMemoriaVirtualMaxima() + " de "
                + disco.getTamanoMemoriaVirtual() + " posiciones");
        modelo.agregar("Ocupacion actual (usuario)",
                memoria.getPosicionesUsadas() + " de " + memoria.getEspacioUsuario()
                + " posiciones");
        modelo.refrescar();
        tblContadores.setModel(modelo);
        Tablas.estilo(tblContadores, new int[]{220, 640});
    }

    /**
     * Nombre: RenderResultado
     * Entradas: no aplica
     * Salidas: no aplica
     * Restricciones: ninguna
     * Descripcion: pinta en rojo los procesos que terminaron por un error y
     *              muestra el mensaje completo al pasar el raton.
     */
    private static final class RenderResultado extends DefaultTableCellRenderer {

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
            super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            String texto = String.valueOf(valor);
            boolean error = texto.startsWith("Error");
            if (!seleccionada) {
                setForeground(error ? Tema.colorError() : Tema.TEXTO);
            }
            setToolTipText(error ? texto : null);
            return this;
        }
    }

    /**
     * Nombre: initComponents
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: NO editar a mano. El disenador visual de NetBeans
     *                regenera este metodo completo a partir del archivo .form
     *                cada vez que se modifica el dialogo
     * Descripcion: crea los componentes del dialogo, les fija sus propiedades,
     *              los ubica en sus contenedores y conecta el evento del boton
     *              Cerrar.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlEncabezado = new javax.swing.JPanel();
        lblTrabajos = new javax.swing.JLabel();
        lblTrabajosValor = new javax.swing.JLabel();
        lblReloj = new javax.swing.JLabel();
        lblRelojValor = new javax.swing.JLabel();
        lblCpu = new javax.swing.JLabel();
        pbCpu = new javax.swing.JProgressBar();
        lblOcupacion = new javax.swing.JLabel();
        pbUsoMemoria = new javax.swing.JProgressBar();
        pnlCentro = new javax.swing.JPanel();
        pnlProcesos = new javax.swing.JPanel();
        scrProcesos = new javax.swing.JScrollPane();
        tblProcesos = new javax.swing.JTable();
        pnlContadores = new javax.swing.JPanel();
        scrContadores = new javax.swing.JScrollPane();
        tblContadores = new javax.swing.JTable();
        pnlBotones = new javax.swing.JPanel();
        btnCerrar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Estadisticas de la ejecucion");
        getContentPane().setLayout(new java.awt.BorderLayout());

        pnlEncabezado.setBorder(javax.swing.BorderFactory.createTitledBorder("Resumen"));
        pnlEncabezado.setLayout(new java.awt.GridLayout(0, 2, 10, 4));

        lblTrabajos.setText("Trabajos:");
        pnlEncabezado.add(lblTrabajos);

        lblTrabajosValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblTrabajosValor.setText("-");
        pnlEncabezado.add(lblTrabajosValor);

        lblReloj.setText("Reloj simulado:");
        pnlEncabezado.add(lblReloj);

        lblRelojValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblRelojValor.setText("-");
        pnlEncabezado.add(lblRelojValor);

        lblCpu.setText("Uso de la CPU:");
        pnlEncabezado.add(lblCpu);

        pbCpu.setStringPainted(true);
        pnlEncabezado.add(pbCpu);

        lblOcupacion.setText("Ocupacion maxima de la zona de usuario:");
        pnlEncabezado.add(lblOcupacion);

        pbUsoMemoria.setStringPainted(true);
        pnlEncabezado.add(pbUsoMemoria);

        getContentPane().add(pnlEncabezado, java.awt.BorderLayout.NORTH);

        pnlCentro.setLayout(new java.awt.BorderLayout());

        pnlProcesos.setBorder(javax.swing.BorderFactory.createTitledBorder("Duracion de cada proceso"));
        pnlProcesos.setLayout(new java.awt.BorderLayout());

        scrProcesos.setPreferredSize(new java.awt.Dimension(860, 170));

        tblProcesos.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        scrProcesos.setViewportView(tblProcesos);

        pnlProcesos.add(scrProcesos, java.awt.BorderLayout.CENTER);

        pnlCentro.add(pnlProcesos, java.awt.BorderLayout.CENTER);

        pnlContadores.setBorder(javax.swing.BorderFactory.createTitledBorder("Contadores de la CPU"));
        pnlContadores.setLayout(new java.awt.BorderLayout());

        scrContadores.setPreferredSize(new java.awt.Dimension(860, 212));

        tblContadores.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        scrContadores.setViewportView(tblContadores);

        pnlContadores.add(scrContadores, java.awt.BorderLayout.CENTER);

        pnlCentro.add(pnlContadores, java.awt.BorderLayout.SOUTH);

        getContentPane().add(pnlCentro, java.awt.BorderLayout.CENTER);

        pnlBotones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btnCerrar.setText("Cerrar");
        btnCerrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarActionPerformed(evt);
            }
        });
        pnlBotones.add(btnCerrar);

        getContentPane().add(pnlBotones, java.awt.BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Nombre: btnCerrarActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: cierra el dialogo y devuelve el control a la ventana
     *              principal.
     */
    private void btnCerrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarActionPerformed
        dispose();
    }//GEN-LAST:event_btnCerrarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCerrar;
    private javax.swing.JLabel lblCpu;
    private javax.swing.JLabel lblOcupacion;
    private javax.swing.JLabel lblReloj;
    private javax.swing.JLabel lblRelojValor;
    private javax.swing.JLabel lblTrabajos;
    private javax.swing.JLabel lblTrabajosValor;
    private javax.swing.JProgressBar pbCpu;
    private javax.swing.JProgressBar pbUsoMemoria;
    private javax.swing.JPanel pnlBotones;
    private javax.swing.JPanel pnlCentro;
    private javax.swing.JPanel pnlContadores;
    private javax.swing.JPanel pnlEncabezado;
    private javax.swing.JPanel pnlProcesos;
    private javax.swing.JScrollPane scrContadores;
    private javax.swing.JScrollPane scrProcesos;
    private javax.swing.JTable tblContadores;
    private javax.swing.JTable tblProcesos;
    // End of variables declaration//GEN-END:variables
}
