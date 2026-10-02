package com.mycompany.minipc.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import com.mycompany.minipc.config.LectorConfiguracion;
import com.mycompany.minipc.gui.panel.PanelBCP;
import com.mycompany.minipc.gui.panel.PanelColas;
import com.mycompany.minipc.gui.panel.PanelListaTrabajos;
import com.mycompany.minipc.gui.panel.PanelPantalla;
import com.mycompany.minipc.gui.panel.Tablas;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;

/**
 * Nombre: VentanaPrincipal
 * Entradas: no aplica, es la ventana de la aplicacion
 * Salidas: no aplica
 * Restricciones: solo dibuja; toda la logica la coordina el controlador, que
 *                le habla a traves de VistaPrincipal
 * Descripcion: la ventana del Gestor de Procesos, segun la maqueta de la
 *              pagina 8 del enunciado y la seccion 8 del plan:
 *
 *                barra de titulo  nombre, reloj simulado, algoritmo y CPU
 *                menu y barra     Cargar archivos | Ejecutar, Siguiente,
 *                                 Pausar | Reiniciar, Limpiar | Estadisticas
 *                izquierda        lista de trabajos y colas de procesos
 *                centro           BCP actual de la CPU 1
 *                derecha          memoria principal y disco, lado a lado
 *                abajo            pantalla con teclado, consola del SO y el
 *                                 programa en ejecucion
 *                barra de estado  uso de memoria y disco, procesos admitidos
 *
 *              Cada seccion es su propia clase en gui/panel. El archivo .form
 *              solo tiene el marco de la ventana; el resto se arma aqui, asi
 *              el codigo generado por NetBeans no se mezcla con el del tema.
 */
public class VentanaPrincipal extends javax.swing.JFrame implements VistaPrincipal {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String[] CARPETAS_EJEMPLO = {"../../Ejemplo", "../Ejemplo", "Ejemplo"};

    private final ControladorPrincipal controlador;

    // Barra de titulo y barra de herramientas
    private final JLabel lblReloj = etiquetaTitulo("00:00:00");
    private final JLabel lblAlgoritmo = etiquetaTitulo("FCFS");
    private final JLabel lblEnCpu = new JLabel();
    private final JButton btnCargar = new JButton("Cargar archivos");
    private final JButton btnEjecutar = new JButton("Ejecutar");
    private final JButton btnSiguiente = new JButton("Siguiente");
    private final JButton btnPausar = new JButton("Pausar");
    private final JButton btnReiniciar = new JButton("Reiniciar");
    private final JButton btnLimpiar = new JButton("Limpiar");
    private final JButton btnEstadisticas = new JButton("Estadisticas");
    private final JButton btnConfigurar = new JButton("Configuracion");

    // Secciones
    private final PanelListaTrabajos panelTrabajos;
    private final PanelColas panelColas = new PanelColas();
    private final PanelBCP panelBCP = new PanelBCP();
    private final PanelPantalla panelPantalla;
    private final JTable tblMemoria;
    private final JTable tblDisco;
    private final JTable tblPrograma;
    private final JTextArea txtConsola = new JTextArea();

    // Barra de estado
    private final JLabel lblMemoria = etiquetaEstado();
    private final JLabel lblDisco = etiquetaEstado();
    private final JLabel lblAdmitidos = etiquetaEstado();
    private final JLabel lblModo = etiquetaEstado();

    /**
     * Nombre: VentanaPrincipal
     * Entradas: ninguna
     * Salidas: la ventana construida y lista para mostrarse
     * Restricciones: debe construirse en el hilo de eventos de Swing
     * Descripcion: crea el controlador, arma todas las secciones con los
     *              modelos y renderers que el controlador provee y deja la
     *              vista en su estado inicial.
     */
    public VentanaPrincipal() {
        initComponents();
        this.controlador = new ControladorPrincipal(this, new LectorConfiguracion());

        panelTrabajos = new PanelListaTrabajos(controlador.getModeloTrabajos());
        panelPantalla = new PanelPantalla(controlador::alEnviarTeclado);

        tblMemoria = new JTable(controlador.getModeloMemoria());
        Tablas.estilo(tblMemoria, new int[]{44, 120, 170});
        tblMemoria.setDefaultRenderer(Object.class, controlador.getRenderMemoria());

        tblDisco = new JTable(controlador.getModeloDisco());
        Tablas.estilo(tblDisco, new int[]{44, 70, 170});
        tblDisco.setDefaultRenderer(Object.class, controlador.getRenderDisco());

        tblPrograma = new JTable(controlador.getModeloInstrucciones());
        Tablas.estilo(tblPrograma, new int[]{40, 300});
        tblPrograma.setDefaultRenderer(Object.class, controlador.getRenderInstrucciones());

        txtConsola.setEditable(false);
        txtConsola.setFont(Tema.FUENTE_MONO);
        txtConsola.setLineWrap(true);
        txtConsola.setWrapStyleWord(true);

        armarVentana();
        controlador.inicializarVista();
    }

    /**
     * Nombre: armarVentana
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: se llama una vez, desde el constructor
     * Descripcion: ubica las secciones segun la distribucion de la clase.
     */
    private void armarVentana() {
        getContentPane().setBackground(Tema.FONDO);
        setJMenuBar(crearMenu());

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(crearBarraTitulo(), BorderLayout.NORTH);
        norte.add(crearBarraHerramientas(), BorderLayout.SOUTH);
        getContentPane().add(norte, BorderLayout.NORTH);

        JSplitPane izquierda = dividir(JSplitPane.VERTICAL_SPLIT,
                Tema.seccion("Lista de trabajos", panelTrabajos),
                Tema.seccion("Colas de procesos", panelColas), 0.62);

        JPanel almacenamiento = new JPanel(new GridLayout(1, 2, 6, 0));
        almacenamiento.setBackground(Tema.FONDO);
        almacenamiento.add(Tema.seccion("Memoria principal", conLeyenda(tblMemoria,
                Tablas.leyenda(Tema.CABECERA_SO, "SO", Tema.colorProceso(0), "BCP",
                        Tema.colorProcesoClaro(0), "Programa", Tema.RESALTADO, "PC"))));
        almacenamiento.add(Tema.seccion("Disco", conLeyenda(tblDisco,
                Tablas.leyenda(new Color(255, 228, 196), "Indice", new Color(214, 234, 248),
                        "Archivos", new Color(232, 232, 232), "Virtual"))));

        JSplitPane derecha = dividir(JSplitPane.HORIZONTAL_SPLIT,
                Tema.seccion("BCP actual - CPU 1", panelBCP), almacenamiento, 0.36);
        JSplitPane centro = dividir(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha, 0.27);

        JTabbedPane consolas = new JTabbedPane();
        consolas.setFont(Tema.FUENTE);
        consolas.addTab("Consola del SO", new JScrollPane(txtConsola));
        consolas.addTab("Programa en ejecucion", new JScrollPane(tblPrograma));
        JPanel inferior = new JPanel(new GridLayout(1, 2, 6, 0));
        inferior.setBackground(Tema.FONDO);
        inferior.add(Tema.seccion("Pantalla", panelPantalla));
        inferior.add(Tema.seccion("Sistema operativo", consolas));
        inferior.setPreferredSize(new Dimension(100, 210));

        JSplitPane general = dividir(JSplitPane.VERTICAL_SPLIT, centro, inferior, 0.70);
        general.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        getContentPane().add(general, BorderLayout.CENTER);
        getContentPane().add(crearBarraEstado(), BorderLayout.SOUTH);

        setSize(1366, 768);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);
    }

    /**
     * Nombre: crearBarraTitulo
     * Entradas: ninguna
     * Salidas: la franja menta con el nombre del programa y el reloj
     * Restricciones: ninguna
     * Descripcion: a la derecha muestra el reloj simulado, el algoritmo y la
     *              CPU, que son datos que conviene ver siempre.
     */
    private JComponent crearBarraTitulo() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.MENTA);
        barra.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        JLabel titulo = new JLabel("Proyecto 1 de SO - Gestor de Procesos");
        titulo.setFont(Tema.FUENTE_TITULO);
        titulo.setForeground(Tema.TEXTO_SOBRE_MENTA);
        barra.add(titulo, BorderLayout.WEST);

        JPanel datos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        datos.setOpaque(false);
        datos.add(etiquetaTitulo("Reloj"));
        datos.add(lblReloj);
        datos.add(etiquetaTitulo("|"));
        datos.add(etiquetaTitulo("Planificacion"));
        datos.add(lblAlgoritmo);
        datos.add(etiquetaTitulo("|"));
        datos.add(etiquetaTitulo("CPU 1"));
        lblReloj.setFont(Tema.FUENTE_TITULO);
        lblReloj.setForeground(Tema.TEXTO_SOBRE_MENTA);
        lblAlgoritmo.setFont(Tema.FUENTE_NEGRITA);
        lblAlgoritmo.setForeground(Tema.TEXTO_SOBRE_MENTA);
        barra.add(datos, BorderLayout.EAST);
        return barra;
    }

    /**
     * Nombre: crearBarraHerramientas
     * Entradas: ninguna
     * Salidas: los botones agrupados por funcion y el proceso en la CPU
     * Restricciones: ninguna
     * Descripcion: cargar | ejecutar, siguiente, pausar | reiniciar, limpiar
     *              | estadisticas, configuracion; cada grupo con su color.
     */
    private JComponent crearBarraHerramientas() {
        Tema.pintarBoton(btnCargar, Tema.BOTON_CARGAR);
        Tema.pintarBoton(btnEjecutar, Tema.BOTON_EJECUTAR);
        Tema.pintarBoton(btnSiguiente, Tema.BOTON_EJECUTAR);
        Tema.pintarBoton(btnPausar, Tema.BOTON_PAUSA);
        Tema.pintarBoton(btnReiniciar, Tema.BOTON_PAUSA);
        Tema.pintarBoton(btnLimpiar, Tema.BOTON_LIMPIAR);
        Tema.pintarBoton(btnEstadisticas, Tema.BOTON_UTILIDAD);
        Tema.pintarBoton(btnConfigurar, Tema.BOTON_UTILIDAD);

        btnCargar.addActionListener(e -> controlador.alCargarArchivos());
        btnEjecutar.addActionListener(e -> controlador.alEjecutar());
        btnSiguiente.addActionListener(e -> controlador.alPasoAPaso());
        btnPausar.addActionListener(e -> controlador.alPausar());
        btnReiniciar.addActionListener(e -> controlador.alReiniciar());
        btnLimpiar.addActionListener(e -> controlador.alLimpiar());
        btnEstadisticas.addActionListener(e -> abrirEstadisticas());
        btnConfigurar.addActionListener(e -> abrirConfiguracion());

        btnSiguiente.setToolTipText("Un segundo de CPU (F8)");
        btnEjecutar.setToolTipText("Ejecuta todos los procesos hasta terminar (F5)");
        btnCargar.setToolTipText("Carga uno o varios archivos .asm (Ctrl+O)");

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botones.setOpaque(false);
        botones.add(btnCargar);
        botones.add(separador());
        botones.add(btnEjecutar);
        botones.add(btnSiguiente);
        botones.add(btnPausar);
        botones.add(separador());
        botones.add(btnReiniciar);
        botones.add(btnLimpiar);
        botones.add(separador());
        botones.add(btnEstadisticas);
        botones.add(btnConfigurar);

        lblEnCpu.setFont(Tema.FUENTE_NEGRITA);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.TARJETA);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                BorderFactory.createEmptyBorder(6, 8, 6, 14)));
        barra.add(botones, BorderLayout.WEST);
        barra.add(lblEnCpu, BorderLayout.EAST);
        return barra;
    }

    /**
     * Nombre: crearMenu
     * Entradas: ninguna
     * Salidas: el menu Archivo, Ejecucion, Configuracion y Ayuda
     * Restricciones: ninguna
     * Descripcion: el enunciado pide un menu o medio de configuracion; el
     *              menu repite las acciones de los botones con atajos.
     */
    private JMenuBar crearMenu() {
        JMenuBar barra = new JMenuBar();
        JMenu archivo = new JMenu("Archivo");
        archivo.add(item("Cargar archivos...", KeyStroke.getKeyStroke(KeyEvent.VK_O,
                InputEvent.CTRL_DOWN_MASK), e -> controlador.alCargarArchivos()));
        archivo.addSeparator();
        archivo.add(item("Salir", null, e -> dispose()));

        JMenu ejecucion = new JMenu("Ejecucion");
        ejecucion.add(item("Ejecutar", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                e -> controlador.alEjecutar()));
        ejecucion.add(item("Siguiente", KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0),
                e -> controlador.alPasoAPaso()));
        ejecucion.add(item("Pausar", null, e -> controlador.alPausar()));
        ejecucion.addSeparator();
        ejecucion.add(item("Reiniciar", null, e -> controlador.alReiniciar()));
        ejecucion.add(item("Limpiar", null, e -> controlador.alLimpiar()));
        ejecucion.addSeparator();
        ejecucion.add(item("Estadisticas", null, e -> abrirEstadisticas()));

        JMenu configuracion = new JMenu("Configuracion");
        configuracion.add(item("Memoria, disco y planificacion...", null,
                e -> abrirConfiguracion()));

        JMenu ayuda = new JMenu("Ayuda");
        ayuda.add(item("Acerca de", null, e -> JOptionPane.showMessageDialog(this,
                "Proyecto 1 - Gestor de Procesos\nIC-6600 Principios de Sistemas Operativos\n"
                + "Kernel: " + TablaBCP.describirFormula() + "\nPlanificacion: FCFS",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE)));

        barra.add(archivo);
        barra.add(ejecucion);
        barra.add(configuracion);
        barra.add(ayuda);
        return barra;
    }

    /**
     * Nombre: crearBarraEstado
     * Entradas: ninguna
     * Salidas: la franja inferior con los datos de uso
     * Restricciones: ninguna
     * Descripcion: memoria de usuario, disco, procesos admitidos y modo.
     */
    private JComponent crearBarraEstado() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 3));
        barra.setBackground(Tema.TARJETA);
        barra.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE));
        barra.add(lblMemoria);
        barra.add(lblDisco);
        barra.add(lblAdmitidos);
        barra.add(lblModo);
        return barra;
    }

    /**
     * Nombre: abrirEstadisticas
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: muestra el dialogo de estadisticas.
     */
    private void abrirEstadisticas() {
        new DialogoEstadisticas(this, true, controlador).setVisible(true);
    }

    /**
     * Nombre: abrirConfiguracion
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: muestra el dialogo de configuracion.
     */
    private void abrirConfiguracion() {
        new DialogoConfiguracion(this, true, controlador).setVisible(true);
    }

    // ------------------------------------------------------------------
    // VistaPrincipal
    // ------------------------------------------------------------------

    /**
     * Nombre: mostrarInstrucciones
     * Entradas: programa, texto de cada instruccion en orden
     * Salidas: ninguna
     * Restricciones: no usa el parametro, porque el modelo de la tabla lo
     *                mantiene el controlador
     * Descripcion: vuelve la tabla del programa al principio y la redibuja.
     */
    @Override
    public void mostrarInstrucciones(List<String> programa) {
        tblPrograma.clearSelection();
        tblPrograma.scrollRectToVisible(tblPrograma.getCellRect(0, 0, true));
        tblPrograma.repaint();
    }

    /**
     * Nombre: resaltarInstruccion
     * Entradas: indiceFila, fila a resaltar, o -1 para no resaltar ninguna
     * Salidas: ninguna
     * Restricciones: un indice fuera de rango se ignora sin fallar
     * Descripcion: redibuja la tabla del programa y la desplaza a la fila.
     */
    @Override
    public void resaltarInstruccion(int indiceFila) {
        tblPrograma.repaint();
        if (indiceFila >= 0 && indiceFila < tblPrograma.getRowCount()) {
            tblPrograma.scrollRectToVisible(tblPrograma.getCellRect(indiceFila, 0, true));
        }
    }

    /**
     * Nombre: refrescarMemoria
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: redibuja la tabla de memoria y la desplaza hasta la
     *              instruccion del proceso en ejecucion.
     */
    @Override
    public void refrescarMemoria() {
        ((AbstractTableModel) tblMemoria.getModel()).fireTableDataChanged();
        Proceso actual = controlador.getSistemaOperativo().getEnEjecucion();
        int direccion = actual == null ? -1 : actual.getPc();
        if (direccion >= 0 && direccion < tblMemoria.getRowCount()) {
            tblMemoria.scrollRectToVisible(tblMemoria.getCellRect(direccion, 0, true));
        }
    }

    /**
     * Nombre: refrescarDisco
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: redibuja la tabla del disco.
     */
    @Override
    public void refrescarDisco() {
        ((AbstractTableModel) tblDisco.getModel()).fireTableDataChanged();
    }

    /**
     * Nombre: mostrarBCP
     * Entradas: proceso, proceso en ejecucion, o nulo si la CPU esta libre
     * Salidas: ninguna
     * Restricciones: tolera el valor nulo
     * Descripcion: el panel del BCP lee cada campo de su celda en memoria.
     */
    @Override
    public void mostrarBCP(Proceso proceso) {
        panelBCP.mostrar(proceso, controlador.getProcesador(),
                controlador.getSistemaOperativo().getMemoria());
    }

    /**
     * Nombre: mostrarColas
     * Entradas: procesos, la lista de procesos en el orden de los enlaces
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: actualiza el panel de colas y la lista de trabajos.
     */
    @Override
    public void mostrarColas(List<Proceso> procesos) {
        panelColas.mostrar(procesos);
        panelTrabajos.refrescar();
    }

    /**
     * Nombre: escribirEnConsola
     * Entradas: mensaje, texto a agregar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: agrega la linea con la hora real y baja hasta el final.
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
     * Descripcion: vacia la consola.
     */
    @Override
    public void limpiarConsola() {
        txtConsola.setText("");
    }

    /**
     * Nombre: mostrarErrores
     * Entradas: titulo, encabezado del cuadro; mensajes, errores a mostrar
     * Salidas: ninguna
     * Restricciones: el cuadro es modal
     * Descripcion: muestra todos los errores juntos, uno por linea.
     */
    @Override
    public void mostrarErrores(String titulo, List<String> mensajes) {
        JOptionPane.showMessageDialog(this, String.join("\n", mensajes), titulo,
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Nombre: actualizarBotones
     * Entradas: hayPrograma, si hay trabajos cargados; enEjecucion, si corre
     *           la ejecucion automatica; termino, si ya no quedan pendientes
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: deshabilita lo que no aplica.
     */
    @Override
    public void actualizarBotones(boolean hayPrograma, boolean enEjecucion, boolean termino) {
        btnCargar.setEnabled(!enEjecucion);
        btnConfigurar.setEnabled(!enEjecucion);
        btnEjecutar.setEnabled(hayPrograma && !enEjecucion && !termino);
        btnSiguiente.setEnabled(hayPrograma && !enEjecucion && !termino);
        btnPausar.setEnabled(enEjecucion);
        btnReiniciar.setEnabled(hayPrograma && !enEjecucion);
        btnLimpiar.setEnabled(hayPrograma && !enEjecucion);
        btnEstadisticas.setEnabled(hayPrograma && !enEjecucion);
        lblModo.setText("Modo: " + (enEjecucion ? "automatico" : "paso a paso"));
    }

    /**
     * Nombre: actualizarBarraContexto
     * Entradas: nombreArchivo, programa en la CPU; estado, estado del sistema
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: muestra a la derecha de la barra de herramientas que
     *              programa tiene la CPU.
     */
    @Override
    public void actualizarBarraContexto(String nombreArchivo, String estado) {
        lblEnCpu.setText("En CPU: " + nombreArchivo + "   |   " + estado);
        lblEnCpu.setForeground("EJECUCION".equals(estado) ? Tema.BOTON_EJECUTAR
                : Tema.TEXTO_SUAVE);
    }

    /**
     * Nombre: actualizarUsoMemoria
     * Entradas: porcentaje, ocupacion de la zona de usuario
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: actualiza la barra de estado.
     */
    @Override
    public void actualizarUsoMemoria(int porcentaje) {
        lblMemoria.setText("Memoria de usuario: " + porcentaje + " %");
    }

    /**
     * Nombre: mostrarResumen
     * Entradas: usoDisco, porcentaje ocupado del area de archivos;
     *           admitidos, procesos con BCP
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: actualiza la barra de estado y el algoritmo del titulo.
     */
    @Override
    public void mostrarResumen(int usoDisco, int admitidos) {
        lblDisco.setText("Disco: " + usoDisco + " %");
        lblAdmitidos.setText("Procesos admitidos: " + admitidos + " de "
                + TablaBCP.MAX_PROCESOS);
        lblAlgoritmo.setText(controlador.getSistemaOperativo().getAlgoritmo().getNombre());
    }

    /**
     * Nombre: mostrarEstadisticas
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: se difiere con invokeLater para que la ventana termine
     *                de refrescarse antes de abrir el dialogo modal
     * Descripcion: ver VistaPrincipal.
     */
    @Override
    public void mostrarEstadisticas() {
        SwingUtilities.invokeLater(this::abrirEstadisticas);
    }

    /**
     * Nombre: mostrarPantalla
     * Entradas: lineas, contenido de la pantalla del Mini PC
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: delega en el panel de la pantalla.
     */
    @Override
    public void mostrarPantalla(List<String> lineas) {
        panelPantalla.mostrar(lineas);
    }

    /**
     * Nombre: habilitarTeclado
     * Entradas: habilitado, true si algun proceso espera un valor
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: delega en el panel de la pantalla.
     */
    @Override
    public void habilitarTeclado(boolean habilitado) {
        panelPantalla.habilitarTeclado(habilitado);
    }

    /**
     * Nombre: mostrarReloj
     * Entradas: reloj, tiempo simulado como hora:minuto:segundo
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: actualiza el reloj de la barra de titulo.
     */
    @Override
    public void mostrarReloj(String reloj) {
        lblReloj.setText(reloj);
    }

    /**
     * Nombre: seleccionarArchivosAsm
     * Entradas: ninguna
     * Salidas: los archivos elegidos, o una lista vacia si el usuario cancelo
     * Restricciones: el selector solo ofrece archivos con extension .asm
     * Descripcion: abre el selector en la carpeta de ejemplos y permite elegir
     *              varios a la vez con Ctrl o Shift.
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

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    /**
     * Nombre: carpetaInicial
     * Entradas: ninguna
     * Salidas: la primera carpeta de ejemplos que exista, o nulo
     * Restricciones: nulo hace que el selector abra en la carpeta del usuario
     * Descripcion: prueba varias rutas porque la carpeta de trabajo cambia
     *              segun se ejecute desde NetBeans o desde el jar.
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
     * Nombre: conLeyenda
     * Entradas: tabla; leyenda, fila de colores
     * Salidas: la tabla con su leyenda abajo
     * Restricciones: ninguna
     * Descripcion: para la memoria y el disco.
     */
    private static JComponent conLeyenda(JTable tabla, JComponent leyenda) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Tema.TARJETA);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        panel.add(leyenda, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Nombre: dividir
     * Entradas: orientacion; primero y segundo, los dos lados; proporcion,
     *           espacio inicial del primero, de 0 a 1
     * Salidas: el divisor, que el usuario puede mover
     * Restricciones: ninguna
     * Descripcion: los divisores dejan acomodar la ventana a la pantalla.
     */
    private static JSplitPane dividir(int orientacion, JComponent primero, JComponent segundo,
            double proporcion) {
        JSplitPane divisor = new JSplitPane(orientacion, primero, segundo);
        divisor.setResizeWeight(proporcion);
        divisor.setDividerSize(6);
        divisor.setBorder(null);
        divisor.setBackground(Tema.FONDO);
        return divisor;
    }

    /**
     * Nombre: item
     * Entradas: texto; atajo, combinacion de teclas o nulo; accion
     * Salidas: el elemento de menu
     * Restricciones: ninguna
     * Descripcion: atajo para armar el menu.
     */
    private static JMenuItem item(String texto, KeyStroke atajo, Consumer<ActionEvent> accion) {
        JMenuItem item = new JMenuItem(texto);
        item.setAccelerator(atajo);
        item.addActionListener(accion::accept);
        return item;
    }

    /**
     * Nombre: separador
     * Entradas: ninguna
     * Salidas: una linea vertical entre grupos de botones
     * Restricciones: ninguna
     * Descripcion: separa los grupos de la barra de herramientas.
     */
    private static JComponent separador() {
        JPanel linea = new JPanel();
        linea.setBackground(Tema.BORDE);
        linea.setPreferredSize(new Dimension(1, 24));
        return linea;
    }

    /**
     * Nombre: etiquetaTitulo
     * Entradas: texto
     * Salidas: una etiqueta para la barra de titulo
     * Restricciones: ninguna
     * Descripcion: todas las etiquetas de la barra con el mismo estilo.
     */
    private static JLabel etiquetaTitulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(Tema.FUENTE);
        etiqueta.setForeground(Tema.TEXTO_SUAVE_SOBRE_MENTA);
        return etiqueta;
    }

    /**
     * Nombre: etiquetaEstado
     * Entradas: ninguna
     * Salidas: una etiqueta para la barra de estado
     * Restricciones: ninguna
     * Descripcion: todas las etiquetas de la barra con el mismo estilo.
     */
    private static JLabel etiquetaEstado() {
        JLabel etiqueta = new JLabel();
        etiqueta.setFont(Tema.FUENTE);
        etiqueta.setForeground(Tema.TEXTO_SUAVE);
        return etiqueta;
    }

    /**
     * Nombre: initComponents
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: NO editar a mano. El disenador visual de NetBeans
     *                regenera este metodo a partir del archivo .form
     * Descripcion: crea el marco de la ventana: titulo, cierre y distribucion.
     *              Las secciones se agregan en armarVentana.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Proyecto 1 de SO - Gestor de Procesos");

        pack();
    }// </editor-fold>//GEN-END:initComponents
}
