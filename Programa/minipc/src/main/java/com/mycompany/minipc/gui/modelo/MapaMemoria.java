package com.mycompany.minipc.gui.modelo;

import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.so.procesos.CampoBCP;
import com.mycompany.minipc.so.procesos.Proceso;
import com.mycompany.minipc.so.procesos.TablaBCP;

/**
 * Nombre: MapaMemoria
 * Entradas: la memoria y la tabla de BCP
 * Salidas: que es y de quien es cada direccion
 * Restricciones: solo lee
 * Descripcion: la memoria no guarda para que sirve cada celda; esta clase lo
 *              calcula a partir de la direccion. En el kernel usa la formula
 *              de la tabla de BCP; en la zona de usuario busca el proceso cuya
 *              base y alcance contienen la direccion. Lo comparten el modelo y
 *              el renderer de la tabla de memoria.
 */
public class MapaMemoria {

    /** Ranura que se devuelve para la cabecera del sistema operativo. */
    public static final int CABECERA = -2;

    /** Ranura que se devuelve para una celda que no es de ningun proceso. */
    public static final int NINGUNA = -1;

    private final Memoria memoria;
    private final TablaBCP tabla;

    /**
     * Nombre: MapaMemoria
     * Entradas: memoria, memoria principal; tabla, tabla de BCP
     * Salidas: el mapa construido
     * Restricciones: ninguna
     * Descripcion: guarda las referencias.
     */
    public MapaMemoria(Memoria memoria, TablaBCP tabla) {
        this.memoria = memoria;
        this.tabla = tabla;
    }

    /**
     * Nombre: describir
     * Entradas: direccion, posicion de memoria
     * Salidas: el nombre de la celda: "SO.Admitidos", "P2.PC", "P1" para una
     *          instruccion de un programa, o vacio si esta libre
     * Restricciones: ninguna
     * Descripcion: es lo que se ve en la columna Campo.
     */
    public String describir(int direccion) {
        if (memoria.esDireccionKernel(direccion)) {
            return tabla.describir(direccion);
        }
        Proceso dueno = duenoDe(direccion);
        return dueno == null ? "" : dueno.toString();
    }

    /**
     * Nombre: ranuraDe
     * Entradas: direccion, posicion de memoria
     * Salidas: CABECERA, la ranura (0 a 4) del proceso al que pertenece, o
     *          NINGUNA
     * Restricciones: ninguna
     * Descripcion: el renderer pinta cada ranura con su color.
     */
    public int ranuraDe(int direccion) {
        if (direccion < TablaBCP.TAMANO_CABECERA) {
            return CABECERA;
        }
        if (memoria.esDireccionKernel(direccion)) {
            int ranura = TablaBCP.ranuraDe(direccion);
            if (ranura < 0 || ranura >= tabla.getRanuras()
                    || tabla.leer(TablaBCP.direccionBCP(ranura), CampoBCP.PID).isEmpty()) {
                return NINGUNA;
            }
            return ranura;
        }
        Proceso dueno = duenoDe(direccion);
        return dueno == null ? NINGUNA : dueno.getRanura();
    }

    /**
     * Nombre: duenoDe
     * Entradas: direccion, posicion de la zona de usuario
     * Salidas: el proceso cuya region contiene la direccion, o nulo
     * Restricciones: ninguna
     * Descripcion: compara contra la base y el alcance de cada BCP. Los
     *              procesos suspendidos se saltan: su BASE es del disco.
     */
    private Proceso duenoDe(int direccion) {
        for (Proceso proceso : tabla.getProcesos()) {
            if (proceso.estaEnMemoriaVirtual()) {
                continue;
            }
            int base = proceso.getBase();
            if (direccion >= base && direccion < base + proceso.getAlcance()) {
                return proceso;
            }
        }
        return null;
    }
}
