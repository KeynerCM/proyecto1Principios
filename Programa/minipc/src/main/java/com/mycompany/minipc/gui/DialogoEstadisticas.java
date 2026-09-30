package com.mycompany.minipc.gui;

import com.mycompany.minipc.hardware.Estadisticas;
import com.mycompany.minipc.hardware.Procesador;
import com.mycompany.minipc.gui.modelo.ModeloTablaEstadisticas;
import com.mycompany.minipc.isa.OpCode;
import com.mycompany.minipc.so.procesos.BCP;

import javax.swing.JProgressBar;

/**
 * Nombre: DialogoEstadisticas
 * Entradas: la ventana padre y el controlador del que se leen los datos
 * Salidas: la presentacion en pantalla del resumen de la ejecucion
 * Restricciones: toma una foto de los datos al abrirse; si la ejecucion
 *                continua despues, hay que volver a abrirlo para verla
 * Descripcion: resumen de la ejecucion. Muestra tres cosas: el estado en que
 *              quedo el proceso, cuantas veces se ejecuto cada operacion, y el
 *              detalle de accesos y tiempos. El reparto por operacion se
 *              dibuja con barras de proporcion en lugar de solo numeros, para
 *              que se vea de un golpe cual domina el programa.
 */
public class DialogoEstadisticas extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private final ModeloTablaEstadisticas modelo;

    /**
     * Nombre: DialogoEstadisticas
     * Entradas: padre, ventana sobre la que se muestra; modal, true para
     *           bloquear la ventana de atras; controlador, del que se toman
     *           los datos
     * Salidas: el dialogo construido y ya poblado
     * Restricciones: el controlador no debe ser nulo; el BCP si puede serlo, y
     *                en ese caso el encabezado muestra guiones
     * Descripcion: arma los componentes y llena de una vez las tres secciones
     *              con los datos actuales del procesador.
     */
    public DialogoEstadisticas(java.awt.Frame padre, boolean modal,
            ControladorPrincipal controlador) {
        super(padre, modal);
        initComponents();

        this.modelo = new ModeloTablaEstadisticas();
        tblEstadisticas.setModel(modelo);

        Procesador cpu = controlador.getProcesador();
        Estadisticas datos = controlador.obtenerEstadisticas();

        llenarEncabezado(cpu);
        llenarOperaciones(datos);
        llenarDetalle(cpu, datos);

        getRootPane().setDefaultButton(btnCerrar);
        pack();
        setLocationRelativeTo(padre);
    }

    /**
     * Nombre: llenarEncabezado
     * Entradas: cpu, procesador del que se leen el BCP y la memoria
     * Salidas: ninguna; actualiza las etiquetas y la barra del resumen
     * Restricciones: tolera que el BCP sea nulo
     * Descripcion: muestra el programa, el estado final del proceso y la
     *              ocupacion de la zona de usuario, esta ultima con el
     *              porcentaje y las cifras absolutas a la vez.
     */
    private void llenarEncabezado(Procesador cpu) {
        BCP bcp = cpu.getBcp();
        lblProgramaValor.setText(bcp != null ? bcp.getNombrePrograma() : "-");
        lblEstadoFinalValor.setText(bcp != null ? bcp.getEstado().name() : "-");

        int porcentaje = cpu.getMemoria().getPorcentajeUso();
        pbUsoMemoria.setValue(porcentaje);
        pbUsoMemoria.setString(porcentaje + " %  ("
                + cpu.getMemoria().getPosicionesUsadas() + " de "
                + cpu.getMemoria().getEspacioUsuario() + " posiciones)");
    }

    /**
     * Nombre: llenarOperaciones
     * Entradas: datos, contabilidad de la ejecucion
     * Salidas: ninguna; actualiza las cinco barras y sus etiquetas
     * Restricciones: ninguna
     * Descripcion: dibuja una barra por operacion, proporcional al total
     *              ejecutado, que es el grafico de barras por tipo de
     *              operacion que pedia el diseno.
     */
    private void llenarOperaciones(Estadisticas datos) {
        int total = datos.getTotalInstrucciones();
        barra(pbMov, lblMovValor, datos.getConteo(OpCode.MOV), total);
        barra(pbLoad, lblLoadValor, datos.getConteo(OpCode.LOAD), total);
        barra(pbStore, lblStoreValor, datos.getConteo(OpCode.STORE), total);
        barra(pbAdd, lblAddValor, datos.getConteo(OpCode.ADD), total);
        barra(pbSub, lblSubValor, datos.getConteo(OpCode.SUB), total);
    }

    /**
     * Nombre: barra
     * Entradas: barra, indicador a ajustar; etiqueta, donde se escribe la
     *           cifra; conteo, veces que se ejecuto la operacion; total,
     *           instrucciones ejecutadas en total
     * Salidas: ninguna
     * Restricciones: si el total es cero el maximo se fuerza a uno, porque un
     *                JProgressBar con maximo cero se dibuja mal
     * Descripcion: ajusta una barra a la proporcion que representa la
     *              operacion y escribe al lado el conteo y su porcentaje.
     */
    private void barra(JProgressBar barra, javax.swing.JLabel etiqueta,
            int conteo, int total) {
        barra.setMaximum(Math.max(total, 1));
        barra.setValue(conteo);
        int porcentaje = total == 0 ? 0 : (conteo * 100) / total;
        etiqueta.setText(conteo + "  (" + porcentaje + " %)");
    }

    /**
     * Nombre: llenarDetalle
     * Entradas: cpu, procesador del que se leen memoria y ciclos; datos,
     *           contabilidad de la ejecucion
     * Salidas: ninguna; llena la tabla de metricas
     * Restricciones: llama a refrescar una sola vez al final, para no
     *                redibujar la tabla por cada fila agregada
     * Descripcion: agrega las diez metricas del detalle. La fila de escrituras
     *              dice "en registros" y no "en memoria" porque en este juego
     *              de instrucciones ninguna operacion escribe datos en una
     *              direccion: STORE copia el acumulador a un registro.
     */
    private void llenarDetalle(Procesador cpu, Estadisticas datos) {
        modelo.agregar("Instrucciones ejecutadas", datos.getTotalInstrucciones());
        modelo.agregar("Ciclos de reloj", cpu.getCiclosReloj());
        modelo.agregar("Lecturas de memoria", datos.getAccesosLectura());
        modelo.agregar("Escrituras en registros", datos.getAccesosEscritura());
        modelo.agregar("Posiciones de memoria ocupadas",
                cpu.getMemoria().getPosicionesUsadas());
        modelo.agregar("Tamano total de la memoria",
                cpu.getMemoria().getTamano() + " posiciones");
        modelo.agregar("Zona de kernel",
                "0 a " + (cpu.getMemoria().getLimiteKernel() - 1));
        modelo.agregar("Zona de usuario", cpu.getMemoria().getLimiteKernel()
                + " a " + (cpu.getMemoria().getTamano() - 1));
        modelo.agregar("Direccion base del programa", cpu.getDireccionBase());
        modelo.agregar("Tiempo de ejecucion", datos.getTiempoTotalMs() + " ms");
        modelo.refrescar();
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
        lblPrograma = new javax.swing.JLabel();
        lblProgramaValor = new javax.swing.JLabel();
        lblEstadoFinal = new javax.swing.JLabel();
        lblEstadoFinalValor = new javax.swing.JLabel();
        lblOcupacion = new javax.swing.JLabel();
        pbUsoMemoria = new javax.swing.JProgressBar();
        pnlCentro = new javax.swing.JPanel();
        pnlOperaciones = new javax.swing.JPanel();
        lblMov = new javax.swing.JLabel();
        pbMov = new javax.swing.JProgressBar();
        lblMovValor = new javax.swing.JLabel();
        lblLoad = new javax.swing.JLabel();
        pbLoad = new javax.swing.JProgressBar();
        lblLoadValor = new javax.swing.JLabel();
        lblStore = new javax.swing.JLabel();
        pbStore = new javax.swing.JProgressBar();
        lblStoreValor = new javax.swing.JLabel();
        lblAdd = new javax.swing.JLabel();
        pbAdd = new javax.swing.JProgressBar();
        lblAddValor = new javax.swing.JLabel();
        lblSub = new javax.swing.JLabel();
        pbSub = new javax.swing.JProgressBar();
        lblSubValor = new javax.swing.JLabel();
        pnlDetalle = new javax.swing.JPanel();
        scrEstadisticas = new javax.swing.JScrollPane();
        tblEstadisticas = new javax.swing.JTable();
        pnlBotones = new javax.swing.JPanel();
        btnCerrar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Estadisticas de la ejecucion");
        getContentPane().setLayout(new java.awt.BorderLayout());

        pnlEncabezado.setBorder(javax.swing.BorderFactory.createTitledBorder("Resumen"));
        pnlEncabezado.setLayout(new java.awt.GridLayout(0, 2, 10, 4));

        lblPrograma.setText("Programa:");
        pnlEncabezado.add(lblPrograma);

        lblProgramaValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblProgramaValor.setText("-");
        pnlEncabezado.add(lblProgramaValor);

        lblEstadoFinal.setText("Estado del proceso:");
        pnlEncabezado.add(lblEstadoFinal);

        lblEstadoFinalValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblEstadoFinalValor.setText("-");
        pnlEncabezado.add(lblEstadoFinalValor);

        lblOcupacion.setText("Ocupacion de la zona de usuario:");
        pnlEncabezado.add(lblOcupacion);

        pbUsoMemoria.setStringPainted(true);
        pnlEncabezado.add(pbUsoMemoria);

        getContentPane().add(pnlEncabezado, java.awt.BorderLayout.NORTH);

        pnlCentro.setLayout(new java.awt.BorderLayout());

        pnlOperaciones.setBorder(javax.swing.BorderFactory.createTitledBorder("Instrucciones por operacion"));
        pnlOperaciones.setLayout(new java.awt.GridLayout(0, 3, 8, 4));

        lblMov.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblMov.setText("MOV");
        pnlOperaciones.add(lblMov);
        pnlOperaciones.add(pbMov);

        lblMovValor.setText("0");
        pnlOperaciones.add(lblMovValor);

        lblLoad.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblLoad.setText("LOAD");
        pnlOperaciones.add(lblLoad);
        pnlOperaciones.add(pbLoad);

        lblLoadValor.setText("0");
        pnlOperaciones.add(lblLoadValor);

        lblStore.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblStore.setText("STORE");
        pnlOperaciones.add(lblStore);
        pnlOperaciones.add(pbStore);

        lblStoreValor.setText("0");
        pnlOperaciones.add(lblStoreValor);

        lblAdd.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblAdd.setText("ADD");
        pnlOperaciones.add(lblAdd);
        pnlOperaciones.add(pbAdd);

        lblAddValor.setText("0");
        pnlOperaciones.add(lblAddValor);

        lblSub.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblSub.setText("SUB");
        pnlOperaciones.add(lblSub);
        pnlOperaciones.add(pbSub);

        lblSubValor.setText("0");
        pnlOperaciones.add(lblSubValor);

        pnlCentro.add(pnlOperaciones, java.awt.BorderLayout.NORTH);

        pnlDetalle.setBorder(javax.swing.BorderFactory.createTitledBorder("Detalle"));
        pnlDetalle.setLayout(new java.awt.BorderLayout());

        scrEstadisticas.setPreferredSize(new java.awt.Dimension(420, 180));

        tblEstadisticas.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblEstadisticas.setRowHeight(22);
        tblEstadisticas.setShowVerticalLines(false);
        scrEstadisticas.setViewportView(tblEstadisticas);

        pnlDetalle.add(scrEstadisticas, java.awt.BorderLayout.CENTER);

        pnlCentro.add(pnlDetalle, java.awt.BorderLayout.CENTER);

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
    private javax.swing.JLabel lblAdd;
    private javax.swing.JLabel lblAddValor;
    private javax.swing.JLabel lblEstadoFinal;
    private javax.swing.JLabel lblEstadoFinalValor;
    private javax.swing.JLabel lblLoad;
    private javax.swing.JLabel lblLoadValor;
    private javax.swing.JLabel lblMov;
    private javax.swing.JLabel lblMovValor;
    private javax.swing.JLabel lblOcupacion;
    private javax.swing.JLabel lblPrograma;
    private javax.swing.JLabel lblProgramaValor;
    private javax.swing.JLabel lblStore;
    private javax.swing.JLabel lblStoreValor;
    private javax.swing.JLabel lblSub;
    private javax.swing.JLabel lblSubValor;
    private javax.swing.JProgressBar pbAdd;
    private javax.swing.JProgressBar pbLoad;
    private javax.swing.JProgressBar pbMov;
    private javax.swing.JProgressBar pbStore;
    private javax.swing.JProgressBar pbSub;
    private javax.swing.JProgressBar pbUsoMemoria;
    private javax.swing.JPanel pnlBotones;
    private javax.swing.JPanel pnlCentro;
    private javax.swing.JPanel pnlDetalle;
    private javax.swing.JPanel pnlEncabezado;
    private javax.swing.JPanel pnlOperaciones;
    private javax.swing.JScrollPane scrEstadisticas;
    private javax.swing.JTable tblEstadisticas;
    // End of variables declaration//GEN-END:variables
}
