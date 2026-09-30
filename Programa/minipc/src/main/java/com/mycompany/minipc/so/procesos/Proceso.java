package com.mycompany.minipc.so.procesos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mycompany.minipc.hardware.Pila;
import com.mycompany.minipc.isa.RegistroID;

/**
 * Nombre: Proceso
 * Entradas: la tabla de BCP y la direccion donde empieza el BCP del proceso
 * Salidas: no aplica
 * Restricciones: no guarda ningun dato del proceso: solo la direccion de su
 *                BCP. Deja de ser valido cuando el BCP se libera
 * Descripcion: la entidad proceso. Todo lo que se consulta o cambia de un
 *              proceso (estado, PC, registros, pila, base, alcance, enlace al
 *              siguiente) se lee o se escribe directamente en las celdas de
 *              su BCP en la memoria del kernel, que es donde el enunciado pide
 *              que viva esa informacion. Dos objetos Proceso con la misma
 *              direccion son el mismo proceso.
 */
public final class Proceso {

    /** Separa los nombres en la celda de archivos abiertos. */
    public static final String SEPARADOR_ARCHIVOS = ";";

    private final TablaBCP tabla;
    private final int direccionBCP;

    /**
     * Nombre: Proceso
     * Entradas: tabla, tabla de BCP; direccionBCP, donde empieza su BCP
     * Salidas: el proceso construido
     * Restricciones: solo la tabla crea procesos, por eso no es publico
     * Descripcion: guarda las dos referencias que permiten llegar a las
     *              celdas del BCP.
     */
    Proceso(TablaBCP tabla, int direccionBCP) {
        this.tabla = tabla;
        this.direccionBCP = direccionBCP;
    }

    /**
     * Nombre: getDireccionBCP
     * Entradas: ninguna
     * Salidas: la direccion de la primera celda del BCP
     * Restricciones: ninguna
     * Descripcion: es el unico dato que guarda el objeto.
     */
    public int getDireccionBCP() {
        return direccionBCP;
    }

    /**
     * Nombre: getDireccionFinBCP
     * Entradas: ninguna
     * Salidas: la direccion de la ultima celda del BCP
     * Restricciones: ninguna
     * Descripcion: sirve para mostrar el rango, por ejemplo "BCP en 28..52".
     */
    public int getDireccionFinBCP() {
        return direccionBCP + TablaBCP.TAMANO_BCP - 1;
    }

    /**
     * Nombre: getRanura
     * Entradas: ninguna
     * Salidas: la ranura de la tabla de BCP, de 0 a 4
     * Restricciones: ninguna
     * Descripcion: la calcula la tabla a partir de la direccion.
     */
    public int getRanura() {
        return TablaBCP.ranuraDe(direccionBCP);
    }

    /**
     * Nombre: getPid
     * Entradas: ninguna
     * Salidas: el identificador del proceso, leido del BCP
     * Restricciones: ninguna
     * Descripcion: acceso al campo PID.
     */
    public int getPid() {
        return entero(CampoBCP.PID);
    }

    /**
     * Nombre: getPrograma
     * Entradas: ninguna
     * Salidas: el nombre del archivo del programa, leido del BCP
     * Restricciones: ninguna
     * Descripcion: acceso al campo PROGRAMA.
     */
    public String getPrograma() {
        return tabla.leer(direccionBCP, CampoBCP.PROGRAMA);
    }

    /**
     * Nombre: getEstado
     * Entradas: ninguna
     * Salidas: el estado del proceso, leido del BCP
     * Restricciones: ninguna
     * Descripcion: la celda guarda el nombre del estado como texto.
     */
    public EstadoProceso getEstado() {
        return EstadoProceso.valueOf(tabla.leer(direccionBCP, CampoBCP.ESTADO));
    }

    /**
     * Nombre: setEstado
     * Entradas: estado, nuevo estado
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: escribe el nombre del estado en su celda.
     */
    public void setEstado(EstadoProceso estado) {
        tabla.escribir(direccionBCP, CampoBCP.ESTADO, estado.name());
    }

    /**
     * Nombre: getPrioridad
     * Entradas: ninguna
     * Salidas: la prioridad guardada en el BCP
     * Restricciones: FCFS no la usa
     * Descripcion: acceso al campo PRIORIDAD.
     */
    public int getPrioridad() {
        return entero(CampoBCP.PRIORIDAD);
    }

    /**
     * Nombre: getPc
     * Entradas: ninguna
     * Salidas: el contador de programa guardado en el BCP
     * Restricciones: ninguna
     * Descripcion: acceso al campo PC.
     */
    public int getPc() {
        return entero(CampoBCP.PC);
    }

    /**
     * Nombre: setPc
     * Entradas: pc, nuevo valor
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: escribe el campo PC.
     */
    public void setPc(int pc) {
        tabla.escribirEntero(direccionBCP, CampoBCP.PC, pc);
    }

    /**
     * Nombre: getIr
     * Entradas: ninguna
     * Salidas: el texto de la instruccion del IR guardado en el BCP
     * Restricciones: es vacio si todavia no se ejecuto nada
     * Descripcion: acceso al campo IR.
     */
    public String getIr() {
        return tabla.leer(direccionBCP, CampoBCP.IR);
    }

    /**
     * Nombre: setIr
     * Entradas: ir, texto de la instruccion
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: escribe el campo IR.
     */
    public void setIr(String ir) {
        tabla.escribir(direccionBCP, CampoBCP.IR, ir);
    }

    /**
     * Nombre: getAc
     * Entradas: ninguna
     * Salidas: el acumulador guardado en el BCP
     * Restricciones: ninguna
     * Descripcion: acceso al campo AC.
     */
    public int getAc() {
        return entero(CampoBCP.AC);
    }

    /**
     * Nombre: setAc
     * Entradas: ac, nuevo valor
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: escribe el campo AC.
     */
    public void setAc(int ac) {
        tabla.escribirEntero(direccionBCP, CampoBCP.AC, ac);
    }

    /**
     * Nombre: getRegistro
     * Entradas: id, registro de proposito general
     * Salidas: el valor guardado en el BCP
     * Restricciones: ninguna
     * Descripcion: AX, BX, CX y DX tienen cada uno su celda.
     */
    public int getRegistro(RegistroID id) {
        return entero(campoDe(id));
    }

    /**
     * Nombre: setRegistro
     * Entradas: id, registro; valor, nuevo valor
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: escribe la celda del registro.
     */
    public void setRegistro(RegistroID id, int valor) {
        tabla.escribirEntero(direccionBCP, campoDe(id), valor);
    }

    /**
     * Nombre: getZf
     * Entradas: ninguna
     * Salidas: la bandera de cero guardada en el BCP
     * Restricciones: ninguna
     * Descripcion: la celda guarda 1 o 0.
     */
    public boolean getZf() {
        return entero(CampoBCP.ZF) != 0;
    }

    /**
     * Nombre: setZf
     * Entradas: zf, nuevo valor de la bandera
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: guarda 1 o 0.
     */
    public void setZf(boolean zf) {
        tabla.escribirEntero(direccionBCP, CampoBCP.ZF, zf ? 1 : 0);
    }

    /**
     * Nombre: getPila
     * Entradas: ninguna
     * Salidas: los valores de la pila, del fondo al tope
     * Restricciones: la lista es una copia
     * Descripcion: lee SP celdas de pila, que es cuantas estan ocupadas.
     */
    public List<Integer> getPila() {
        int sp = entero(CampoBCP.SP);
        List<Integer> valores = new ArrayList<>();
        for (int i = 0; i < sp; i++) {
            valores.add(entero(CampoBCP.pila(i)));
        }
        return valores;
    }

    /**
     * Nombre: setPila
     * Entradas: valores, contenido de la pila del fondo al tope
     * Salidas: ninguna
     * Restricciones: como maximo 5 valores
     * Descripcion: escribe SP y las celdas ocupadas; las demas quedan vacias.
     */
    public void setPila(List<Integer> valores) {
        tabla.escribirEntero(direccionBCP, CampoBCP.SP, valores.size());
        for (int i = 0; i < Pila.CAPACIDAD; i++) {
            if (i < valores.size()) {
                tabla.escribirEntero(direccionBCP, CampoBCP.pila(i), valores.get(i));
            } else {
                tabla.escribir(direccionBCP, CampoBCP.pila(i), "");
            }
        }
    }

    /**
     * Nombre: getBase
     * Entradas: ninguna
     * Salidas: la direccion donde empieza el programa en memoria
     * Restricciones: ninguna
     * Descripcion: acceso al campo BASE.
     */
    public int getBase() {
        return entero(CampoBCP.BASE);
    }

    /**
     * Nombre: getAlcance
     * Entradas: ninguna
     * Salidas: cuantas posiciones ocupa el programa
     * Restricciones: ninguna
     * Descripcion: acceso al campo ALCANCE.
     */
    public int getAlcance() {
        return entero(CampoBCP.ALCANCE);
    }

    /**
     * Nombre: getCpu
     * Entradas: ninguna
     * Salidas: la CPU donde se ejecuta, o vacio si no esta en ejecucion
     * Restricciones: ninguna
     * Descripcion: parte de la informacion contable del enunciado.
     */
    public String getCpu() {
        return tabla.leer(direccionBCP, CampoBCP.CPU);
    }

    /**
     * Nombre: setCpu
     * Entradas: cpu, nombre de la CPU, o vacio
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: escribe el campo CPU.
     */
    public void setCpu(String cpu) {
        tabla.escribir(direccionBCP, CampoBCP.CPU, cpu);
    }

    /**
     * Nombre: getTiempoInicio
     * Entradas: ninguna
     * Salidas: el segundo simulado en que el proceso fue admitido
     * Restricciones: ninguna
     * Descripcion: parte de la informacion contable del enunciado.
     */
    public int getTiempoInicio() {
        return entero(CampoBCP.TIEMPO_INICIO);
    }

    /**
     * Nombre: getTiempoEmpleado
     * Entradas: ninguna
     * Salidas: los segundos de CPU que lleva el proceso
     * Restricciones: ninguna
     * Descripcion: parte de la informacion contable del enunciado.
     */
    public int getTiempoEmpleado() {
        return entero(CampoBCP.TIEMPO_EMPLEADO);
    }

    /**
     * Nombre: sumarTiempoEmpleado
     * Entradas: segundos, tiempo de CPU a sumar
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: lo llama el sistema operativo en cada segundo que el
     *              proceso tiene la CPU.
     */
    public void sumarTiempoEmpleado(int segundos) {
        tabla.escribirEntero(direccionBCP, CampoBCP.TIEMPO_EMPLEADO,
                getTiempoEmpleado() + segundos);
    }

    /**
     * Nombre: getArchivosAbiertos
     * Entradas: ninguna
     * Salidas: los nombres de los archivos abiertos
     * Restricciones: la lista es una copia
     * Descripcion: la celda guarda los nombres separados por punto y coma.
     */
    public List<String> getArchivosAbiertos() {
        String texto = tabla.leer(direccionBCP, CampoBCP.ARCHIVOS_ABIERTOS);
        return texto.isEmpty() ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(texto.split(SEPARADOR_ARCHIVOS)));
    }

    /**
     * Nombre: getSiguiente
     * Entradas: ninguna
     * Salidas: el proceso cuyo BCP sigue en la lista de procesos, o nulo
     * Restricciones: ninguna
     * Descripcion: el enlace al siguiente BCP es una direccion de memoria.
     */
    public Proceso getSiguiente() {
        return tabla.procesoApuntadoPor(TablaBCP.direccionCampo(direccionBCP,
                CampoBCP.SIGUIENTE));
    }

    /**
     * Nombre: setSiguiente
     * Entradas: siguiente, proceso al que debe apuntar, o nulo
     * Salidas: ninguna
     * Restricciones: solo la lista de procesos debe cambiar los enlaces
     * Descripcion: guarda la direccion del BCP siguiente en el campo SIGUIENTE.
     */
    public void setSiguiente(Proceso siguiente) {
        tabla.apuntar(TablaBCP.direccionCampo(direccionBCP, CampoBCP.SIGUIENTE), siguiente);
    }

    /**
     * Nombre: equals
     * Entradas: otro, objeto a comparar
     * Salidas: true si apunta al mismo BCP
     * Restricciones: ninguna
     * Descripcion: la identidad del proceso es la direccion de su BCP.
     */
    @Override
    public boolean equals(Object otro) {
        return otro instanceof Proceso && ((Proceso) otro).direccionBCP == direccionBCP;
    }

    /**
     * Nombre: hashCode
     * Entradas: ninguna
     * Salidas: el codigo hash coherente con equals
     * Restricciones: ninguna
     * Descripcion: necesario al redefinir equals.
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(direccionBCP);
    }

    /**
     * Nombre: toString
     * Entradas: ninguna
     * Salidas: el nombre corto del proceso, por ejemplo "P2"
     * Restricciones: ninguna
     * Descripcion: se usa en los mensajes de la consola.
     */
    @Override
    public String toString() {
        return "P" + getPid();
    }

    /**
     * Nombre: entero
     * Entradas: campo, campo numerico
     * Salidas: su valor como int
     * Restricciones: ninguna
     * Descripcion: atajo para leer un campo numerico del BCP.
     */
    private int entero(CampoBCP campo) {
        return tabla.leerEntero(direccionBCP, campo);
    }

    /**
     * Nombre: campoDe
     * Entradas: id, registro de proposito general
     * Salidas: el campo del BCP que lo guarda
     * Restricciones: ninguna
     * Descripcion: traduce el registro a su celda.
     */
    private static CampoBCP campoDe(RegistroID id) {
        switch (id) {
            case AX:
                return CampoBCP.AX;
            case BX:
                return CampoBCP.BX;
            case CX:
                return CampoBCP.CX;
            default:
                return CampoBCP.DX;
        }
    }
}
