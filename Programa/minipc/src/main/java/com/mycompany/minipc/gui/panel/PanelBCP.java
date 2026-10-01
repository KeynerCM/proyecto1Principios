package com.mycompany.minipc.gui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.mycompany.minipc.gui.Tema;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.hardware.Pila;
import com.mycompany.minipc.hardware.Procesador;
import com.mycompany.minipc.isa.RegistroID;
import com.mycompany.minipc.so.SistemaOperativo;
import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: PanelBCP
 * Entradas: el proceso en ejecucion, la CPU y la memoria
 * Salidas: el panel "BCP actual - CPU 1"
 * Restricciones: no guarda datos del proceso: cada vez que se muestra, lee
 *                las celdas del BCP en memoria a traves de Proceso
 * Descripcion: todos los campos del BCP que pide el enunciado, agrupados en
 *              Proceso, Registros, Pila, Memoria, Contabilidad y E/S. Dice
 *              tambien en que celdas del kernel quedo guardado el BCP, que es
 *              lo que pide "visualizar donde y como se almaceno".
 */
public class PanelBCP extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Texto entre comillas de la celda a la que apunta DX. */
    private static final Pattern TEXTO = Pattern.compile("\"([^\"]+)\"");

    private final JLabel pid = valor();
    private final JLabel programa = valor();
    private final JLabel estado = valor();
    private final JLabel prioridad = valor();
    private final JLabel cpu = valor();
    private final JLabel pc = valor();
    private final JLabel ir = valor();
    private final JLabel ac = valor();
    private final JLabel ax = valor();
    private final JLabel bx = valor();
    private final JLabel cx = valor();
    private final JLabel dx = valor();
    private final JLabel zf = valor();
    private final JLabel sp = valor();
    private final JLabel[] pila = new JLabel[Pila.CAPACIDAD];
    private final JLabel base = valor();
    private final JLabel alcance = valor();
    private final JLabel ubicacion = valor();
    private final JLabel siguiente = valor();
    private final JLabel inicio = valor();
    private final JLabel empleado = valor();
    private final JLabel archivos = valor();

    /**
     * Nombre: PanelBCP
     * Entradas: ninguna
     * Salidas: el panel construido, vacio
     * Restricciones: ninguna
     * Descripcion: arma los grupos de campos en una columna con desplazamiento.
     */
    public PanelBCP() {
        super(new BorderLayout());
        JPanel columna = new JPanel();
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.setBackground(Tema.TARJETA);

        columna.add(grupo("Proceso", "PID", pid, "Programa", programa, "Estado", estado,
                "Prioridad", prioridad, "CPU", cpu));
        columna.add(grupo("Registros", "PC", pc, "IR", ir, "AC", ac, "AX", ax, "BX", bx,
                "CX", cx, "DX", dx, "ZF", zf, "SP", sp));
        columna.add(grupoPila());
        columna.add(grupo("Memoria", "Base", base, "Alcance", alcance, "BCP en", ubicacion,
                "Siguiente BCP", siguiente));
        columna.add(grupo("Contabilidad", "Inicio", inicio, "Tiempo empleado", empleado));
        columna.add(grupo("E/S", "Archivos abiertos", archivos));

        JScrollPane desplazable = new JScrollPane(columna,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        desplazable.setBorder(null);
        desplazable.getVerticalScrollBar().setUnitIncrement(12);
        add(desplazable, BorderLayout.CENTER);
        setBackground(Tema.TARJETA);
        mostrar(null, null, null);
    }

    /**
     * Nombre: mostrar
     * Entradas: proceso, el que tiene la CPU, o nulo; cpu, para el avance de
     *           la instruccion en curso; memoria, para el texto al que apunta DX
     * Salidas: ninguna
     * Restricciones: con un proceso nulo deja guiones
     * Descripcion: vuelca los campos del BCP leidos de memoria.
     */
    public void mostrar(Proceso proceso, Procesador cpu, Memoria memoria) {
        if (proceso == null) {
            for (JLabel etiqueta : new JLabel[]{pid, programa, estado, prioridad, this.cpu, pc,
                ir, ac, ax, bx, cx, dx, zf, sp, base, alcance, ubicacion, siguiente, inicio,
                empleado, archivos}) {
                etiqueta.setText("-");
                etiqueta.setForeground(Tema.TEXTO);
            }
            mostrarPila(List.of(), -1);
            return;
        }
        pid.setText("P" + proceso.getPid());
        programa.setText(proceso.getPrograma());
        estado.setText(proceso.getEstado().name());
        estado.setForeground(Tema.colorEstado(proceso.getEstado()));
        prioridad.setText(String.valueOf(proceso.getPrioridad()));
        this.cpu.setText(proceso.getCpu().isEmpty() ? "-" : proceso.getCpu());

        pc.setText(String.valueOf(proceso.getPc()));
        String textoIr = proceso.getIr();
        if (!textoIr.isEmpty() && cpu != null && cpu.getPesoActual() > 0) {
            textoIr += "   (" + cpu.getSegundosCumplidos() + "/" + cpu.getPesoActual() + " s)";
        }
        ir.setText(textoIr.isEmpty() ? "-" : textoIr);
        ac.setText(String.valueOf(proceso.getAc()));
        ax.setText(proceso.getRegistro(RegistroID.AX) + "   (AH " + proceso.getRegistro(RegistroID.AH)
                + " | AL " + proceso.getRegistro(RegistroID.AL) + ")");
        bx.setText(String.valueOf(proceso.getRegistro(RegistroID.BX)));
        cx.setText(String.valueOf(proceso.getRegistro(RegistroID.CX)));
        dx.setText(textoDx(proceso, memoria));
        zf.setText(proceso.getZf() ? "1" : "0");

        List<Integer> valores = proceso.getPila();
        sp.setText(valores.size() + " de " + Pila.CAPACIDAD);
        mostrarPila(valores, proceso.getRanura());

        base.setText(String.valueOf(proceso.getBase()));
        alcance.setText(proceso.getAlcance() + " posiciones");
        ubicacion.setText(proceso.getDireccionBCP() + " a " + proceso.getDireccionFinBCP()
                + " (ranura " + proceso.getRanura() + ")");
        Proceso sig = proceso.getSiguiente();
        siguiente.setText(sig == null ? "ninguno" : sig + " en " + sig.getDireccionBCP());
        inicio.setText(SistemaOperativo.formatearReloj(proceso.getTiempoInicio()));
        empleado.setText(proceso.getTiempoEmpleado() + " s");
        List<String> abiertos = proceso.getArchivosAbiertos();
        archivos.setText(abiertos.isEmpty() ? "ninguno" : String.join(", ", abiertos));
    }

    /**
     * Nombre: textoDx
     * Entradas: proceso; memoria
     * Salidas: el valor de DX y, si apunta a un nombre dentro del programa,
     *          ese nombre, por ejemplo "130 -> \"datos.txt\""
     * Restricciones: ninguna
     * Descripcion: asi se ve que DX "contiene" el nombre del archivo (9.3).
     */
    private static String textoDx(Proceso proceso, Memoria memoria) {
        int valor = proceso.getRegistro(RegistroID.DX);
        if (memoria != null && valor >= proceso.getBase()
                && valor < proceso.getBase() + proceso.getAlcance()) {
            Matcher texto = TEXTO.matcher(memoria.leer(valor));
            if (texto.find()) {
                return valor + "  ->  \"" + texto.group(1) + "\"";
            }
        }
        return String.valueOf(valor);
    }

    /**
     * Nombre: mostrarPila
     * Entradas: valores, contenido de la pila del fondo al tope; ranura, la
     *           del proceso, para usar su color
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: una casilla por posicion; el tope se marca con color.
     */
    private void mostrarPila(List<Integer> valores, int ranura) {
        for (int i = 0; i < pila.length; i++) {
            boolean ocupada = i < valores.size();
            pila[i].setText(ocupada ? String.valueOf(valores.get(i)) : " ");
            boolean tope = ocupada && i == valores.size() - 1;
            pila[i].setBackground(tope ? Tema.RESALTADO
                    : ocupada ? Tema.colorProcesoClaro(ranura) : Tema.TARJETA);
        }
    }

    /**
     * Nombre: grupo
     * Entradas: titulo; pares rotulo y etiqueta de valor
     * Salidas: el panel del grupo
     * Restricciones: los pares van alternados: String, JLabel, String, JLabel
     * Descripcion: dos columnas, rotulo gris y valor en negrita.
     */
    private static JPanel grupo(String titulo, Object... pares) {
        JPanel filas = new JPanel(new GridLayout(0, 2, 8, 3));
        filas.setBackground(Tema.TARJETA);
        for (int i = 0; i < pares.length; i += 2) {
            JLabel rotulo = new JLabel((String) pares[i]);
            rotulo.setFont(Tema.FUENTE);
            rotulo.setForeground(Tema.TEXTO_SUAVE);
            filas.add(rotulo);
            filas.add((JLabel) pares[i + 1]);
        }
        return envolver(titulo, filas);
    }

    /**
     * Nombre: grupoPila
     * Entradas: ninguna
     * Salidas: el grupo de la pila: cinco casillas
     * Restricciones: ninguna
     * Descripcion: la pila se dibuja como cinco casillas del fondo al tope.
     */
    private JPanel grupoPila() {
        JPanel casillas = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        casillas.setBackground(Tema.TARJETA);
        for (int i = 0; i < pila.length; i++) {
            pila[i] = new JLabel(" ", SwingConstants.CENTER);
            pila[i].setOpaque(true);
            pila[i].setFont(Tema.FUENTE_MONO);
            pila[i].setPreferredSize(new Dimension(36, 24));
            pila[i].setBorder(BorderFactory.createLineBorder(Tema.BORDE));
            pila[i].setToolTipText("Posicion " + (i + 1) + (i == 0 ? " (fondo)" : ""));
            casillas.add(pila[i]);
        }
        return envolver("Pila (fondo a tope)", casillas);
    }

    /**
     * Nombre: envolver
     * Entradas: titulo; contenido
     * Salidas: el contenido con su titulo pequeno arriba
     * Restricciones: ninguna
     * Descripcion: subtitulo de grupo dentro del panel.
     */
    private static JPanel envolver(String titulo, JPanel contenido) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setBackground(Tema.TARJETA);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        JLabel rotulo = new JLabel(titulo);
        rotulo.setFont(Tema.FUENTE_NEGRITA);
        rotulo.setForeground(Tema.TITULO_SECCION);
        rotulo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE));
        panel.add(rotulo, BorderLayout.NORTH);
        panel.add(contenido, BorderLayout.CENTER);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    /**
     * Nombre: valor
     * Entradas: ninguna
     * Salidas: una etiqueta para un valor del BCP
     * Restricciones: ninguna
     * Descripcion: todos los valores con la misma letra y color.
     */
    private static JLabel valor() {
        JLabel etiqueta = new JLabel("-");
        etiqueta.setFont(Tema.FUENTE_NEGRITA);
        etiqueta.setForeground(Tema.TEXTO);
        return etiqueta;
    }
}
