package com.mycompany.minipc.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.mycompany.minipc.excepciones.ConfiguracionException;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.so.planificacion.FabricaAlgoritmos;
import com.mycompany.minipc.so.procesos.TablaBCP;

/**
 * Nombre: LectorConfiguracion
 * Entradas: la ruta del archivo de configuracion
 * Salidas: la configuracion leida del archivo
 * Restricciones: el archivo es de texto en formato clave=valor (properties);
 *                las lineas que empiezan con # son comentarios
 * Descripcion: lee y escribe el archivo externo con los parametros de la
 *              minicomputadora, para que la configuracion no quede en el
 *              codigo. Si el archivo no existe, lo crea con los valores por
 *              defecto para que el usuario tenga un punto de partida que
 *              editar. Una clave ausente toma su valor por defecto; un valor
 *              que no es numero o que queda fuera de rango se reporta, y se
 *              reportan todos juntos.
 */
public class LectorConfiguracion {

    /** Nombre del archivo, que se busca en la carpeta desde donde se ejecuta. */
    public static final String NOMBRE_ARCHIVO = "config.properties";

    /** Clave del tamano de la memoria principal. */
    public static final String CLAVE_MEMORIA = "memoria.tamano";

    /** Clave del tamano del disco. */
    public static final String CLAVE_DISCO = "disco.tamano";

    /** Clave del tamano de la memoria virtual dentro del disco. */
    public static final String CLAVE_MEMORIA_VIRTUAL = "disco.memoriaVirtual";

    /** Clave de la duracion real de cada segundo de CPU en automatico. */
    public static final String CLAVE_MS_POR_SEGUNDO = "ejecucion.msPorSegundo";

    /** Clave del algoritmo de planificacion de procesos. */
    public static final String CLAVE_ALGORITMO = "planificacion.algoritmo";

    private final Path archivo;

    /**
     * Nombre: LectorConfiguracion
     * Entradas: ninguna
     * Salidas: el lector construido
     * Restricciones: ninguna
     * Descripcion: usa config.properties en la carpeta de trabajo, que es
     *              donde queda junto al jar o en la raiz del proyecto de
     *              NetBeans.
     */
    public LectorConfiguracion() {
        this(Paths.get(NOMBRE_ARCHIVO));
    }

    /**
     * Nombre: LectorConfiguracion
     * Entradas: archivo, ruta del archivo de configuracion
     * Salidas: el lector construido
     * Restricciones: la ruta no debe ser nula
     * Descripcion: permite usar otra ruta, lo que aprovechan las pruebas para
     *              trabajar en una carpeta temporal.
     */
    public LectorConfiguracion(Path archivo) {
        this.archivo = archivo;
    }

    /**
     * Nombre: getArchivo
     * Entradas: ninguna
     * Salidas: la ruta del archivo de configuracion
     * Restricciones: ninguna
     * Descripcion: la interfaz la muestra para que el usuario sepa que archivo
     *              editar.
     */
    public Path getArchivo() {
        return archivo;
    }

    /**
     * Nombre: cargar
     * Entradas: ninguna
     * Salidas: la configuracion leida
     * Restricciones: lanza IOException si el archivo no se puede leer o crear,
     *                y ConfiguracionException con todos los problemas si algun
     *                valor es invalido
     * Descripcion: si el archivo no existe, lo crea con los valores por
     *              defecto y los devuelve. Si existe, lo lee y valida.
     */
    public Configuracion cargar() throws IOException, ConfiguracionException {
        if (!Files.exists(archivo)) {
            Configuracion porDefecto = Configuracion.porDefecto();
            guardar(porDefecto);
            return porDefecto;
        }

        Properties propiedades = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        }

        Configuracion porDefecto = Configuracion.porDefecto();
        List<String> errores = new ArrayList<>();
        int memoria = leerEntero(propiedades, CLAVE_MEMORIA,
                porDefecto.getTamanoMemoria(), errores);
        int disco = leerEntero(propiedades, CLAVE_DISCO,
                porDefecto.getTamanoDisco(), errores);
        int memoriaVirtual = leerEntero(propiedades, CLAVE_MEMORIA_VIRTUAL,
                porDefecto.getTamanoMemoriaVirtual(), errores);
        int msPorSegundo = leerEntero(propiedades, CLAVE_MS_POR_SEGUNDO,
                porDefecto.getMsPorSegundo(), errores);
        String algoritmo = propiedades.getProperty(CLAVE_ALGORITMO, porDefecto.getAlgoritmo());

        // Los valores que no son numeros se reportan antes de revisar rangos,
        // porque sin un numero no tiene sentido decir si esta fuera de rango.
        if (!errores.isEmpty()) {
            throw new ConfiguracionException(prefijarArchivo(errores));
        }
        try {
            return new Configuracion(memoria, disco, memoriaVirtual, msPorSegundo, algoritmo);
        } catch (ConfiguracionException e) {
            throw new ConfiguracionException(prefijarArchivo(e.getErrores()));
        }
    }

    /**
     * Nombre: guardar
     * Entradas: configuracion, valores a escribir
     * Salidas: ninguna
     * Restricciones: lanza IOException si el archivo no se puede escribir;
     *                reemplaza el contenido anterior
     * Descripcion: escribe el archivo a mano, con un comentario por clave, en
     *              lugar de usar Properties.store, que no admite comentarios
     *              por clave y agrega una fecha en cada guardado.
     */
    public void guardar(Configuracion configuracion) throws IOException {
        List<String> lineas = List.of(
                "# Configuracion de la minicomputadora",
                "# Se puede editar a mano; los cambios se aplican al reiniciar el programa.",
                "",
                "# Memoria principal: posiciones totales. El kernel no se configura: ocupa "
                        + TablaBCP.describirFormula() + ", y a los programas les deben"
                        + " quedar al menos " + Memoria.ESPACIO_USUARIO_MINIMO,
                CLAVE_MEMORIA + "=" + configuracion.getTamanoMemoria(),
                "",
                "# Disco: posiciones totales (minimo 64); las primeras 20 son el indice",
                CLAVE_DISCO + "=" + configuracion.getTamanoDisco(),
                "# Posiciones al final del disco reservadas para memoria virtual",
                CLAVE_MEMORIA_VIRTUAL + "=" + configuracion.getTamanoMemoriaVirtual(),
                "",
                "# Milisegundos reales por cada segundo de CPU en la ejecucion automatica"
                        + " (" + Configuracion.MS_POR_SEGUNDO_MINIMO + " a "
                        + Configuracion.MS_POR_SEGUNDO_MAXIMO + ")",
                CLAVE_MS_POR_SEGUNDO + "=" + configuracion.getMsPorSegundo(),
                "",
                "# Algoritmo de planificacion de procesos. Disponibles: "
                        + String.join(", ", FabricaAlgoritmos.disponibles()),
                CLAVE_ALGORITMO + "=" + configuracion.getAlgoritmo());
        Path carpeta = archivo.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        Files.write(archivo, lineas, StandardCharsets.UTF_8);
    }

    /**
     * Nombre: leerEntero
     * Entradas: propiedades, contenido del archivo; clave, nombre del valor;
     *           porDefecto, valor a usar si la clave no esta; errores, lista
     *           donde se anota el problema si el valor no es un numero
     * Salidas: el valor leido, o el valor por defecto
     * Restricciones: no lanza excepciones; los problemas quedan en la lista
     * Descripcion: lee un entero tolerando espacios alrededor del valor.
     */
    private int leerEntero(Properties propiedades, String clave, int porDefecto,
            List<String> errores) {
        String texto = propiedades.getProperty(clave);
        if (texto == null || texto.isBlank()) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            errores.add(clave + ": \"" + texto.trim() + "\" no es un numero entero");
            return porDefecto;
        }
    }

    /**
     * Nombre: prefijarArchivo
     * Entradas: errores, mensajes a reportar
     * Salidas: los mismos mensajes precedidos por una linea con la ruta
     * Restricciones: ninguna
     * Descripcion: indica al usuario en que archivo debe hacer la correccion.
     */
    private List<String> prefijarArchivo(List<String> errores) {
        List<String> conArchivo = new ArrayList<>();
        conArchivo.add("Hay valores invalidos en " + archivo.toAbsolutePath() + ":");
        conArchivo.addAll(errores);
        return conArchivo;
    }
}
