package com.mycompany.minipc.gui.modelo;

import com.mycompany.minipc.so.SistemaOperativo;
import com.mycompany.minipc.so.planificacion.FCFS;
import com.mycompany.minipc.so.trabajos.Trabajo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la tabla de estadisticas: inicio, fin y duracion de cada proceso
 * con el reloj simulado.
 */
class ModeloTablaDuracionesTest {

    @Test
    @DisplayName("Cada proceso muestra inicio, fin, duracion, CPU y espera con el reloj simulado")
    void duraciones() throws Exception {
        SistemaOperativo so = new SistemaOperativo(256, 512, 64, new FCFS());
        so.getDisco().guardarPrograma("a.asm", List.of("MOV AX, 1", "ADD AX", "INT 20H"));
        so.getDisco().guardarPrograma("b.asm", List.of("POP AX", "INT 20H"));
        so.agregarTrabajo("a.asm");
        so.agregarTrabajo("b.asm");
        while (so.tick()) {
            // cada tick es un segundo
        }

        List<Trabajo> trabajos = so.getListaTrabajos().getTrabajos();
        ModeloTablaDuraciones modelo = new ModeloTablaDuraciones(trabajos);
        assertEquals(2, modelo.getRowCount());
        assertEquals("Proceso", modelo.getColumnName(0));

        // A: MOV 1 + ADD 3 + INT 20H 2 = 6 s, sin esperar.
        assertEquals("P1", modelo.getValueAt(0, 0));
        assertEquals("00:00:00", modelo.getValueAt(0, 2));
        assertEquals("00:00:06", modelo.getValueAt(0, 3));
        assertEquals(6, modelo.getValueAt(0, 4));
        assertEquals(6, modelo.getValueAt(0, 5));
        assertEquals(0, modelo.getValueAt(0, 6));
        assertEquals("Correcto", modelo.getValueAt(0, 7));

        // B espera los 6 s de A y falla en POP (peso 1).
        assertEquals("00:00:07", modelo.getValueAt(1, 3));
        assertEquals(7, modelo.getValueAt(1, 4));
        assertEquals(1, modelo.getValueAt(1, 5));
        assertEquals(6, modelo.getValueAt(1, 6));
        assertTrue(String.valueOf(modelo.getValueAt(1, 7)).startsWith("Error: "));
    }

    @Test
    @DisplayName("Un trabajo sin admitir no tiene tiempos y uno admitido esta en curso")
    void sinTerminar() throws Exception {
        SistemaOperativo so = new SistemaOperativo(256, 512, 64, new FCFS());
        so.getDisco().guardarPrograma("a.asm", List.of("MOV AX, 1", "INT 20H"));
        so.agregarTrabajo("a.asm");
        ModeloTablaDuraciones nuevo = new ModeloTablaDuraciones(
                so.getListaTrabajos().getTrabajos());
        assertEquals("-", nuevo.getValueAt(0, 2));
        assertEquals("-", nuevo.getValueAt(0, 4));
        assertEquals("NUEVO", nuevo.getValueAt(0, 7));

        so.tick();
        ModeloTablaDuraciones enCurso = new ModeloTablaDuraciones(
                so.getListaTrabajos().getTrabajos());
        assertEquals("en curso", enCurso.getValueAt(0, 4));
        assertEquals(1, enCurso.getValueAt(0, 5), "El tiempo de CPU se lee del BCP");
    }
}
