package com.mycompany.minipc;

import com.mycompany.minipc.gui.VentanaPrincipal;

/**
 * Nombre: MiniPC
 * Entradas: no aplica, solo expone el punto de entrada
 * Salidas: no aplica
 * Restricciones: ninguna
 * Descripcion: clase de arranque del simulador. Su unica tarea es preparar la
 *              apariencia y levantar la ventana principal.
 */
public class MiniPC {

    /**
     * Nombre: main
     * Entradas: args, argumentos de linea de comandos que no se usan
     * Salidas: ninguna
     * Restricciones: la ventana debe crearse dentro del hilo de despacho de
     *                eventos, que es donde Swing exige que se creen y
     *                manipulen los componentes
     * Descripcion: fija la apariencia del sistema y encola la construccion de
     *              la ventana con invokeLater, en lugar de crearla en el hilo
     *              main, que es la causa habitual de fallos intermitentes de
     *              dibujado en aplicaciones Swing.
     */
    public static void main(String[] args) {
        aplicarApariencia();
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new VentanaPrincipal().setVisible(true);
            }
        });
    }

    /**
     * Nombre: aplicarApariencia
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: si FlatLaf no esta en el classpath (por ejemplo, al
     *                ejecutar el jar sin sus dependencias) se usa la apariencia
     *                del sistema, y si esa tambien falla, la de Java; nunca se
     *                interrumpe el arranque
     * Descripcion: usa FlatLaf en su tema claro, que da un aspecto plano y
     *              moderno, con esquinas redondeadas suaves. Los colores de la
     *              ventana los pone gui.Tema encima de esta apariencia.
     */
    public static void aplicarApariencia() {
        try {
            javax.swing.UIManager.put("Component.arc", 6);
            javax.swing.UIManager.put("Button.arc", 6);
            javax.swing.UIManager.put("ScrollBar.width", 11);
            javax.swing.UIManager.put("TabbedPane.showTabSeparators", true);
            com.formdev.flatlaf.FlatLightLaf.setup();
            return;
        } catch (LinkageError e) {
            // FlatLaf no esta disponible: se sigue con la del sistema.
        }
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException e) {
            // La apariencia es un detalle estetico: si falla, se sigue igual.
        }
    }
}
