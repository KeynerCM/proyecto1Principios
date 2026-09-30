package com.mycompany.minipc.gui;

import com.mycompany.minipc.config.LectorConfiguracion;
import com.mycompany.minipc.so.procesos.BCP;
import com.mycompany.minipc.isa.RegistroID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Nombre: VentanaPrincipal
 * Entradas: las acciones que el usuario pulsa sobre sus controles
 * Salidas: la representacion en pantalla del estado del simulador
 * Restricciones: no contiene logica de simulacion; cada boton delega en una
 *                sola llamada al controlador
 * Descripcion: ventana principal del simulador Mini PC. Implementa
 *              VistaPrincipal, de modo que el controlador le pide que muestre
 *              cosas y ella solo dibuja. Reune la barra de herramientas, la
 *              tabla de instrucciones, la tabla de memoria, el panel del BCP y
 *              la consola de actividad.
 */
public class VentanaPrincipal extends javax.swing.JFrame implements VistaPrincipal {

    private static final long serialVersionUID = 1L;

    /** Formato de la hora que precede cada linea de la consola. */
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Carpetas donde buscar los programas de ejemplo al abrir el dialogo. */
    private static final String[] CARPETAS_EJEMPLO = {"../../Ejemplo", "../Ejemplo", "Ejemplo"};

    /** Accion de entrada: traer un programa del disco. */
    private static final Color COLOR_CARGA = new Color(41, 98, 155);

    /** Acciones que hacen avanzar el ciclo de instruccion. */
    private static final Color COLOR_EJECUCION = new Color(34, 124, 78);

    /** Accion que devuelve el procesador al inicio. */
    private static final Color COLOR_REINICIO = new Color(176, 122, 24);

    /** Accion destructiva: descarta lo cargado. */
    private static final Color COLOR_LIMPIEZA = new Color(163, 58, 48);

    /** Acciones auxiliares que no alteran la ejecucion. */
    private static final Color COLOR_UTILIDAD = new Color(84, 95, 110);

    private static final Color FONDO_DESHABILITADO = new Color(206, 208, 211);
    private static final Color TEXTO_DESHABILITADO = new Color(128, 131, 136);

    /** Estado del proceso mientras avanza con normalidad. */
    private static final Color ESTADO_ACTIVO = new Color(34, 124, 78);

    /** Estado del proceso cuando se detuvo por un error. */
    private static final Color ESTADO_ERROR = new Color(163, 58, 48);

    /** Estado del proceso cuando no hay nada cargado. */
    private static final Color ESTADO_NEUTRO = new Color(96, 100, 106);

    private final ControladorPrincipal controlador;

    /** Tabla del disco; se crea fuera del disenador (ver agregarPestanaDisco). */
    private JTable tblDisco;

    /**
     * Nombre: VentanaPrincipal
     * Entradas: ninguna
     * Salidas: la ventana construida y lista para mostrarse
     * Restricciones: debe crearse dentro del hilo de despacho de eventos de
     *                Swing
     * Descripcion: arma los componentes, crea el controlador, le asigna a cada
     *              tabla su modelo y su renderer, ajusta los anchos de columna
     *              y pide al controlador que deje la vista en su estado
     *              inicial.
     */
    public VentanaPrincipal() {
        initComponents();
        this.controlador = new ControladorPrincipal(this, new LectorConfiguracion());

        tblInstrucciones.setModel(controlador.getModeloInstrucciones());
        tblInstrucciones.setDefaultRenderer(Object.class, controlador.getRenderInstrucciones());
        ajustarAnchos(tblInstrucciones.getColumnModel(), new int[]{40, 350});

        tblMemoria.setModel(controlador.getModeloMemoria());
        tblMemoria.setDefaultRenderer(Object.class, controlador.getRenderMemoria());
        ajustarAnchos(tblMemoria.getColumnModel(), new int[]{50, 70, 280});

        agregarPestanaDisco();

        pintarBotones();
        controlador.inicializarVista();
    }

    /**
     * Nombre: agregarPestanaDisco
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: debe llamarse despues de initComponents, cuando la tabla
     *                de memoria ya existe
     * Descripcion: convierte el panel de memoria en dos pestanas, una para la
     *              memoria principal y otra para el disco. Se arma aqui y no
     *              en el disenador para no tocar el codigo generado; el
     *              disenador sigue mostrando el panel de memoria como antes.
     */
    private void agregarPestanaDisco() {
        tblDisco = new JTable(controlador.getModeloDisco());
        tblDisco.setDefaultRenderer(Object.class, controlador.getRenderDisco());
        tblDisco.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblDisco.setRowHeight(tblMemoria.getRowHeight());
        tblDisco.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDisco.setShowVerticalLines(false);
        ajustarAnchos(tblDisco.getColumnModel(), new int[]{50, 70, 280});

        pnlMemoria.remove(scrMemoria);
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Memoria principal", scrMemoria);
        pestanas.addTab("Disco", new JScrollPane(tblDisco));
        pnlMemoria.add(pestanas, java.awt.BorderLayout.CENTER);
        pnlMemoria.setBorder(BorderFactory.createTitledBorder("Almacenamiento"));
    }

    /**
     * Nombre: pintarBotones
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: asigna a cada boton el color de su grupo de acciones, de
     *              modo que el usuario distinga de un vistazo lo que carga, lo
     *              que ejecuta, lo que reinicia y lo que descarta.
     */
    private void pintarBotones() {
        pintarBoton(btnCargar, COLOR_CARGA);
        pintarBoton(btnEjecutar, COLOR_EJECUCION);
        pintarBoton(btnPaso, COLOR_EJECUCION);
        pintarBoton(btnReiniciar, COLOR_REINICIO);
        pintarBoton(btnLimpiar, COLOR_LIMPIEZA);
        pintarBoton(btnConfig, COLOR_UTILIDAD);
        pintarBoton(btnEstadisticas, COLOR_UTILIDAD);
    }

    /**
     * Nombre: pintarBoton
     * Entradas: boton, control a pintar; color, fondo cuando este habilitado
     * Salidas: ninguna
     * Restricciones: hay que apagar el relleno propio de la apariencia del
     *                sistema, porque de lo contrario Windows dibuja su propia
     *                superficie encima y el color asignado no se ve
     * Descripcion: pinta el boton y le agrega un oyente que vuelve a aplicar
     *              el color cada vez que cambia su habilitacion. Sin ese
     *              oyente, un boton deshabilitado conservaria su color vivo y
     *              parecería disponible cuando no lo esta.
     */
    private void pintarBoton(final JButton boton, final Color color) {
        boton.setContentAreaFilled(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        aplicarColor(boton, color);
        boton.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                aplicarColor(boton, color);
            }
        });
    }

    /**
     * Nombre: aplicarColor
     * Entradas: boton, control a pintar; color, fondo para el estado habilitado
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: aplica el color vivo con texto blanco si el boton esta
     *              habilitado, y un gris apagado si no lo esta.
     */
    private void aplicarColor(JButton boton, Color color) {
        if (boton.isEnabled()) {
            boton.setBackground(color);
            boton.setForeground(Color.WHITE);
        } else {
            boton.setBackground(FONDO_DESHABILITADO);
            boton.setForeground(TEXTO_DESHABILITADO);
        }
    }

    /**
     * Nombre: colorDelEstado
     * Entradas: estado, nombre del estado del proceso
     * Salidas: el color con que debe mostrarse ese estado
     * Restricciones: ninguna
     * Descripcion: rojo si el proceso quedo bloqueado por un error, gris si no
     *              hay programa cargado, y verde en cualquier otro caso, que
     *              son los estados en que el proceso avanza con normalidad.
     */
    private Color colorDelEstado(String estado) {
        if ("BLOQUEADO_ERROR".equals(estado)) {
            return ESTADO_ERROR;
        }
        if ("SIN PROGRAMA".equals(estado) || "-".equals(estado)) {
            return ESTADO_NEUTRO;
        }
        return ESTADO_ACTIVO;
    }

    /**
     * Nombre: ajustarAnchos
     * Entradas: columnas, modelo de columnas de la tabla; anchos, ancho
     *           deseado para cada una en pixeles
     * Salidas: ninguna
     * Restricciones: si el arreglo tiene mas entradas que columnas, las de mas
     *                se ignoran
     * Descripcion: fija el ancho preferido de cada columna, para que la
     *              posicion no ocupe lo mismo que el contenido.
     */
    private void ajustarAnchos(TableColumnModel columnas, int[] anchos) {
        for (int i = 0; i < anchos.length && i < columnas.getColumnCount(); i++) {
            columnas.getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

 
    /**
     * Nombre: mostrarInstrucciones
     * Entradas: programa, texto de cada instruccion en orden
     * Salidas: ninguna
     * Restricciones: no usa el parametro, porque el modelo de la tabla lo
     *                mantiene el controlador
     * Descripcion: devuelve la tabla al principio de la lista y la redibuja,
     *              que es lo unico que le corresponde hacer a la vista cuando
     *              hay un programa nuevo.
     */
    @Override
    public void mostrarInstrucciones(List<String> programa) {
        tblInstrucciones.clearSelection();
        tblInstrucciones.scrollRectToVisible(tblInstrucciones.getCellRect(0, 0, true));
        tblInstrucciones.repaint();
    }

    /**
     * Nombre: resaltarInstruccion
     * Entradas: indiceFila, fila a resaltar, o -1 para no resaltar ninguna
     * Salidas: ninguna
     * Restricciones: un indice fuera de rango se ignora sin fallar
     * Descripcion: redibuja la tabla, cuyo renderer ya sabe que fila destacar,
     *              y desplaza la vista para que esa fila quede visible aunque
     *              el programa sea mas largo que la ventana.
     */
    @Override
    public void resaltarInstruccion(int indiceFila) {
        tblInstrucciones.repaint();
        if (indiceFila >= 0 && indiceFila < tblInstrucciones.getRowCount()) {
            tblInstrucciones.scrollRectToVisible(
                    tblInstrucciones.getCellRect(indiceFila, 0, true));
        }
    }

    /**
     * Nombre: refrescarMemoria
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: avisa al modelo de que su contenido cambio y desplaza la
     *              tabla hasta la posicion que apunta el PC, para que el
     *              usuario siga la ejecucion sin tener que buscarla entre las
     *              256 filas.
     */
    @Override
    public void refrescarMemoria() {
        ((AbstractTableModel) tblMemoria.getModel()).fireTableDataChanged();
        int direccion = controlador.getProcesador().getPc();
        if (controlador.getProcesador().hayPrograma()
                && direccion < tblMemoria.getRowCount()) {
            tblMemoria.scrollRectToVisible(tblMemoria.getCellRect(direccion, 0, true));
        }
    }

    /**
     * Nombre: refrescarDisco
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no hace nada si la pestana del disco todavia no existe
     * Descripcion: avisa al modelo del disco de que su contenido cambio, por
     *              ejemplo tras cargar archivos, limpiar o reconfigurar.
     */
    @Override
    public void refrescarDisco() {
        if (tblDisco != null) {
            ((AbstractTableModel) tblDisco.getModel()).fireTableDataChanged();
        }
    }

    /**
     * Nombre: mostrarBCP
     * Entradas: bcp, bloque a mostrar, o nulo si no hay proceso
     * Salidas: ninguna
     * Restricciones: tolera el valor nulo, que ocurre tras descargar el
     *                programa
     * Descripcion: vuelca los catorce atributos del bloque de control en las
     *              etiquetas del panel correspondiente.
     */
    @Override
    public void mostrarBCP(BCP bcp) {
        if (bcp == null) {
            limpiarBCP();
            return;
        }
        lblPidValor.setText(String.valueOf(bcp.getPid()));
        lblProgramaValor.setText(bcp.getNombrePrograma());
        lblEstadoBcpValor.setText(bcp.getEstado().name());
        lblEstadoBcpValor.setForeground(colorDelEstado(bcp.getEstado().name()));
        lblPcValor.setText(String.valueOf(bcp.getPc()));
        lblIrTextoValor.setText(bcp.getIrTexto().isEmpty() ? "-" : bcp.getIrTexto());
        lblAcValor.setText(String.valueOf(bcp.getAc()));
        lblAxValor.setText(String.valueOf(bcp.getRegistro(RegistroID.AX)));
        lblBxValor.setText(String.valueOf(bcp.getRegistro(RegistroID.BX)));
        lblCxValor.setText(String.valueOf(bcp.getRegistro(RegistroID.CX)));
        lblDxValor.setText(String.valueOf(bcp.getRegistro(RegistroID.DX)));
        lblBaseValor.setText(String.valueOf(bcp.getDireccionBase()));
        lblLimiteValor.setText(bcp.getLimite() + " posiciones");
        lblEjecutadasValor.setText(String.valueOf(bcp.getInstruccionesEjecutadas()));
        lblCiclosValor.setText(String.valueOf(bcp.getCiclosReloj()));
        lblHoraCreacionValor.setText(bcp.getHoraCreacion().format(HORA));
    }

    /**
     * Nombre: limpiarBCP
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: deja los catorce campos del BCP en su valor de reposo,
     *              cuando no hay ningun proceso cargado.
     */
    private void limpiarBCP() {
        lblPidValor.setText("-");
        lblProgramaValor.setText("-");
        lblEstadoBcpValor.setText("-");
        lblEstadoBcpValor.setForeground(ESTADO_NEUTRO);
        lblPcValor.setText("-");
        lblIrTextoValor.setText("-");
        lblAcValor.setText("0");
        lblAxValor.setText("0");
        lblBxValor.setText("0");
        lblCxValor.setText("0");
        lblDxValor.setText("0");
        lblBaseValor.setText("-");
        lblLimiteValor.setText("-");
        lblEjecutadasValor.setText("0");
        lblCiclosValor.setText("0");
        lblHoraCreacionValor.setText("-");
    }

    /**
     * Nombre: escribirEnConsola
     * Entradas: mensaje, texto a registrar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: agrega la linea precedida de la hora y desplaza el cursor
     *              al final, para que lo ultimo escrito quede siempre a la
     *              vista.
     */
    @Override
    public void escribirEnConsola(String mensaje) {
        txtConsola.append("[" + LocalTime.now().format(HORA) + "] " + mensaje + "\n");
        txtConsola.setCaretPosition(txtConsola.getDocument().getLength());
    }

    /**
     * Nombre: limpiarConsola
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: vacia el area de texto del registro de actividad.
     */
    @Override
    public void limpiarConsola() {
        txtConsola.setText("");
    }

    /**
     * Nombre: mostrarErrores
     * Entradas: titulo, encabezado del cuadro; mensajes, errores a mostrar
     * Salidas: ninguna
     * Restricciones: bloquea la ventana hasta que el usuario cierre el cuadro
     * Descripcion: muestra todos los errores juntos, uno por linea, en un
     *              cuadro de dialogo modal.
     */
    @Override
    public void mostrarErrores(String titulo, List<String> mensajes) {
        JOptionPane.showMessageDialog(this, String.join("\n", mensajes),
                titulo, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Nombre: actualizarBotones
     * Entradas: hayPrograma, enEjecucion y termino, que describen la situacion
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: decide la habilitacion de los siete botones. Durante la
     *              ejecucion automatica se bloquea todo menos lo que no
     *              interfiere, y al terminar se vuelven a habilitar reiniciar,
     *              limpiar y estadisticas.
     */
    @Override
    public void actualizarBotones(boolean hayPrograma, boolean enEjecucion, boolean termino) {
        btnCargar.setEnabled(!enEjecucion);
        btnConfig.setEnabled(!enEjecucion);
        btnEjecutar.setEnabled(hayPrograma && !enEjecucion && !termino);
        btnPaso.setEnabled(hayPrograma && !enEjecucion && !termino);
        btnReiniciar.setEnabled(hayPrograma && !enEjecucion);
        btnLimpiar.setEnabled(hayPrograma && !enEjecucion);
        btnEstadisticas.setEnabled(hayPrograma && !enEjecucion);
    }

    /**
     * Nombre: actualizarBarraContexto
     * Entradas: nombreArchivo, archivo cargado; estado, estado del proceso
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: actualiza las dos etiquetas de la linea de contexto.
     */
    @Override
    public void actualizarBarraContexto(String nombreArchivo, String estado) {
        lblArchivoValor.setText(nombreArchivo);
        lblEstadoValor.setText(estado);
        lblEstadoValor.setForeground(colorDelEstado(estado));
    }

    /**
     * Nombre: actualizarUsoMemoria
     * Entradas: porcentaje, ocupacion de la zona de usuario de 0 a 100
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: mueve la barra de progreso y escribe el porcentaje encima.
     */
    @Override
    public void actualizarUsoMemoria(int porcentaje) {
        pbUsoMemoria.setValue(porcentaje);
        pbUsoMemoria.setString(porcentaje + " %");
    }

    /**
     * Nombre: seleccionarArchivosAsm
     * Entradas: ninguna
     * Salidas: los archivos elegidos, o una lista vacia si el usuario cancelo
     * Restricciones: el selector solo ofrece archivos con extension .asm
     * Descripcion: abre el selector de archivos ya posicionado en la carpeta
     *              de ejemplos, para que el usuario no tenga que buscarla, y
     *              permite elegir varios a la vez con Ctrl o Shift.
     */
    @Override
    public List<File> seleccionarArchivosAsm() {
        JFileChooser selector = new JFileChooser(carpetaInicial());
        selector.setDialogTitle("Seleccionar programas en ensamblador");
        selector.setMultiSelectionEnabled(true);
        selector.setAcceptAllFileFilterUsed(false);
        selector.setFileFilter(new FileNameExtensionFilter(
                "Archivos de ensamblador (*.asm)", "asm"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return Collections.emptyList();
        }
        return Arrays.asList(selector.getSelectedFiles());
    }

    /**
     * Nombre: carpetaInicial
     * Entradas: ninguna
     * Salidas: la primera carpeta de ejemplos que exista, o nulo
     * Restricciones: devolver nulo hace que el selector abra en la carpeta del
     *                usuario, que es un valor de respaldo aceptable
     * Descripcion: prueba varias rutas relativas porque la carpeta de trabajo
     *              cambia segun se ejecute desde el entorno de desarrollo o
     *              desde el jar empaquetado.
     */
    private File carpetaInicial() {
        for (String ruta : CARPETAS_EJEMPLO) {
            File carpeta = new File(ruta);
            if (carpeta.isDirectory()) {
                return carpeta;
            }
        }
        return null;
    }

    /**
     * Nombre: initComponents
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: NO editar a mano. El disenador visual de NetBeans
     *                regenera este metodo completo a partir del archivo .form
     *                cada vez que se modifica la ventana, de modo que
     *                cualquier cambio manual se pierde
     * Descripcion: crea los componentes de la ventana, les fija sus
     *              propiedades, los ubica en sus contenedores y conecta los
     *              eventos de los siete botones.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlSuperior = new javax.swing.JPanel();
        tbBarra = new javax.swing.JToolBar();
        btnCargar = new javax.swing.JButton();
        sepEjecucion = new javax.swing.JToolBar.Separator();
        btnEjecutar = new javax.swing.JButton();
        btnPaso = new javax.swing.JButton();
        btnReiniciar = new javax.swing.JButton();
        btnLimpiar = new javax.swing.JButton();
        sepConfiguracion = new javax.swing.JToolBar.Separator();
        btnConfig = new javax.swing.JButton();
        btnEstadisticas = new javax.swing.JButton();
        pnlContexto = new javax.swing.JPanel();
        lblArchivo = new javax.swing.JLabel();
        lblArchivoValor = new javax.swing.JLabel();
        lblSepA = new javax.swing.JLabel();
        lblEstado = new javax.swing.JLabel();
        lblEstadoValor = new javax.swing.JLabel();
        lblSepB = new javax.swing.JLabel();
        lblUsoMemoria = new javax.swing.JLabel();
        pbUsoMemoria = new javax.swing.JProgressBar();
        spPrincipal = new javax.swing.JSplitPane();
        pnlInstrucciones = new javax.swing.JPanel();
        scrInstrucciones = new javax.swing.JScrollPane();
        tblInstrucciones = new javax.swing.JTable();
        spSecundario = new javax.swing.JSplitPane();
        pnlMemoria = new javax.swing.JPanel();
        scrMemoria = new javax.swing.JScrollPane();
        tblMemoria = new javax.swing.JTable();
        pnlBCP = new javax.swing.JPanel();
        scrBcp = new javax.swing.JScrollPane();
        pnlBcpInterior = new javax.swing.JPanel();
        pnlBcpCampos = new javax.swing.JPanel();
        pnlProceso = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        lblPidValor = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        lblProgramaValor = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        lblEstadoBcpValor = new javax.swing.JLabel();
        pnlContextoCpu = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        lblPcValor = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        lblIrTextoValor = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lblAcValor = new javax.swing.JLabel();
        pnlRegistros = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        lblAxValor = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        lblBxValor = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        lblCxValor = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        lblDxValor = new javax.swing.JLabel();
        pnlMemoriaProceso = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        lblBaseValor = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        lblLimiteValor = new javax.swing.JLabel();
        pnlContabilidad = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        lblEjecutadasValor = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        lblCiclosValor = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        lblHoraCreacionValor = new javax.swing.JLabel();
        pnlConsola = new javax.swing.JPanel();
        scrConsola = new javax.swing.JScrollPane();
        txtConsola = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Mini PC - Simulador del ciclo de instruccion");
        getContentPane().setLayout(new java.awt.BorderLayout());

        pnlSuperior.setLayout(new javax.swing.BoxLayout(pnlSuperior, javax.swing.BoxLayout.Y_AXIS));

        tbBarra.setFloatable(false);
        tbBarra.setRollover(true);

        btnCargar.setText("Cargar .asm");
        btnCargar.setToolTipText("Selecciona uno o varios archivos de codigo ensamblador y los guarda en el disco");
        btnCargar.setFocusable(false);
        btnCargar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCargarActionPerformed(evt);
            }
        });
        tbBarra.add(btnCargar);
        tbBarra.add(sepEjecucion);

        btnEjecutar.setText("Ejecutar");
        btnEjecutar.setToolTipText("Ejecuta el programa completo de forma automatica");
        btnEjecutar.setEnabled(false);
        btnEjecutar.setFocusable(false);
        btnEjecutar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEjecutarActionPerformed(evt);
            }
        });
        tbBarra.add(btnEjecutar);

        btnPaso.setText("Paso a paso");
        btnPaso.setToolTipText("Ejecuta una sola instruccion");
        btnPaso.setEnabled(false);
        btnPaso.setFocusable(false);
        btnPaso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPasoActionPerformed(evt);
            }
        });
        tbBarra.add(btnPaso);

        btnReiniciar.setText("Reiniciar");
        btnReiniciar.setToolTipText("Vuelve el procesador al inicio del programa sin descargarlo");
        btnReiniciar.setEnabled(false);
        btnReiniciar.setFocusable(false);
        btnReiniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReiniciarActionPerformed(evt);
            }
        });
        tbBarra.add(btnReiniciar);

        btnLimpiar.setText("Limpiar");
        btnLimpiar.setToolTipText("Vacia la memoria de usuario, los registros, las tablas y la consola");
        btnLimpiar.setEnabled(false);
        btnLimpiar.setFocusable(false);
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarActionPerformed(evt);
            }
        });
        tbBarra.add(btnLimpiar);
        tbBarra.add(sepConfiguracion);

        btnConfig.setText("Configurar");
        btnConfig.setToolTipText("Tamano de memoria, limite de kernel y velocidad de ejecucion");
        btnConfig.setFocusable(false);
        btnConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfigActionPerformed(evt);
            }
        });
        tbBarra.add(btnConfig);

        btnEstadisticas.setText("Estadisticas");
        btnEstadisticas.setToolTipText("Resumen de la ejecucion");
        btnEstadisticas.setEnabled(false);
        btnEstadisticas.setFocusable(false);
        btnEstadisticas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEstadisticasActionPerformed(evt);
            }
        });
        tbBarra.add(btnEstadisticas);

        pnlSuperior.add(tbBarra);

        pnlContexto.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        lblArchivo.setText("Archivo:");
        pnlContexto.add(lblArchivo);

        lblArchivoValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblArchivoValor.setText("(ninguno)");
        pnlContexto.add(lblArchivoValor);

        lblSepA.setText("     |     ");
        pnlContexto.add(lblSepA);

        lblEstado.setText("Estado:");
        pnlContexto.add(lblEstado);

        lblEstadoValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblEstadoValor.setText("SIN PROGRAMA");
        pnlContexto.add(lblEstadoValor);

        lblSepB.setText("     |     ");
        pnlContexto.add(lblSepB);

        lblUsoMemoria.setText("Uso de memoria:");
        pnlContexto.add(lblUsoMemoria);

        pbUsoMemoria.setToolTipText("Porcentaje ocupado de la zona de usuario");
        pbUsoMemoria.setPreferredSize(new java.awt.Dimension(170, 18));
        pbUsoMemoria.setStringPainted(true);
        pnlContexto.add(pbUsoMemoria);

        pnlSuperior.add(pnlContexto);

        getContentPane().add(pnlSuperior, java.awt.BorderLayout.NORTH);

        spPrincipal.setDividerLocation(400);
        spPrincipal.setOneTouchExpandable(true);

        pnlInstrucciones.setBorder(javax.swing.BorderFactory.createTitledBorder("Instrucciones"));
        pnlInstrucciones.setLayout(new java.awt.BorderLayout());

        scrInstrucciones.setPreferredSize(new java.awt.Dimension(390, 430));

        tblInstrucciones.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblInstrucciones.setRowHeight(22);
        tblInstrucciones.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tblInstrucciones.setShowVerticalLines(false);
        scrInstrucciones.setViewportView(tblInstrucciones);

        pnlInstrucciones.add(scrInstrucciones, java.awt.BorderLayout.CENTER);

        spPrincipal.setLeftComponent(pnlInstrucciones);

        spSecundario.setDividerLocation(340);
        spSecundario.setOneTouchExpandable(true);

        pnlMemoria.setBorder(javax.swing.BorderFactory.createTitledBorder("Memoria"));
        pnlMemoria.setLayout(new java.awt.BorderLayout());

        scrMemoria.setPreferredSize(new java.awt.Dimension(330, 430));

        tblMemoria.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        tblMemoria.setRowHeight(22);
        tblMemoria.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tblMemoria.setShowVerticalLines(false);
        scrMemoria.setViewportView(tblMemoria);

        pnlMemoria.add(scrMemoria, java.awt.BorderLayout.CENTER);

        spSecundario.setLeftComponent(pnlMemoria);

        pnlBCP.setBorder(javax.swing.BorderFactory.createTitledBorder("BCP actual - CPU 1"));
        pnlBCP.setLayout(new java.awt.BorderLayout());

        scrBcp.setPreferredSize(new java.awt.Dimension(320, 430));

        pnlBcpInterior.setLayout(new java.awt.BorderLayout());

        pnlBcpCampos.setLayout(new javax.swing.BoxLayout(pnlBcpCampos, javax.swing.BoxLayout.Y_AXIS));

        pnlProceso.setBorder(javax.swing.BorderFactory.createTitledBorder("Proceso"));
        pnlProceso.setLayout(new java.awt.GridLayout(0, 2, 8, 4));

        jLabel1.setText("PID:");
        pnlProceso.add(jLabel1);

        lblPidValor.setText("-");
        pnlProceso.add(lblPidValor);

        jLabel2.setText("Programa:");
        pnlProceso.add(jLabel2);

        lblProgramaValor.setText("-");
        pnlProceso.add(lblProgramaValor);

        jLabel3.setText("Estado:");
        pnlProceso.add(jLabel3);

        lblEstadoBcpValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblEstadoBcpValor.setText("-");
        pnlProceso.add(lblEstadoBcpValor);

        pnlBcpCampos.add(pnlProceso);

        pnlContextoCpu.setBorder(javax.swing.BorderFactory.createTitledBorder("Contexto del CPU"));
        pnlContextoCpu.setLayout(new java.awt.GridLayout(0, 2, 8, 4));

        jLabel4.setText("PC:");
        pnlContextoCpu.add(jLabel4);

        lblPcValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblPcValor.setText("-");
        pnlContextoCpu.add(lblPcValor);

        jLabel6.setText("IR:");
        pnlContextoCpu.add(jLabel6);

        lblIrTextoValor.setText("-");
        pnlContextoCpu.add(lblIrTextoValor);

        jLabel7.setText("AC:");
        pnlContextoCpu.add(jLabel7);

        lblAcValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblAcValor.setText("0");
        pnlContextoCpu.add(lblAcValor);

        pnlBcpCampos.add(pnlContextoCpu);

        pnlRegistros.setBorder(javax.swing.BorderFactory.createTitledBorder("Registros de proposito general"));
        pnlRegistros.setLayout(new java.awt.GridLayout(0, 2, 8, 4));

        jLabel8.setText("AX:");
        pnlRegistros.add(jLabel8);

        lblAxValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblAxValor.setText("0");
        pnlRegistros.add(lblAxValor);

        jLabel9.setText("BX:");
        pnlRegistros.add(jLabel9);

        lblBxValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblBxValor.setText("0");
        pnlRegistros.add(lblBxValor);

        jLabel10.setText("CX:");
        pnlRegistros.add(jLabel10);

        lblCxValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblCxValor.setText("0");
        pnlRegistros.add(lblCxValor);

        jLabel11.setText("DX:");
        pnlRegistros.add(jLabel11);

        lblDxValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblDxValor.setText("0");
        pnlRegistros.add(lblDxValor);

        pnlBcpCampos.add(pnlRegistros);

        pnlMemoriaProceso.setBorder(javax.swing.BorderFactory.createTitledBorder("Memoria del proceso"));
        pnlMemoriaProceso.setLayout(new java.awt.GridLayout(0, 2, 8, 4));

        jLabel12.setText("Direccion base:");
        pnlMemoriaProceso.add(jLabel12);

        lblBaseValor.setText("-");
        pnlMemoriaProceso.add(lblBaseValor);

        jLabel13.setText("Limite:");
        pnlMemoriaProceso.add(jLabel13);

        lblLimiteValor.setText("-");
        pnlMemoriaProceso.add(lblLimiteValor);

        pnlBcpCampos.add(pnlMemoriaProceso);

        pnlContabilidad.setBorder(javax.swing.BorderFactory.createTitledBorder("Contabilidad"));
        pnlContabilidad.setLayout(new java.awt.GridLayout(0, 2, 8, 4));

        jLabel14.setText("Instrucciones ejec.:");
        pnlContabilidad.add(jLabel14);

        lblEjecutadasValor.setText("0");
        pnlContabilidad.add(lblEjecutadasValor);

        jLabel15.setText("Ciclos de reloj:");
        pnlContabilidad.add(jLabel15);

        lblCiclosValor.setText("0");
        pnlContabilidad.add(lblCiclosValor);

        jLabel16.setText("Hora de creacion:");
        pnlContabilidad.add(jLabel16);

        lblHoraCreacionValor.setText("-");
        pnlContabilidad.add(lblHoraCreacionValor);

        pnlBcpCampos.add(pnlContabilidad);

        pnlBcpInterior.add(pnlBcpCampos, java.awt.BorderLayout.NORTH);

        scrBcp.setViewportView(pnlBcpInterior);

        pnlBCP.add(scrBcp, java.awt.BorderLayout.CENTER);

        spSecundario.setRightComponent(pnlBCP);

        spPrincipal.setRightComponent(spSecundario);

        getContentPane().add(spPrincipal, java.awt.BorderLayout.CENTER);

        pnlConsola.setBorder(javax.swing.BorderFactory.createTitledBorder("Consola"));
        pnlConsola.setLayout(new java.awt.BorderLayout());

        txtConsola.setEditable(false);
        txtConsola.setColumns(20);
        txtConsola.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        txtConsola.setLineWrap(true);
        txtConsola.setRows(6);
        txtConsola.setWrapStyleWord(true);
        scrConsola.setViewportView(txtConsola);

        pnlConsola.add(scrConsola, java.awt.BorderLayout.CENTER);

        getContentPane().add(pnlConsola, java.awt.BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Nombre: btnCargarActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: delega en el controlador la carga de un archivo .asm.
     */
    private void btnCargarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCargarActionPerformed
        controlador.alCargarArchivos();
    }//GEN-LAST:event_btnCargarActionPerformed

    /**
     * Nombre: btnEjecutarActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: delega en el controlador el arranque de la ejecucion
     *              automatica.
     */
    private void btnEjecutarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEjecutarActionPerformed
        controlador.alEjecutar();
    }//GEN-LAST:event_btnEjecutarActionPerformed

    /**
     * Nombre: btnPasoActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: delega en el controlador la ejecucion de una sola
     *              instruccion.
     */
    private void btnPasoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPasoActionPerformed
        controlador.alPasoAPaso();
    }//GEN-LAST:event_btnPasoActionPerformed

    /**
     * Nombre: btnReiniciarActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: delega en el controlador el reinicio del programa.
     */
    private void btnReiniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReiniciarActionPerformed
        controlador.alReiniciar();
    }//GEN-LAST:event_btnReiniciarActionPerformed

    /**
     * Nombre: btnLimpiarActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: delega en el controlador el vaciado de la maquina.
     */
    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        controlador.alLimpiar();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    /**
     * Nombre: btnConfigActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: el dialogo es modal, de modo que bloquea esta ventana
     *                mientras este abierto
     * Descripcion: abre el dialogo de configuracion de memoria y velocidad.
     */
    private void btnConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfigActionPerformed
        new DialogoConfiguracion(this, true, controlador).setVisible(true);
    }//GEN-LAST:event_btnConfigActionPerformed

    /**
     * Nombre: btnEstadisticasActionPerformed
     * Entradas: evt, evento de accion que genero el clic
     * Salidas: ninguna
     * Restricciones: el dialogo es modal, de modo que bloquea esta ventana
     *                mientras este abierto
     * Descripcion: abre el dialogo con el resumen de la ejecucion.
     */
    private void btnEstadisticasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEstadisticasActionPerformed
        new DialogoEstadisticas(this, true, controlador).setVisible(true);
    }//GEN-LAST:event_btnEstadisticasActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCargar;
    private javax.swing.JButton btnConfig;
    private javax.swing.JButton btnEjecutar;
    private javax.swing.JButton btnEstadisticas;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnPaso;
    private javax.swing.JButton btnReiniciar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel lblAcValor;
    private javax.swing.JLabel lblArchivo;
    private javax.swing.JLabel lblArchivoValor;
    private javax.swing.JLabel lblAxValor;
    private javax.swing.JLabel lblBaseValor;
    private javax.swing.JLabel lblBxValor;
    private javax.swing.JLabel lblCiclosValor;
    private javax.swing.JLabel lblCxValor;
    private javax.swing.JLabel lblDxValor;
    private javax.swing.JLabel lblEjecutadasValor;
    private javax.swing.JLabel lblHoraCreacionValor;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblEstadoBcpValor;
    private javax.swing.JLabel lblEstadoValor;
    private javax.swing.JLabel lblIrTextoValor;
    private javax.swing.JLabel lblLimiteValor;
    private javax.swing.JLabel lblPcValor;
    private javax.swing.JLabel lblPidValor;
    private javax.swing.JLabel lblProgramaValor;
    private javax.swing.JLabel lblSepA;
    private javax.swing.JLabel lblSepB;
    private javax.swing.JLabel lblUsoMemoria;
    private javax.swing.JProgressBar pbUsoMemoria;
    private javax.swing.JPanel pnlBCP;
    private javax.swing.JPanel pnlBcpCampos;
    private javax.swing.JPanel pnlBcpInterior;
    private javax.swing.JPanel pnlConsola;
    private javax.swing.JPanel pnlContabilidad;
    private javax.swing.JPanel pnlContexto;
    private javax.swing.JPanel pnlContextoCpu;
    private javax.swing.JPanel pnlInstrucciones;
    private javax.swing.JPanel pnlMemoria;
    private javax.swing.JPanel pnlMemoriaProceso;
    private javax.swing.JPanel pnlProceso;
    private javax.swing.JPanel pnlRegistros;
    private javax.swing.JPanel pnlSuperior;
    private javax.swing.JScrollPane scrBcp;
    private javax.swing.JScrollPane scrConsola;
    private javax.swing.JScrollPane scrInstrucciones;
    private javax.swing.JScrollPane scrMemoria;
    private javax.swing.JToolBar.Separator sepConfiguracion;
    private javax.swing.JToolBar.Separator sepEjecucion;
    private javax.swing.JSplitPane spPrincipal;
    private javax.swing.JSplitPane spSecundario;
    private javax.swing.JTable tblInstrucciones;
    private javax.swing.JTable tblMemoria;
    private javax.swing.JToolBar tbBarra;
    private javax.swing.JTextArea txtConsola;
    // End of variables declaration//GEN-END:variables
}
