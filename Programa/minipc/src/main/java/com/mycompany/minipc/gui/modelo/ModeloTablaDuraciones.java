package com.mycompany.minipc.gui.modelo;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.mycompany.minipc.so.SistemaOperativo;
import com.mycompany.minipc.so.procesos.EstadoProceso;
import com.mycompany.minipc.so.trabajos.Trabajo;

/**
 * Nombre: ModeloTablaDuraciones
 * Entradas: los trabajos de la lista de trabajos
 * Salidas: una fila por trabajo con sus tiempos
 * Restricciones: toma una foto de los trabajos al construirse
 * Descripcion: la tabla de estadisticas: proceso, hora:minuto de inicio,
 *              hora:minuto final y duracion en segundos. Todo se mide con el
 *              reloj simulado, donde cada segundo es una unidad de peso. Ademas
 *              muestra el tiempo de CPU (la suma de los pesos que ejecuto) y la
 *              espera, que es la diferencia: el tiempo que paso en la cola,
 *              esperando el teclado o suspendido.
 */
public class ModeloTablaDuraciones extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNAS = {"Proceso", "Programa", "Inicio", "Fin",
        "Duracion (s)", "CPU (s)", "Espera (s)", "Resultado"};

    private static final String SIN_DATO = "-";

    private final List<Object[]> filas;

    /**
     * Nombre: ModeloTablaDuraciones
     * Entradas: trabajos, los de la lista de trabajos
     * Salidas: el modelo construido
     * Restricciones: ninguna
     * Descripcion: calcula las filas una sola vez.
     */
    public ModeloTablaDuraciones(List<Trabajo> trabajos) {
        this.filas = new ArrayList<>();
        for (Trabajo trabajo : trabajos) {
            filas.add(fila(trabajo));
        }
    }

    /**
     * Nombre: fila
     * Entradas: trabajo, uno de la lista de trabajos
     * Salidas: los valores de cada columna
     * Restricciones: ninguna
     * Descripcion: un trabajo sin admitir no tiene inicio; uno sin terminar
     *              no tiene fin ni duracion.
     */
    private static Object[] fila(Trabajo trabajo) {
        boolean admitido = trabajo.getInicio() >= 0;
        boolean terminado = trabajo.getFin() >= 0;
        int cpu = tiempoCpu(trabajo);
        Object duracion = SIN_DATO;
        Object espera = SIN_DATO;
        if (terminado) {
            int segundos = trabajo.getFin() - trabajo.getInicio();
            duracion = segundos;
            espera = segundos - cpu;
        } else if (admitido) {
            duracion = "en curso";
        }
        return new Object[]{
            "P" + trabajo.getPid(),
            trabajo.getPrograma(),
            admitido ? SistemaOperativo.formatearReloj(trabajo.getInicio()) : SIN_DATO,
            terminado ? SistemaOperativo.formatearReloj(trabajo.getFin()) : SIN_DATO,
            duracion,
            cpu,
            espera,
            resultado(trabajo)
        };
    }

    /**
     * Nombre: tiempoCpu
     * Entradas: trabajo
     * Salidas: los segundos de CPU que lleva o que uso
     * Restricciones: ninguna
     * Descripcion: mientras tiene BCP se lee del BCP (tiempo empleado); al
     *              terminar, del trabajo, porque el BCP ya se libero.
     */
    public static int tiempoCpu(Trabajo trabajo) {
        return trabajo.getProceso() != null ? trabajo.getProceso().getTiempoEmpleado()
                : trabajo.getTiempoCpu();
    }

    /**
     * Nombre: resultado
     * Entradas: trabajo
     * Salidas: "Correcto", "Error: ..." o el estado en que esta
     * Restricciones: ninguna
     * Descripcion: como termino el proceso, o en que va si no ha terminado.
     */
    private static String resultado(Trabajo trabajo) {
        if (trabajo.getError() != null) {
            return "Error: " + trabajo.getError();
        }
        return trabajo.getEstado() == EstadoProceso.FINALIZADO ? "Correcto"
                : trabajo.getEstado().name();
    }

    /**
     * Nombre: getRowCount
     * Entradas: ninguna
     * Salidas: cuantos trabajos hay
     * Restricciones: ninguna
     * Descripcion: una fila por trabajo.
     */
    @Override
    public int getRowCount() {
        return filas.size();
    }

    /**
     * Nombre: getColumnCount
     * Entradas: ninguna
     * Salidas: ocho columnas
     * Restricciones: ninguna
     * Descripcion: ver COLUMNAS.
     */
    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    /**
     * Nombre: getColumnName
     * Entradas: columna, indice de la columna
     * Salidas: su titulo
     * Restricciones: ninguna
     * Descripcion: lo usa el encabezado de la tabla.
     */
    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    /**
     * Nombre: isCellEditable
     * Entradas: fila y columna
     * Salidas: false
     * Restricciones: ninguna
     * Descripcion: la tabla es de solo lectura.
     */
    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    /**
     * Nombre: getValueAt
     * Entradas: fila y columna
     * Salidas: el valor de esa celda
     * Restricciones: ninguna
     * Descripcion: lee de la foto calculada al construir.
     */
    @Override
    public Object getValueAt(int fila, int columna) {
        return filas.get(fila)[columna];
    }
}
