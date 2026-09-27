package com.mycompany.minipc.config;

import com.mycompany.minipc.excepciones.ConfiguracionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la configuracion externa: valores por defecto, creacion del
 * archivo, lectura y reporte de errores.
 */
class ConfiguracionTest {

    private static Path escribir(Path carpeta, String contenido) throws Exception {
        Path archivo = carpeta.resolve("config.properties");
        Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
        return archivo;
    }

    @Test
    @DisplayName("Los valores por defecto son los del enunciado")
    void valoresPorDefecto() {
        Configuracion c = Configuracion.porDefecto();
        assertEquals(256, c.getTamanoMemoria());
        assertEquals(64, c.getLimiteKernel());
        assertEquals(512, c.getTamanoDisco());
        assertEquals(64, c.getTamanoMemoriaVirtual());
        assertEquals(1000, c.getMsPorSegundo());
    }

    @Test
    @DisplayName("Si el archivo no existe se crea con los valores por defecto")
    void creaElArchivoSiNoExiste(@TempDir Path carpeta) throws Exception {
        Path archivo = carpeta.resolve("config.properties");
        LectorConfiguracion lector = new LectorConfiguracion(archivo);

        Configuracion c = lector.cargar();

        assertTrue(Files.exists(archivo));
        assertEquals(256, c.getTamanoMemoria());
        String texto = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(texto.contains("memoria.tamano=256"), texto);
        assertTrue(texto.contains("disco.memoriaVirtual=64"), texto);
    }

    @Test
    @DisplayName("Lo que se guarda se vuelve a leer igual")
    void guardarYCargar(@TempDir Path carpeta) throws Exception {
        LectorConfiguracion lector = new LectorConfiguracion(carpeta.resolve("config.properties"));
        lector.guardar(new Configuracion(512, 128, 1024, 128, 250));

        Configuracion c = lector.cargar();

        assertEquals(512, c.getTamanoMemoria());
        assertEquals(128, c.getLimiteKernel());
        assertEquals(1024, c.getTamanoDisco());
        assertEquals(128, c.getTamanoMemoriaVirtual());
        assertEquals(250, c.getMsPorSegundo());
    }

    @Test
    @DisplayName("Las claves ausentes toman el valor por defecto y se ignoran comentarios")
    void clavesAusentes(@TempDir Path carpeta) throws Exception {
        Path archivo = escribir(carpeta, "# solo cambio la memoria\nmemoria.tamano = 300\n");

        Configuracion c = new LectorConfiguracion(archivo).cargar();

        assertEquals(300, c.getTamanoMemoria());
        assertEquals(64, c.getLimiteKernel());
        assertEquals(512, c.getTamanoDisco());
    }

    @Test
    @DisplayName("Los valores no numericos se reportan todos juntos")
    void reportaValoresNoNumericos(@TempDir Path carpeta) throws Exception {
        Path archivo = escribir(carpeta, "memoria.tamano=mucho\ndisco.tamano=12x\n");

        ConfiguracionException e = assertThrows(ConfiguracionException.class,
                () -> new LectorConfiguracion(archivo).cargar());

        assertEquals(3, e.getErrores().size(), "Una linea con el archivo y dos errores");
        assertTrue(e.getErrores().get(0).contains("config.properties"));
        assertTrue(e.getErrores().get(1).contains("memoria.tamano"));
        assertTrue(e.getErrores().get(2).contains("disco.tamano"));
    }

    @Test
    @DisplayName("Los valores fuera de rango se reportan con las reglas de memoria y disco")
    void reportaValoresFueraDeRango(@TempDir Path carpeta) throws Exception {
        Path archivo = escribir(carpeta,
                "memoria.tamano=100\ndisco.tamano=512\ndisco.memoriaVirtual=600\n"
                + "ejecucion.msPorSegundo=10\n");

        ConfiguracionException e = assertThrows(ConfiguracionException.class,
                () -> new LectorConfiguracion(archivo).cargar());

        String mensaje = e.getMessage();
        assertTrue(mensaje.contains("al menos 128"), mensaje);
        assertTrue(mensaje.contains("memoria virtual"), mensaje);
        assertTrue(mensaje.contains("50 y 2000"), mensaje);
    }

    @Test
    @DisplayName("El constructor rechaza un kernel que no deja zona de usuario")
    void rechazaKernelMayorQueMemoria() {
        assertThrows(ConfiguracionException.class,
                () -> new Configuracion(256, 256, 512, 64, 1000));
    }
}
