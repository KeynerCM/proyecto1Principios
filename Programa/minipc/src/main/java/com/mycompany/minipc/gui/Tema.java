package com.mycompany.minipc.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.mycompany.minipc.so.procesos.EstadoProceso;

/**
 * Nombre: Tema
 * Entradas: no aplica, solo expone constantes y metodos de ayuda
 * Salidas: no aplica
 * Restricciones: no se instancia
 * Descripcion: la paleta y las letras de toda la interfaz, en un solo lugar.
 *              Un color por estado del proceso y un color por ranura de BCP
 *              (P en la ranura 0 a 4), que se repiten en la lista de trabajos,
 *              las colas, el BCP y la tabla de memoria, para que el mismo
 *              proceso se reconozca en todas partes.
 */
public final class Tema {

    /** Menta de la barra de titulo y de los encabezados de las tablas. */
    public static final Color MENTA = new Color(62, 180, 137);

    /** Texto sobre el menta: verde muy oscuro, mas legible que el blanco. */
    public static final Color TEXTO_SOBRE_MENTA = new Color(14, 52, 40);

    /** Texto secundario sobre el menta (rotulos de la barra de titulo). */
    public static final Color TEXTO_SUAVE_SOBRE_MENTA = new Color(30, 84, 66);

    /** Menta oscuro para los titulos de seccion sobre fondo blanco. */
    public static final Color TITULO_SECCION = new Color(26, 122, 92);

    /** Fondo general de la ventana. */
    public static final Color FONDO = new Color(242, 244, 247);

    /** Fondo de cada panel. */
    public static final Color TARJETA = Color.WHITE;

    /** Lineas de borde de los paneles. */
    public static final Color BORDE = new Color(214, 219, 226);

    /** Texto principal. */
    public static final Color TEXTO = new Color(33, 37, 41);

    /** Texto secundario: rotulos y leyendas. */
    public static final Color TEXTO_SUAVE = new Color(102, 112, 122);

    /** Botones segun su funcion. */
    public static final Color BOTON_CARGAR = new Color(41, 98, 155);
    public static final Color BOTON_EJECUTAR = new Color(34, 124, 78);
    public static final Color BOTON_PAUSA = new Color(176, 122, 24);
    public static final Color BOTON_LIMPIAR = new Color(163, 58, 48);
    public static final Color BOTON_UTILIDAD = new Color(84, 95, 110);
    public static final Color BOTON_DESHABILITADO = new Color(206, 208, 211);
    public static final Color TEXTO_DESHABILITADO = new Color(128, 131, 136);

    /** Fila de la instruccion en curso en las tablas. */
    public static final Color RESALTADO = new Color(255, 235, 156);

    /** Fila de la lista de trabajos que acaba de cambiar de estado. */
    public static final Color DESTELLO = new Color(255, 245, 200);

    /** Pantalla del Mini PC. */
    public static final Color PANTALLA_FONDO = new Color(24, 28, 32);
    public static final Color PANTALLA_TEXTO = new Color(120, 230, 140);

    /** Cabecera del sistema operativo en la tabla de memoria. */
    public static final Color CABECERA_SO = new Color(96, 104, 116);

    /** Zona de kernel libre (ranura de BCP sin usar). */
    public static final Color KERNEL_LIBRE = new Color(232, 234, 237);

    /** Un color por ranura de BCP. */
    private static final Color[] PROCESO = {
        new Color(173, 206, 240), new Color(178, 223, 178), new Color(247, 200, 160),
        new Color(214, 188, 232), new Color(240, 180, 190)
    };

    /** Version clara de cada color, para el programa en la zona de usuario. */
    private static final Color[] PROCESO_CLARO = {
        new Color(222, 236, 250), new Color(224, 242, 224), new Color(252, 232, 214),
        new Color(238, 228, 245), new Color(250, 222, 228)
    };

    /** Letra de la interfaz. */
    public static final Font FUENTE = new Font(familia("Segoe UI", Font.SANS_SERIF), Font.PLAIN, 12);

    /** Letra de los valores destacados. */
    public static final Font FUENTE_NEGRITA = FUENTE.deriveFont(Font.BOLD);

    /** Letra de los titulos de seccion. */
    public static final Font FUENTE_SECCION = FUENTE.deriveFont(Font.BOLD, 11f);

    /** Letra del titulo de la ventana. */
    public static final Font FUENTE_TITULO = FUENTE.deriveFont(Font.BOLD, 16f);

    /** Letra de ancho fijo para tablas, pantalla y consola. */
    public static final Font FUENTE_MONO = new Font(familia("Consolas", Font.MONOSPACED), Font.PLAIN, 12);

    /**
     * Nombre: Tema
     * Entradas: ninguna
     * Salidas: no aplica
     * Restricciones: no se instancia
     * Descripcion: clase de utilidad con miembros estaticos.
     */
    private Tema() {
    }

    /**
     * Nombre: familia
     * Entradas: preferida, letra deseada; respaldo, familia logica de Java
     * Salidas: la preferida si esta instalada, si no el respaldo
     * Restricciones: ninguna
     * Descripcion: Segoe UI y Consolas vienen con Windows; en otro sistema se
     *              usan las letras genericas de Java.
     */
    private static String familia(String preferida, String respaldo) {
        String[] instaladas = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        return Arrays.asList(instaladas).contains(preferida) ? preferida : respaldo;
    }

    /**
     * Nombre: colorEstado
     * Entradas: estado, estado del proceso
     * Salidas: el color con que se muestra ese estado
     * Restricciones: ninguna
     * Descripcion: NUEVO gris, PREPARADO azul, EJECUCION verde, EN_ESPERA
     *              ambar, suspendidos lila y morado, FINALIZADO gris oscuro.
     */
    public static Color colorEstado(EstadoProceso estado) {
        switch (estado) {
            case NUEVO:
                return new Color(134, 142, 150);
            case PREPARADO:
                return new Color(41, 98, 155);
            case EJECUCION:
                return new Color(34, 139, 84);
            case EN_ESPERA:
                return new Color(196, 132, 18);
            case SUSPENDIDO_PREPARADO:
                return new Color(132, 94, 194);
            case SUSPENDIDO_EN_ESPERA:
                return new Color(102, 51, 153);
            default:
                return new Color(73, 80, 87);
        }
    }

    /**
     * Nombre: colorError
     * Entradas: ninguna
     * Salidas: el rojo de los procesos que terminaron por un error
     * Restricciones: ninguna
     * Descripcion: lo usan la lista de trabajos y el panel del BCP.
     */
    public static Color colorError() {
        return BOTON_LIMPIAR;
    }

    /**
     * Nombre: colorProceso
     * Entradas: ranura, ranura de BCP del proceso (0 a 4)
     * Salidas: el color del proceso
     * Restricciones: ranuras fuera de rango usan el gris del kernel libre
     * Descripcion: el mismo color en la memoria, las colas y el BCP.
     */
    public static Color colorProceso(int ranura) {
        return ranura >= 0 && ranura < PROCESO.length ? PROCESO[ranura] : KERNEL_LIBRE;
    }

    /**
     * Nombre: colorProcesoClaro
     * Entradas: ranura, ranura de BCP del proceso (0 a 4)
     * Salidas: la version clara del color del proceso
     * Restricciones: ranuras fuera de rango usan blanco
     * Descripcion: para el programa del proceso en la zona de usuario.
     */
    public static Color colorProcesoClaro(int ranura) {
        return ranura >= 0 && ranura < PROCESO_CLARO.length ? PROCESO_CLARO[ranura] : TARJETA;
    }

    /**
     * Nombre: seccion
     * Entradas: titulo, nombre de la seccion; contenido, lo que va adentro
     * Salidas: un panel blanco con el titulo en mayusculas arriba
     * Restricciones: ninguna
     * Descripcion: todas las secciones de la ventana se ven igual: fondo
     *              blanco, borde fino y titulo pequeno en mayusculas, sin
     *              bordes 3D.
     */
    public static JPanel seccion(String titulo, JComponent contenido) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(TARJETA);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JLabel rotulo = new JLabel(titulo.toUpperCase());
        rotulo.setFont(FUENTE_SECCION);
        rotulo.setForeground(TITULO_SECCION);
        panel.add(rotulo, BorderLayout.NORTH);
        panel.add(contenido, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Nombre: pintarBoton
     * Entradas: boton, boton a pintar; color, color de su funcion
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: boton plano con texto blanco; se ve gris cuando esta
     *              deshabilitado, para que se note que no aplica. Con FlatLaf
     *              basta con el color de fondo; con la apariencia de Windows
     *              hay que pintar el boton como un panel opaco, porque esa
     *              apariencia ignora el color de fondo de los botones.
     */
    public static void pintarBoton(JButton boton, Color color) {
        boolean flat = UIManager.getLookAndFeel().getClass().getName().contains("flatlaf");
        boton.setContentAreaFilled(flat);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setFont(FUENTE_NEGRITA);
        boton.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        aplicarColorBoton(boton, color);
        boton.addPropertyChangeListener("enabled", e -> aplicarColorBoton(boton, color));
    }

    /**
     * Nombre: aplicarColorBoton
     * Entradas: boton; color, color de su funcion
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: color de la funcion si esta habilitado, gris si no.
     */
    private static void aplicarColorBoton(JButton boton, Color color) {
        boton.setBackground(boton.isEnabled() ? color : BOTON_DESHABILITADO);
        boton.setForeground(boton.isEnabled() ? Color.WHITE : TEXTO_DESHABILITADO);
    }
}
