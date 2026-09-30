package com.mycompany.minipc.so.despacho;

import java.util.List;

import com.mycompany.minipc.hardware.Pila;
import com.mycompany.minipc.hardware.Procesador;
import com.mycompany.minipc.isa.RegistroID;
import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: CambioContexto
 * Entradas: la CPU y el proceso cuyo BCP se lee o se escribe
 * Salidas: no aplica
 * Restricciones: no decide nada: solo copia registros entre la CPU y el BCP
 * Descripcion: el "cambio de contexto" del enunciado. El contexto de un
 *              proceso es lo que la CPU necesita para continuarlo donde quedo:
 *              PC, IR, AC, AX a DX, la bandera ZF y la pila (Stallings,
 *              figuras 1.10 y 1.11). Guardar copia esos registros de la CPU a
 *              las celdas del BCP en memoria; restaurar hace lo contrario y
 *              ademas carga los registros base y alcance del proceso.
 */
public class CambioContexto {

    /**
     * Nombre: guardar
     * Entradas: cpu, procesador; proceso, dueno del contexto
     * Salidas: ninguna
     * Restricciones: la CPU debe tener cargado el contexto de ese proceso
     * Descripcion: CPU -> BCP.
     */
    public void guardar(Procesador cpu, Proceso proceso) {
        proceso.setPc(cpu.getPc());
        proceso.setIr(cpu.getIrTexto());
        proceso.setAc(cpu.getAc());
        for (RegistroID id : RegistroID.values()) {
            proceso.setRegistro(id, cpu.getRegistros().leer(id));
        }
        proceso.setZf(cpu.getZf());
        proceso.setPila(cpu.getPila().getValores());
    }

    /**
     * Nombre: restaurar
     * Entradas: proceso, dueno del contexto; cpu, procesador
     * Salidas: ninguna
     * Restricciones: borra lo que la CPU tuviera cargado
     * Descripcion: BCP -> CPU, incluidos los registros base y alcance que
     *              protegen la region del proceso.
     */
    public void restaurar(Proceso proceso, Procesador cpu) {
        cpu.limpiar();
        cpu.cargarLimites(proceso.getBase(), proceso.getAlcance());
        cpu.setPc(proceso.getPc());
        cpu.setIrTexto(proceso.getIr());
        cpu.setAc(proceso.getAc());
        for (RegistroID id : RegistroID.values()) {
            cpu.getRegistros().escribir(id, proceso.getRegistro(id));
        }
        cpu.setZf(proceso.getZf());
        Pila pila = cpu.getPila();
        List<Integer> valores = proceso.getPila();
        for (int valor : valores) {
            pila.apilar(valor);
        }
    }
}
