package com.mycompany.minipc.gui;

import com.mycompany.minipc.config.Configuracion;
import com.mycompany.minipc.excepciones.ConfiguracionException;
import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.so.planificacion.FabricaAlgoritmos;
import com.mycompany.minipc.so.procesos.TablaBCP;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;

/**
 * Nombre: DialogoConfiguracion
 * Entradas: la ventana padre y el controlador al que aplicar los cambios
 * Salidas: la nueva configuracion, entregada al controlador al aceptar
 * Restricciones: aplicar la configuracion vacia la memoria y el disco, porque
 *                redimensionarlos invalida las direcciones ya asignadas
 * Descripcion: dialogo de configuracion de la maquina. Permite cambiar el
 *              tamano de la memoria principal, el tamano del disco y de su
 *              memoria virtual, y la duracion de cada segundo de CPU en la
 *              ejecucion automatica. El kernel se muestra calculado y no se
 *              puede editar: ocupa K = C + P x B posiciones (tabla de BCP). Muestra en vivo como
 *              quedan repartidas la memoria y el disco, y deshabilita el boton
 *              Aceptar mientras la combinacion no sea valida, en lugar de
 *              dejar equivocarse y reclamar despues. Al aceptar, el
 *              controlador guarda los valores en el archivo de configuracion.
 */
public class DialogoConfiguracion extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private final ControladorPrincipal controlador;

    /** Algoritmo de planificacion; se agrega desde el constructor. */
    private JComboBox<String> cmbAlgoritmo;

    /**
     * Nombre: DialogoConfiguracion
     * Entradas: padre, ventana sobre la que se muestra; modal, true para
     *           bloquear la ventana de atras; controlador, al que se le
     *           aplicara la configuracion
     * Salidas: el dialogo construido, con los valores actuales ya cargados
     * Restricciones: el controlador no debe ser nulo, porque de el se leen los
     *                valores de partida
     * Descripcion: arma los componentes y les asigna los modelos de los
     *              spinners. Esos modelos se crean aqui y no en el disenador
     *              para que los limites queden junto a las constantes de
     *              Memoria, Disco y Configuracion que los definen, en lugar de
     *              duplicados en el XML.
     */
    public DialogoConfiguracion(java.awt.Frame padre, boolean modal,
            ControladorPrincipal controlador) {
        super(padre, modal);
        this.controlador = controlador;
        initComponents();

        Configuracion actual = controlador.getConfiguracion();
        int minimo = TablaBCP.TAMANO_KERNEL + Memoria.ESPACIO_USUARIO_MINIMO;
        spnTamano.setModel(new SpinnerNumberModel(
                Math.max(actual.getTamanoMemoria(), minimo), minimo, 1024, 32));
        // El kernel no se configura: se muestra el valor calculado.
        lblKernel.setText("Kernel (calculado):");
        spnKernel.setModel(new SpinnerNumberModel(TablaBCP.TAMANO_KERNEL,
                TablaBCP.TAMANO_KERNEL, TablaBCP.TAMANO_KERNEL, 1));
        spnKernel.setEnabled(false);
        spnKernel.setToolTipText("K = " + TablaBCP.describirFormula());
        spnDisco.setModel(new SpinnerNumberModel(
                actual.getTamanoDisco(), Disco.TAMANO_MINIMO, 4096, 64));
        spnMemoriaVirtual.setModel(new SpinnerNumberModel(
                actual.getTamanoMemoriaVirtual(), 0, 2048, 16));
        spnVelocidad.setModel(new SpinnerNumberModel(actual.getMsPorSegundo(),
                Configuracion.MS_POR_SEGUNDO_MINIMO, Configuracion.MS_POR_SEGUNDO_MAXIMO, 50));

        // El algoritmo se elige de los que conoce la fabrica; en este proyecto
        // solo FCFS, y en el proyecto 2 apareceran los demas sin tocar esto.
        cmbAlgoritmo = new JComboBox<>(FabricaAlgoritmos.disponibles().toArray(new String[0]));
        cmbAlgoritmo.setSelectedItem(actual.getAlgoritmo());
        cmbAlgoritmo.setToolTipText("Algoritmo de planificacion de procesos");
        pnlParametros.add(new JLabel("Planificacion:"));
        pnlParametros.add(cmbAlgoritmo);

        actualizarResumen();
        getRootPane().setDefaultButton(btnAceptar);
        pack();
        setLocationRelativeTo(padre);
    }

    /**
     * Nombre: valor
     * Entradas: spinner, control del que se lee el numero
     * Salidas: el entero seleccionado
     * Restricciones: el spinner debe tener un SpinnerNumberModel de enteros
     * Descripcion: concentra la conversion del valor del spinner, que llega
     *              como Object, para no repetir el casteo en cada uso.
     */
    private static int valor(javax.swing.JSpinner spinner) {
        return (Integer) spinner.getValue();
    }

    /**
     * Nombre: configuracionElegida
     * Entradas: ninguna; lee los valores actuales de los spinners
     * Salidas: la configuracion que resulta de lo elegido
     * Restricciones: lanza ConfiguracionException si la combinacion no es
     *                valida
     * Descripcion: delega la validacion en Configuracion, que usa las mismas
     *              reglas que la memoria y el disco, para no repetirlas aqui.
     */
    private Configuracion configuracionElegida() throws ConfiguracionException {
        return new Configuracion(valor(spnTamano), valor(spnDisco),
                valor(spnMemoriaVirtual), valor(spnVelocidad),
                cmbAlgoritmo == null ? controlador.getConfiguracion().getAlgoritmo()
                        : (String) cmbAlgoritmo.getSelectedItem());
    }

    /**
     * Nombre: actualizarResumen
     * Entradas: ninguna; lee los valores actuales de los spinners
     * Salidas: ninguna; actualiza las etiquetas y el estado del boton Aceptar
     * Restricciones: ninguna
     * Descripcion: recalcula como quedan repartidas la memoria y el disco y
     *              avisa si la combinacion elegida no sirve. Si no es valida,
     *              borra el resumen, muestra el primer problema y deshabilita
     *              Aceptar, de modo que el error se previene en vez de
     *              reclamarse despues.
     */
    private void actualizarResumen() {
        Configuracion elegida;
        try {
            elegida = configuracionElegida();
        } catch (ConfiguracionException e) {
            for (javax.swing.JLabel etiqueta : new javax.swing.JLabel[]{lblZonaKernelValor,
                lblZonaUsuarioValor, lblEspacioValor, lblIndiceDiscoValor,
                lblArchivosDiscoValor, lblVirtualDiscoValor}) {
                etiqueta.setText("-");
            }
            lblAviso.setText(e.getErrores().get(0));
            btnAceptar.setEnabled(false);
            return;
        }

        int tamano = elegida.getTamanoMemoria();
        int kernel = TablaBCP.TAMANO_KERNEL;
        int disco = elegida.getTamanoDisco();
        int inicioVirtual = disco - elegida.getTamanoMemoriaVirtual();

        lblZonaKernelValor.setText("0 a " + (kernel - 1));
        lblZonaUsuarioValor.setText(kernel + " a " + (tamano - 1));
        lblEspacioValor.setText((tamano - kernel) + " posiciones");
        lblIndiceDiscoValor.setText("0 a " + (Disco.ENTRADAS_INDICE - 1));
        lblArchivosDiscoValor.setText(Disco.ENTRADAS_INDICE + " a " + (inicioVirtual - 1));
        lblVirtualDiscoValor.setText(elegida.getTamanoMemoriaVirtual() == 0
                ? "(ninguna)" : inicioVirtual + " a " + (disco - 1));
        lblAviso.setText(" ");
        btnAceptar.setEnabled(true);
    }

    /**
     * Nombre: initComponents
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: NO editar a mano. El disenador visual de NetBeans
     *                regenera este metodo completo a partir del archivo .form
     *                cada vez que se modifica el dialogo
     * Descripcion: crea los componentes del dialogo, les fija sus propiedades,
     *              los ubica en sus contenedores y conecta los eventos de los
     *              spinners y de los tres botones.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlParametros = new javax.swing.JPanel();
        lblTamano = new javax.swing.JLabel();
        spnTamano = new javax.swing.JSpinner();
        lblKernel = new javax.swing.JLabel();
        spnKernel = new javax.swing.JSpinner();
        lblDisco = new javax.swing.JLabel();
        spnDisco = new javax.swing.JSpinner();
        lblMemoriaVirtual = new javax.swing.JLabel();
        spnMemoriaVirtual = new javax.swing.JSpinner();
        lblVelocidad = new javax.swing.JLabel();
        spnVelocidad = new javax.swing.JSpinner();
        pnlResumen = new javax.swing.JPanel();
        lblZonaKernel = new javax.swing.JLabel();
        lblZonaKernelValor = new javax.swing.JLabel();
        lblZonaUsuario = new javax.swing.JLabel();
        lblZonaUsuarioValor = new javax.swing.JLabel();
        lblEspacio = new javax.swing.JLabel();
        lblEspacioValor = new javax.swing.JLabel();
        lblIndiceDisco = new javax.swing.JLabel();
        lblIndiceDiscoValor = new javax.swing.JLabel();
        lblArchivosDisco = new javax.swing.JLabel();
        lblArchivosDiscoValor = new javax.swing.JLabel();
        lblVirtualDisco = new javax.swing.JLabel();
        lblVirtualDiscoValor = new javax.swing.JLabel();
        pnlInferior = new javax.swing.JPanel();
        lblAviso = new javax.swing.JLabel();
        pnlBotones = new javax.swing.JPanel();
        btnRestaurar = new javax.swing.JButton();
        btnAceptar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Configuracion del Mini PC");
        setResizable(false);
        getContentPane().setLayout(new java.awt.BorderLayout());

        pnlParametros.setBorder(javax.swing.BorderFactory.createTitledBorder("Parametros"));
        pnlParametros.setLayout(new java.awt.GridLayout(0, 2, 10, 8));

        lblTamano.setText("Tamano de memoria:");
        pnlParametros.add(lblTamano);

        spnTamano.setToolTipText("Cantidad total de posiciones. El enunciado exige un minimo de 128");
        spnTamano.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnTamanoStateChanged(evt);
            }
        });
        pnlParametros.add(spnTamano);

        lblKernel.setText("Limite de kernel:");
        pnlParametros.add(lblKernel);

        spnKernel.setToolTipText("Primera direccion de la zona de usuario");
        spnKernel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnKernelStateChanged(evt);
            }
        });
        pnlParametros.add(spnKernel);

        lblDisco.setText("Tamano del disco:");
        pnlParametros.add(lblDisco);

        spnDisco.setToolTipText("Cantidad total de posiciones del disco. Las primeras 20 son el indice de archivos");
        spnDisco.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnDiscoStateChanged(evt);
            }
        });
        pnlParametros.add(spnDisco);

        lblMemoriaVirtual.setText("Memoria virtual:");
        pnlParametros.add(lblMemoriaVirtual);

        spnMemoriaVirtual.setToolTipText("Posiciones al final del disco reservadas para intercambio");
        spnMemoriaVirtual.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMemoriaVirtualStateChanged(evt);
            }
        });
        pnlParametros.add(spnMemoriaVirtual);

        lblVelocidad.setText("Segundo de CPU (ms):");
        pnlParametros.add(lblVelocidad);

        spnVelocidad.setToolTipText("Milisegundos reales que dura cada segundo de CPU en la ejecucion automatica");
        pnlParametros.add(spnVelocidad);

        getContentPane().add(pnlParametros, java.awt.BorderLayout.NORTH);

        pnlResumen.setBorder(javax.swing.BorderFactory.createTitledBorder("Distribucion resultante"));
        pnlResumen.setLayout(new java.awt.GridLayout(0, 2, 10, 4));

        lblZonaKernel.setText("Zona de kernel:");
        pnlResumen.add(lblZonaKernel);

        lblZonaKernelValor.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblZonaKernelValor.setText("-");
        pnlResumen.add(lblZonaKernelValor);

        lblZonaUsuario.setText("Zona de usuario:");
        pnlResumen.add(lblZonaUsuario);

        lblZonaUsuarioValor.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblZonaUsuarioValor.setText("-");
        pnlResumen.add(lblZonaUsuarioValor);

        lblEspacio.setText("Espacio para programas:");
        pnlResumen.add(lblEspacio);

        lblEspacioValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblEspacioValor.setText("-");
        pnlResumen.add(lblEspacioValor);

        lblIndiceDisco.setText("Indice del disco:");
        pnlResumen.add(lblIndiceDisco);

        lblIndiceDiscoValor.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblIndiceDiscoValor.setText("-");
        pnlResumen.add(lblIndiceDiscoValor);

        lblArchivosDisco.setText("Area de archivos:");
        pnlResumen.add(lblArchivosDisco);

        lblArchivosDiscoValor.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblArchivosDiscoValor.setText("-");
        pnlResumen.add(lblArchivosDiscoValor);

        lblVirtualDisco.setText("Area de memoria virtual:");
        pnlResumen.add(lblVirtualDisco);

        lblVirtualDiscoValor.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblVirtualDiscoValor.setText("-");
        pnlResumen.add(lblVirtualDiscoValor);

        getContentPane().add(pnlResumen, java.awt.BorderLayout.CENTER);

        pnlInferior.setLayout(new java.awt.BorderLayout());

        lblAviso.setForeground(new java.awt.Color(192, 51, 51));
        lblAviso.setText(" ");
        pnlInferior.add(lblAviso, java.awt.BorderLayout.NORTH);

        pnlBotones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btnRestaurar.setText("Valores por defecto");
        btnRestaurar.setToolTipText("Memoria de 256 con kernel de 0 a 127, disco de 512 con 64 de memoria virtual, 1000 ms por segundo");
        btnRestaurar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRestaurarActionPerformed(evt);
            }
        });
        pnlBotones.add(btnRestaurar);

        btnAceptar.setText("Aceptar");
        btnAceptar.setToolTipText("Aplica la configuracion, la guarda en config.properties y vacia la memoria y el disco");
        btnAceptar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAceptarActionPerformed(evt);
            }
        });
        pnlBotones.add(btnAceptar);

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarActionPerformed(evt);
            }
        });
        pnlBotones.add(btnCancelar);

        pnlInferior.add(pnlBotones, java.awt.BorderLayout.SOUTH);

        getContentPane().add(pnlInferior, java.awt.BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Nombre: spnTamanoStateChanged
     * Entradas: evt, evento de cambio del spinner
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: recalcula el resumen cada vez que cambia el tamano de
     *              memoria, para que la vista previa siga al usuario.
     */
    private void spnTamanoStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnTamanoStateChanged
        actualizarResumen();
    }//GEN-LAST:event_spnTamanoStateChanged

    /**
     * Nombre: spnKernelStateChanged
     * Entradas: evt, evento de cambio del spinner
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: recalcula el resumen cada vez que cambia el limite de
     *              kernel.
     */
    private void spnKernelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnKernelStateChanged
        actualizarResumen();
    }//GEN-LAST:event_spnKernelStateChanged

    /**
     * Nombre: spnDiscoStateChanged
     * Entradas: evt, evento de cambio del spinner
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: recalcula el resumen cada vez que cambia el tamano del
     *              disco.
     */
    private void spnDiscoStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnDiscoStateChanged
        actualizarResumen();
    }//GEN-LAST:event_spnDiscoStateChanged

    /**
     * Nombre: spnMemoriaVirtualStateChanged
     * Entradas: evt, evento de cambio del spinner
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: recalcula el resumen cada vez que cambia el tamano de la
     *              memoria virtual.
     */
    private void spnMemoriaVirtualStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnMemoriaVirtualStateChanged
        actualizarResumen();
    }//GEN-LAST:event_spnMemoriaVirtualStateChanged

    /**
     * Nombre: btnRestaurarActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: no aplica nada todavia; solo cambia lo que muestran los
     *                spinners
     * Descripcion: devuelve los valores a los de por defecto, tomandolos de
     *              Configuracion en lugar de escribirlos aqui.
     */
    private void btnRestaurarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRestaurarActionPerformed
        Configuracion porDefecto = Configuracion.porDefecto();
        spnTamano.setValue(porDefecto.getTamanoMemoria());
        spnDisco.setValue(porDefecto.getTamanoDisco());
        spnMemoriaVirtual.setValue(porDefecto.getTamanoMemoriaVirtual());
        spnVelocidad.setValue(porDefecto.getMsPorSegundo());
        actualizarResumen();
    }//GEN-LAST:event_btnRestaurarActionPerformed

    /**
     * Nombre: btnAceptarActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: el boton solo esta habilitado si la combinacion es
     *                valida; aun asi se atrapa la excepcion por seguridad
     * Descripcion: aplica la configuracion a traves del controlador y cierra
     *              el dialogo.
     */
    private void btnAceptarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAceptarActionPerformed
        try {
            controlador.alConfigurar(configuracionElegida());
            dispose();
        } catch (ConfiguracionException e) {
            actualizarResumen();
        }
    }//GEN-LAST:event_btnAceptarActionPerformed

    /**
     * Nombre: btnCancelarActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: cierra el dialogo sin aplicar ningun cambio.
     */
    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAceptar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnRestaurar;
    private javax.swing.JLabel lblArchivosDisco;
    private javax.swing.JLabel lblArchivosDiscoValor;
    private javax.swing.JLabel lblAviso;
    private javax.swing.JLabel lblDisco;
    private javax.swing.JLabel lblEspacio;
    private javax.swing.JLabel lblEspacioValor;
    private javax.swing.JLabel lblIndiceDisco;
    private javax.swing.JLabel lblIndiceDiscoValor;
    private javax.swing.JLabel lblKernel;
    private javax.swing.JLabel lblMemoriaVirtual;
    private javax.swing.JLabel lblTamano;
    private javax.swing.JLabel lblVelocidad;
    private javax.swing.JLabel lblVirtualDisco;
    private javax.swing.JLabel lblVirtualDiscoValor;
    private javax.swing.JLabel lblZonaKernel;
    private javax.swing.JLabel lblZonaKernelValor;
    private javax.swing.JLabel lblZonaUsuario;
    private javax.swing.JLabel lblZonaUsuarioValor;
    private javax.swing.JPanel pnlBotones;
    private javax.swing.JPanel pnlInferior;
    private javax.swing.JPanel pnlParametros;
    private javax.swing.JPanel pnlResumen;
    private javax.swing.JSpinner spnDisco;
    private javax.swing.JSpinner spnKernel;
    private javax.swing.JSpinner spnMemoriaVirtual;
    private javax.swing.JSpinner spnTamano;
    private javax.swing.JSpinner spnVelocidad;
    // End of variables declaration//GEN-END:variables
}
