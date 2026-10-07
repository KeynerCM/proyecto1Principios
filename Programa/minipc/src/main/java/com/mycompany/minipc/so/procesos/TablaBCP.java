package com.mycompany.minipc.so.procesos;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.minipc.hardware.Memoria;

/**
 * Nombre: TablaBCP
 * Entradas: la memoria principal donde viven la cabecera y los BCP
 * Salidas: no aplica
 * Restricciones: es la unica clase que conoce la formula del kernel; las demas
 *                piden direcciones aqui
 * Descripcion: tecnica de calculo de la memoria del kernel. El kernel guarda
 *              una cabecera de C celdas y una tabla de P ranuras, cada una
 *              con un BCP de B celdas:
 *
 *                K = C + P x B = 3 + 5 x 25 = 128 celdas
 *
 *                0 .. 2      cabecera del sistema operativo
 *                3 .. 27     BCP de la ranura 0
 *                28 .. 52    BCP de la ranura 1
 *                ...
 *                103 .. 127  BCP de la ranura 4
 *
 *              La direccion de un campo es base + desplazamiento, como en un
 *              arreglo de registros: dirBCP(i) = C + i x B, y el campo esta en
 *              dirBCP(i) + su desplazamiento. Al reves, de una direccion se
 *              obtiene la ranura con (dir - C) / B y el campo con (dir - C) % B.
 *              C y B salen de los enum CampoCabecera y CampoBCP, asi que la
 *              formula se ajusta sola si se agrega un campo. Una ranura esta
 *              libre cuando su celda PID esta vacia.
 */
public class TablaBCP {

    /** Procesos admitidos a la vez: "podra ejecutar hasta 5 procesos". */
    public static final int MAX_PROCESOS = 5;

    /** C: celdas de la cabecera. */
    public static final int TAMANO_CABECERA = CampoCabecera.values().length;

    /** B: celdas de cada BCP. */
    public static final int TAMANO_BCP = CampoBCP.values().length;

    /** K = C + P x B: celdas del kernel. */
    public static final int TAMANO_KERNEL = TAMANO_CABECERA + MAX_PROCESOS * TAMANO_BCP;

    private final Memoria memoria;

    /**
     * Nombre: TablaBCP
     * Entradas: memoria, memoria principal cuya zona de kernel mide al menos K
     * Salidas: la tabla construida, con la cabecera inicializada
     * Restricciones: lanza IllegalArgumentException si el kernel de la memoria
     *                es menor que K
     * Descripcion: deja la cabecera en su estado inicial: nada en ejecucion,
     *              lista vacia y cero procesos admitidos.
     */
    public TablaBCP(Memoria memoria) {
        if (memoria.getLimiteKernel() < TAMANO_KERNEL) {
            throw new IllegalArgumentException("La zona de kernel mide "
                    + memoria.getLimiteKernel() + " y la tabla de BCP necesita " + TAMANO_KERNEL);
        }
        this.memoria = memoria;
        formatear();
    }

    /**
     * Nombre: describirFormula
     * Entradas: ninguna
     * Salidas: la formula del kernel con sus valores, en texto
     * Restricciones: ninguna
     * Descripcion: se muestra en la configuracion y en la consola.
     */
    public static String describirFormula() {
        return TAMANO_KERNEL + " celdas (" + TAMANO_CABECERA + " de cabecera + "
                + MAX_PROCESOS + " BCP x " + TAMANO_BCP + ")";
    }

    /**
     * Nombre: direccionBCP
     * Entradas: ranura, posicion en la tabla, de 0 a 4
     * Salidas: la direccion donde empieza el BCP de esa ranura
     * Restricciones: lanza IllegalArgumentException si la ranura no existe
     * Descripcion: dirBCP(i) = C + i x B.
     */
    public static int direccionBCP(int ranura) {
        if (ranura < 0 || ranura >= MAX_PROCESOS) {
            throw new IllegalArgumentException("Ranura de BCP inexistente: " + ranura);
        }
        return TAMANO_CABECERA + ranura * TAMANO_BCP;
    }

    /**
     * Nombre: direccionCampo
     * Entradas: direccionBCP, donde empieza el BCP; campo, campo buscado
     * Salidas: la direccion de la celda del campo
     * Restricciones: ninguna
     * Descripcion: base + desplazamiento.
     */
    public static int direccionCampo(int direccionBCP, CampoBCP campo) {
        return direccionBCP + campo.getDesplazamiento();
    }

    /**
     * Nombre: ranuraDe
     * Entradas: direccion, cualquier direccion de memoria
     * Salidas: la ranura de BCP que contiene esa direccion, o -1 si la
     *          direccion no esta en la tabla de BCP
     * Restricciones: ninguna
     * Descripcion: (dir - C) / B.
     */
    public static int ranuraDe(int direccion) {
        if (direccion < TAMANO_CABECERA || direccion >= TAMANO_KERNEL) {
            return -1;
        }
        return (direccion - TAMANO_CABECERA) / TAMANO_BCP;
    }

    /**
     * Nombre: campoDe
     * Entradas: direccion, una direccion dentro de la tabla de BCP
     * Salidas: el campo que ocupa esa direccion, o nulo si no esta en la tabla
     * Restricciones: ninguna
     * Descripcion: (dir - C) % B.
     */
    public static CampoBCP campoDe(int direccion) {
        if (ranuraDe(direccion) < 0) {
            return null;
        }
        return CampoBCP.values()[(direccion - TAMANO_CABECERA) % TAMANO_BCP];
    }

    /**
     * Nombre: describir
     * Entradas: direccion, cualquier direccion del kernel
     * Salidas: el nombre de lo que guarda esa celda, por ejemplo "SO.Admitidos"
     *          o "P2.PC"; "BCP libre" si la ranura no se usa; vacio si la
     *          direccion no es del kernel
     * Restricciones: ninguna
     * Descripcion: lo usa la tabla de memoria para mostrar donde y como quedo
     *              guardado cada BCP.
     */
    public String describir(int direccion) {
        if (direccion >= 0 && direccion < TAMANO_CABECERA) {
            return "SO." + CampoCabecera.values()[direccion].getEtiqueta();
        }
        int ranura = ranuraDe(direccion);
        if (ranura < 0) {
            return "";
        }
        String pid = leer(direccionBCP(ranura), CampoBCP.PID);
        if (pid.isEmpty()) {
            return "BCP libre";
        }
        return "P" + pid + "." + campoDe(direccion).getEtiqueta();
    }

    /**
     * Nombre: crear
     * Entradas: pid, identificador; programa, nombre del archivo; base y
     *           alcance, donde quedo el programa en memoria; reloj, segundo
     *           simulado en que se crea
     * Salidas: el proceso, que apunta a su BCP en la primera ranura libre
     * Restricciones: lanza IllegalStateException si las cinco ranuras estan
     *                ocupadas
     * Descripcion: escribe el BCP inicial en memoria: estado PREPARADO, PC en
     *              la base, registros y pila en cero, y suma uno a los
     *              procesos admitidos de la cabecera.
     */
    public Proceso crear(int pid, String programa, int base, int alcance, int reloj) {
        int ranura = primeraRanuraLibre();
        if (ranura < 0) {
            throw new IllegalStateException("No hay ranuras de BCP libres: ya hay "
                    + MAX_PROCESOS + " procesos admitidos");
        }
        int dir = direccionBCP(ranura);
        for (CampoBCP campo : CampoBCP.values()) {
            memoria.escribir(direccionCampo(dir, campo), Memoria.VACIA);
        }
        Proceso proceso = new Proceso(this, dir);
        escribirEntero(dir, CampoBCP.PID, pid);
        escribir(dir, CampoBCP.PROGRAMA, programa);
        proceso.setEstado(EstadoProceso.PREPARADO);
        escribirEntero(dir, CampoBCP.PRIORIDAD, 0);
        escribirEntero(dir, CampoBCP.PC, base);
        for (CampoBCP campo : new CampoBCP[]{CampoBCP.AC, CampoBCP.AX, CampoBCP.BX,
            CampoBCP.CX, CampoBCP.DX, CampoBCP.ZF, CampoBCP.SP}) {
            escribirEntero(dir, campo, 0);
        }
        escribirEntero(dir, CampoBCP.BASE, base);
        escribirEntero(dir, CampoBCP.ALCANCE, alcance);
        escribirEntero(dir, CampoBCP.TIEMPO_INICIO, reloj);
        escribirEntero(dir, CampoBCP.TIEMPO_EMPLEADO, 0);
        cambiarAdmitidos(1);
        return proceso;
    }

    /**
     * Nombre: liberar
     * Entradas: proceso, proceso cuyo BCP se descarta
     * Salidas: ninguna
     * Restricciones: el proceso ya no debe estar en la lista de procesos
     * Descripcion: deja vacias las celdas del BCP, lo que libera la ranura, y
     *              resta uno a los procesos admitidos.
     */
    public void liberar(Proceso proceso) {
        if (proceso.equals(getEnEjecucion())) {
            setEnEjecucion(null);
        }
        for (CampoBCP campo : CampoBCP.values()) {
            memoria.escribir(direccionCampo(proceso.getDireccionBCP(), campo), Memoria.VACIA);
        }
        cambiarAdmitidos(-1);
    }

    /**
     * Nombre: formatear
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: descarta todos los BCP
     * Descripcion: vacia la tabla y deja la cabecera en su estado inicial.
     */
    public final void formatear() {
        for (int i = 0; i < TAMANO_KERNEL; i++) {
            memoria.escribir(i, Memoria.VACIA);
        }
        memoria.escribirEntero(CampoCabecera.PROCESOS_ADMITIDOS.getDireccion(), 0);
    }

    /**
     * Nombre: hayRanuraLibre
     * Entradas: ninguna
     * Salidas: true si todavia se puede admitir un proceso
     * Restricciones: ninguna
     * Descripcion: lo consulta el planificador de trabajos antes de admitir.
     */
    public boolean hayRanuraLibre() {
        return primeraRanuraLibre() >= 0;
    }

    /**
     * Nombre: getProcesosAdmitidos
     * Entradas: ninguna
     * Salidas: cuantos procesos tienen BCP, leido de la cabecera
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura a la celda de la cabecera.
     */
    public int getProcesosAdmitidos() {
        return memoria.leerEntero(CampoCabecera.PROCESOS_ADMITIDOS.getDireccion());
    }

    /**
     * Nombre: getProcesos
     * Entradas: ninguna
     * Salidas: los procesos con BCP, en orden de ranura
     * Restricciones: el orden es el de la tabla, no el de la lista de procesos
     * Descripcion: recorre las ranuras ocupadas. Sirve para saber a que
     *              proceso pertenece una celda de la zona de usuario.
     */
    public List<Proceso> getProcesos() {
        List<Proceso> procesos = new ArrayList<>();
        for (int ranura = 0; ranura < MAX_PROCESOS; ranura++) {
            int dir = direccionBCP(ranura);
            if (!leer(dir, CampoBCP.PID).isEmpty()) {
                procesos.add(new Proceso(this, dir));
            }
        }
        return procesos;
    }

    /**
     * Nombre: getEnEjecucion
     * Entradas: ninguna
     * Salidas: el proceso que tiene la CPU, o nulo si esta libre
     * Restricciones: ninguna
     * Descripcion: lee el puntero de la cabecera.
     */
    public Proceso getEnEjecucion() {
        return procesoApuntadoPor(CampoCabecera.EN_EJECUCION.getDireccion());
    }

    /**
     * Nombre: setEnEjecucion
     * Entradas: proceso, el que recibe la CPU, o nulo para dejarla libre
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: escribe el puntero de la cabecera.
     */
    public void setEnEjecucion(Proceso proceso) {
        apuntar(CampoCabecera.EN_EJECUCION.getDireccion(), proceso);
    }

    /**
     * Nombre: getInicioLista
     * Entradas: ninguna
     * Salidas: el primer proceso de la lista de procesos, o nulo si esta vacia
     * Restricciones: ninguna
     * Descripcion: lee el puntero de la cabecera.
     */
    public Proceso getInicioLista() {
        return procesoApuntadoPor(CampoCabecera.INICIO_LISTA.getDireccion());
    }

    /**
     * Nombre: setInicioLista
     * Entradas: proceso, nuevo primero de la lista, o nulo si queda vacia
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: escribe el puntero de la cabecera.
     */
    public void setInicioLista(Proceso proceso) {
        apuntar(CampoCabecera.INICIO_LISTA.getDireccion(), proceso);
    }

    /**
     * Nombre: leer
     * Entradas: direccionBCP, donde empieza el BCP; campo, campo a leer
     * Salidas: el texto guardado en la celda del campo
     * Restricciones: ninguna
     * Descripcion: acceso a un campo del BCP a traves de la memoria.
     */
    public String leer(int direccionBCP, CampoBCP campo) {
        return memoria.leer(direccionCampo(direccionBCP, campo));
    }

    /**
     * Nombre: leerEntero
     * Entradas: direccionBCP, donde empieza el BCP; campo, campo a leer
     * Salidas: el valor del campo convertido a int, o cero si esta vacio
     * Restricciones: el campo debe guardar un numero
     * Descripcion: para los campos que se usan en calculos.
     */
    public int leerEntero(int direccionBCP, CampoBCP campo) {
        return memoria.leerEntero(direccionCampo(direccionBCP, campo));
    }

    /**
     * Nombre: escribir
     * Entradas: direccionBCP, donde empieza el BCP; campo, campo a escribir;
     *           texto, valor a guardar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: acceso a un campo del BCP a traves de la memoria.
     */
    public void escribir(int direccionBCP, CampoBCP campo, String texto) {
        memoria.escribir(direccionCampo(direccionBCP, campo), texto);
    }

    /**
     * Nombre: escribirEntero
     * Entradas: direccionBCP, donde empieza el BCP; campo, campo a escribir;
     *           valor, numero a guardar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: guarda el numero como texto en la celda del campo.
     */
    public void escribirEntero(int direccionBCP, CampoBCP campo, int valor) {
        memoria.escribirEntero(direccionCampo(direccionBCP, campo), valor);
    }

    /**
     * Nombre: getMemoria
     * Entradas: ninguna
     * Salidas: la memoria donde vive la tabla
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Memoria getMemoria() {
        return memoria;
    }

    /**
     * Nombre: primeraRanuraLibre
     * Entradas: ninguna
     * Salidas: la primera ranura con la celda PID vacia, o -1 si no hay
     * Restricciones: ninguna
     * Descripcion: primer ajuste sobre las cinco ranuras.
     */
    private int primeraRanuraLibre() {
        for (int ranura = 0; ranura < MAX_PROCESOS; ranura++) {
            if (leer(direccionBCP(ranura), CampoBCP.PID).isEmpty()) {
                return ranura;
            }
        }
        return -1;
    }

    /**
     * Nombre: cambiarAdmitidos
     * Entradas: delta, 1 al admitir o -1 al liberar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: actualiza el contador de la cabecera.
     */
    private void cambiarAdmitidos(int delta) {
        memoria.escribirEntero(CampoCabecera.PROCESOS_ADMITIDOS.getDireccion(),
                getProcesosAdmitidos() + delta);
    }

    /**
     * Nombre: procesoApuntadoPor
     * Entradas: direccion, celda que guarda la direccion de un BCP
     * Salidas: el proceso de ese BCP, o nulo si la celda esta vacia
     * Restricciones: ninguna
     * Descripcion: interpreta una celda como puntero a un BCP.
     */
    Proceso procesoApuntadoPor(int direccion) {
        if (memoria.estaLibre(direccion)) {
            return null;
        }
        return new Proceso(this, memoria.leerEntero(direccion));
    }

    /**
     * Nombre: apuntar
     * Entradas: direccion, celda puntero; proceso, al que debe apuntar, o nulo
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: guarda la direccion del BCP, o deja la celda vacia.
     */
    void apuntar(int direccion, Proceso proceso) {
        if (proceso == null) {
            memoria.escribir(direccion, Memoria.VACIA);
        } else {
            memoria.escribirEntero(direccion, proceso.getDireccionBCP());
        }
    }
}
