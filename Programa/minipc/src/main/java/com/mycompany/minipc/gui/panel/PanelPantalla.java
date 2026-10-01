package com.mycompany.minipc.gui.panel;

import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.mycompany.minipc.gui.Tema;

/**
 * Nombre: PanelPantalla
 * Entradas: las lineas de la pantalla del Mini PC y quien recibe lo que se
 *           escribe en el teclado
 * Salidas: la "Pantalla" de la maqueta, con la linea de teclado abajo
 * Restricciones: el teclado solo se habilita cuando un proceso ejecuto
 *                INT 09H
 * Descripcion: la pantalla es de fondo oscuro con letra de ancho fijo, como
 *              un monitor. Debajo esta la linea ">> Ingresar valor:" con el
 *              campo y el boton Enter (tambien sirve la tecla ENTER).
 */
public class PanelPantalla extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextArea pantalla;
    private final JTextField teclado;
    private final JButton enter;

    /**
     * Nombre: PanelPantalla
     * Entradas: alEnviar, recibe el texto del teclado al presionar ENTER
     * Salidas: el panel construido
     * Restricciones: ninguna
     * Descripcion: arma la pantalla y la linea de teclado.
     */
    public PanelPantalla(Consumer<String> alEnviar) {
        super(new BorderLayout(0, 4));
        setBackground(Tema.TARJETA);

        pantalla = new JTextArea(6, 30);
        pantalla.setEditable(false);
        pantalla.setBackground(Tema.PANTALLA_FONDO);
        pantalla.setForeground(Tema.PANTALLA_TEXTO);
        pantalla.setFont(Tema.FUENTE_MONO.deriveFont(13f));
        pantalla.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        teclado = new JTextField(8);
        teclado.setFont(Tema.FUENTE_MONO);
        teclado.setToolTipText("Solo numeros de 0 a 255; se habilita con INT 09H");
        enter = new JButton("Enter");
        Tema.pintarBoton(enter, Tema.BOTON_EJECUTAR);
        Runnable enviar = () -> {
            String texto = teclado.getText();
            teclado.setText("");
            alEnviar.accept(texto);
        };
        teclado.addActionListener(e -> enviar.run());
        enter.addActionListener(e -> enviar.run());

        JLabel aviso = new JLabel("Teclado:");
        aviso.setFont(Tema.FUENTE_MONO);
        JPanel linea = new JPanel(new BorderLayout(6, 0));
        linea.setBackground(Tema.TARJETA);
        linea.add(aviso, BorderLayout.WEST);
        linea.add(teclado, BorderLayout.CENTER);
        linea.add(enter, BorderLayout.EAST);

        add(new JScrollPane(pantalla), BorderLayout.CENTER);
        add(linea, BorderLayout.SOUTH);
        habilitarTeclado(false);
    }

    /**
     * Nombre: mostrar
     * Entradas: lineas, contenido de la pantalla
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: reemplaza el texto y deja visible la ultima linea.
     */
    public void mostrar(List<String> lineas) {
        pantalla.setText(String.join("\n", lineas));
        pantalla.setCaretPosition(pantalla.getDocument().getLength());
    }

    /**
     * Nombre: habilitarTeclado
     * Entradas: habilitado, true si un proceso espera un valor
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: al habilitarse, el campo recibe el foco para escribir
     *              enseguida.
     */
    public void habilitarTeclado(boolean habilitado) {
        boolean cambia = teclado.isEnabled() != habilitado;
        teclado.setEnabled(habilitado);
        enter.setEnabled(habilitado);
        if (habilitado && cambia) {
            teclado.requestFocusInWindow();
        }
    }
}
