package com.mycompany.minipc.so.procesos;

import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.isa.RegistroID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la tecnica de calculo del kernel y del BCP guardado en memoria.
 */
class TablaBCPTest {

    private Memoria memoria;
    private TablaBCP tabla;

    @BeforeEach
    void preparar() {
        memoria = new Memoria(256, TablaBCP.TAMANO_KERNEL);
        tabla = new TablaBCP(memoria);
    }

    @Test
    @DisplayName("K = C + P x B = 3 + 5 x 25 = 128")
    void formulaDelKernel() {
        assertEquals(3, TablaBCP.TAMANO_CABECERA);
        assertEquals(25, TablaBCP.TAMANO_BCP);
        assertEquals(5, TablaBCP.MAX_PROCESOS);
        assertEquals(128, TablaBCP.TAMANO_KERNEL);
        assertEquals("128 celdas (3 de cabecera + 5 BCP x 25)", TablaBCP.describirFormula());
    }

    @Test
    @DisplayName("La direccion de cada BCP y de cada campo se calcula con base + desplazamiento")
    void direcciones() {
        assertEquals(3, TablaBCP.direccionBCP(0));
        assertEquals(28, TablaBCP.direccionBCP(1));
        assertEquals(103, TablaBCP.direccionBCP(4));
        assertEquals(28 + 4, TablaBCP.direccionCampo(28, CampoBCP.PC));
        assertThrows(IllegalArgumentException.class, () -> TablaBCP.direccionBCP(5));
    }

    @Test
    @DisplayName("De una direccion se obtienen la ranura y el campo")
    void direccionARanuraYCampo() {
        assertEquals(-1, TablaBCP.ranuraDe(2), "La cabecera no es de ninguna ranura");
        assertEquals(0, TablaBCP.ranuraDe(3));
        assertEquals(0, TablaBCP.ranuraDe(27));
        assertEquals(1, TablaBCP.ranuraDe(28));
        assertEquals(4, TablaBCP.ranuraDe(127));
        assertEquals(-1, TablaBCP.ranuraDe(128), "La zona de usuario no es de la tabla");
        assertEquals(CampoBCP.PC, TablaBCP.campoDe(32));
        assertEquals(CampoBCP.SIGUIENTE, TablaBCP.campoDe(127));
        assertNull(TablaBCP.campoDe(200));
    }

    @Test
    @DisplayName("Crear un proceso escribe su BCP en las celdas del kernel")
    void crearEscribeEnMemoria() {
        Proceso p = tabla.crear(7, "file.asm", 130, 5, 12);

        assertEquals(3, p.getDireccionBCP());
        assertEquals(27, p.getDireccionFinBCP());
        assertEquals("7", memoria.leer(3), "PID");
        assertEquals("file.asm", memoria.leer(4), "Programa");
        assertEquals("PREPARADO", memoria.leer(5), "Estado");
        assertEquals("130", memoria.leer(TablaBCP.direccionCampo(3, CampoBCP.PC)));
        assertEquals("130", memoria.leer(TablaBCP.direccionCampo(3, CampoBCP.BASE)));
        assertEquals("5", memoria.leer(TablaBCP.direccionCampo(3, CampoBCP.ALCANCE)));
        assertEquals("12", memoria.leer(TablaBCP.direccionCampo(3, CampoBCP.TIEMPO_INICIO)));
        assertEquals(1, tabla.getProcesosAdmitidos());
        assertEquals("1", memoria.leer(CampoCabecera.PROCESOS_ADMITIDOS.getDireccion()));
    }

    @Test
    @DisplayName("Proceso no copia datos: lee y escribe las celdas de su BCP")
    void procesoLeeDeMemoria() {
        Proceso p = tabla.crear(1, "a.asm", 130, 3, 0);

        memoria.escribirEntero(TablaBCP.direccionCampo(3, CampoBCP.PC), 999);
        assertEquals(999, p.getPc(), "El cambio en memoria se ve desde el proceso");

        p.setRegistro(RegistroID.CX, 42);
        assertEquals("42", memoria.leer(TablaBCP.direccionCampo(3, CampoBCP.CX)));

        p.setPila(List.of(4, 5));
        assertEquals("2", memoria.leer(TablaBCP.direccionCampo(3, CampoBCP.SP)));
        assertEquals("5", memoria.leer(TablaBCP.direccionCampo(3, CampoBCP.PILA_2)));
        assertEquals(List.of(4, 5), p.getPila());

        p.setZf(true);
        assertTrue(p.getZf());
        assertEquals(p, tabla.getProcesos().get(0), "Mismo BCP, mismo proceso");
    }

    @Test
    @DisplayName("La tabla nombra cada celda del kernel")
    void describeLasCeldas() {
        tabla.crear(1, "a.asm", 130, 3, 0);
        tabla.crear(2, "b.asm", 133, 3, 0);

        assertEquals("SO.Admitidos", tabla.describir(2));
        assertEquals("P1.PID", tabla.describir(3));
        assertEquals("P2.PC", tabla.describir(32));
        assertEquals("BCP libre", tabla.describir(60));
    }

    @Test
    @DisplayName("Solo caben 5 BCP; al liberar uno, su ranura se reutiliza")
    void cincoRanuras() {
        Proceso[] procesos = new Proceso[5];
        for (int i = 0; i < 5; i++) {
            procesos[i] = tabla.crear(i + 1, "p.asm", 130 + i, 1, 0);
        }
        assertFalse(tabla.hayRanuraLibre());
        assertThrows(IllegalStateException.class, () -> tabla.crear(6, "p.asm", 140, 1, 0));

        tabla.liberar(procesos[2]);
        assertEquals(4, tabla.getProcesosAdmitidos());
        assertTrue(memoria.estaLibre(TablaBCP.direccionBCP(2)), "El PID queda vacio");
        assertEquals(TablaBCP.direccionBCP(2), tabla.crear(6, "p.asm", 140, 1, 0)
                .getDireccionBCP());
    }

    @Test
    @DisplayName("El puntero de la cabecera dice que proceso tiene la CPU")
    void enEjecucion() {
        Proceso p = tabla.crear(1, "a.asm", 130, 3, 0);
        assertNull(tabla.getEnEjecucion());

        tabla.setEnEjecucion(p);
        assertEquals("3", memoria.leer(CampoCabecera.EN_EJECUCION.getDireccion()));
        assertEquals(p, tabla.getEnEjecucion());

        tabla.liberar(p);
        assertNull(tabla.getEnEjecucion(), "Liberar el BCP deja la CPU libre");
    }

    @Test
    @DisplayName("La memoria debe tener al menos el kernel calculado")
    void memoriaChica() {
        assertThrows(IllegalArgumentException.class,
                () -> new TablaBCP(new Memoria(256, 64)));
    }
}
