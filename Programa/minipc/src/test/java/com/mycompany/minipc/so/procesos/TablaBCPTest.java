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
        memoria = new Memoria(256, TablaBCP.calcularKernel(256,
                TablaBCP.PORCENTAJE_KERNEL_POR_DEFECTO));
        tabla = new TablaBCP(memoria);
    }

    @Test
    @DisplayName("El kernel es un porcentaje de la memoria y de el sale cuantos BCP caben")
    void calculoDelKernel() {
        assertEquals(3, TablaBCP.TAMANO_CABECERA);
        assertEquals(25, TablaBCP.TAMANO_BCP);
        assertEquals(5, TablaBCP.MAX_PROCESOS);
        assertEquals(50, TablaBCP.PORCENTAJE_KERNEL_POR_DEFECTO);

        assertEquals(128, TablaBCP.calcularKernel(256, 50));
        assertEquals(5, TablaBCP.ranurasPara(128), "Con 50 % caben los 5 BCP");
        assertEquals(51, TablaBCP.calcularKernel(256, 20));
        assertEquals(1, TablaBCP.ranurasPara(51), "Con 20 % cabe uno solo");
        assertEquals(5, TablaBCP.ranurasPara(TablaBCP.calcularKernel(256, 60)),
                "Con mas kernel siguen siendo 5 como maximo");
        assertEquals(0, TablaBCP.ranurasPara(27));

        assertEquals("256 x 50 % = 128 celdas: 3 de cabecera + 5 BCP x 25",
                TablaBCP.describirCalculo(256, 50));
        assertEquals("256 x 60 % = 153 celdas: 3 de cabecera + 5 BCP x 25 + 25 sin usar",
                TablaBCP.describirCalculo(256, 60));
    }

    @Test
    @DisplayName("El porcentaje debe estar entre 10 y 90 y dejar lugar para un BCP")
    void validarPorcentaje() {
        assertTrue(TablaBCP.validarKernel(256, 50).isEmpty());
        assertTrue(TablaBCP.validarKernel(256, 20).isEmpty());
        assertTrue(TablaBCP.validarKernel(256, 5).get(0).contains("entre 10 y 90"));
        assertTrue(TablaBCP.validarKernel(256, 95).get(0).contains("entre 10 y 90"));
        assertTrue(TablaBCP.validarKernel(100, 20).get(0).contains("no cabe ningun BCP"));
    }

    @Test
    @DisplayName("Con 20 % de kernel cabe un solo BCP y el resto del kernel queda sin usar")
    void kernelChico() {
        Memoria chica = new Memoria(256, TablaBCP.calcularKernel(256, 20));
        TablaBCP pequena = new TablaBCP(chica);
        assertEquals(1, pequena.getRanuras());
        pequena.crear(1, "a.asm", 100, 5, 0);
        assertFalse(pequena.hayRanuraLibre(), "Solo se admite un proceso a la vez");
        assertEquals("P1.PC", pequena.describir(TablaBCP.direccionCampo(3, CampoBCP.PC)));
        assertEquals("Kernel sin usar", pequena.describir(30));
        assertEquals("", pequena.describir(51), "51 ya es zona de usuario");
        assertThrows(IllegalArgumentException.class,
                () -> new TablaBCP(new Memoria(256, 20)), "Con 20 celdas no cabe un BCP");
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
    @DisplayName("En el kernel debe caber al menos un BCP")
    void memoriaChica() {
        assertThrows(IllegalArgumentException.class,
                () -> new TablaBCP(new Memoria(256, 27)));
    }
}
