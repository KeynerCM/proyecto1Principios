package com.mycompany.minipc.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Timer;

import com.mycompany.minipc.config.Configuracion;
import com.mycompany.minipc.config.LectorConfiguracion;
import com.mycompany.minipc.core.Disco;
import com.mycompany.minipc.core.EntradaIndice;
import com.mycompany.minipc.core.Estadisticas;
import com.mycompany.minipc.core.Fase;
import com.mycompany.minipc.core.ObservadorCPU;
import com.mycompany.minipc.core.Procesador;
import com.mycompany.minipc.excepciones.ConfiguracionException;
import com.mycompany.minipc.excepciones.DesbordamientoException;
import com.mycompany.minipc.excepciones.DiscoException;
import com.mycompany.minipc.excepciones.MemoriaInsuficienteException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.gui.modelo.ModeloTablaDisco;
import com.mycompany.minipc.gui.modelo.ModeloTablaInstrucciones;
import com.mycompany.minipc.gui.modelo.ModeloTablaMemoria;
import com.mycompany.minipc.gui.modelo.RenderInstruccionActual;
import com.mycompany.minipc.gui.modelo.RenderZonaDisco;
import com.mycompany.minipc.gui.modelo.RenderZonaMemoria;
import com.mycompany.minipc.io.CargadorASM;
import com.mycompany.minipc.isa.Ensamblador;
import com.mycompany.minipc.isa.Instruccion;

/**
 * Nombre: ControladorPrincipal
 * Entradas: la vista a la que da servicio y las acciones que el usuario pulsa
 * Salidas: las actualizaciones que envia a la vista
 * Restricciones: es lo unico que conoce a los dos lados; el nucleo nunca sabe
 *                que existe Swing y la vista nunca conoce al procesador
 * Descripcion: coordina la ventana con el procesador. Recibe lo que el usuario
 *              pulsa, se lo pide al nucleo, y refresca la vista cuando el
 *              nucleo avisa que algo cambio. Implementa ObservadorCPU para
 *              enterarse de cada etapa del ciclo de instruccion.
 */
public class ControladorPrincipal implements ObservadorCPU {

    private final VistaPrincipal vista;
    private final Procesador cpu;
    private final Disco disco;
    private final CargadorASM cargador;
    private final Ensamblador ensamblador;

    /** Lector del archivo de configuracion, o nulo si no se usa archivo. */
    private final LectorConfiguracion lectorConfiguracion;

    /** Configuracion aplicada en este momento. */
    private Configuracion configuracion;

    /** Problemas al leer la configuracion, que se informan al mostrar la vista. */
    private final List<String> erroresConfiguracion;

    private final ModeloTablaInstrucciones modeloInstrucciones;
    private final ModeloTablaMemoria modeloMemoria;
    private final ModeloTablaDisco modeloDisco;
    private final RenderInstruccionActual renderInstrucciones;
    private final RenderZonaMemoria renderMemoria;
    private final RenderZonaDisco renderDisco;

    /**
     * Temporizador de la ejecucion automatica.
     *
     * Se usa un javax.swing.Timer y no un bucle porque sus disparos ocurren
     * en el hilo de despacho de eventos. Un while llamando a paso() dentro
     * de ese hilo congelaria la ventana hasta terminar, y no se veria nada
     * de la ejecucion, que es justo lo que hay que mostrar.
     */
    private final Timer temporizador;

    private String nombreArchivo;

    /**
     * Nombre: ControladorPrincipal
     * Entradas: vista, ventana a la que este controlador da servicio
     * Salidas: el controlador construido con la configuracion por defecto
     * Restricciones: la vista no debe ser nula; no lee ni escribe ningun
     *                archivo de configuracion
     * Descripcion: variante sin archivo, pensada para las pruebas, que no
     *              deben depender de lo que haya en la carpeta de trabajo.
     */
    public ControladorPrincipal(VistaPrincipal vista) {
        this(vista, null);
    }

    /**
     * Nombre: ControladorPrincipal
     * Entradas: vista, ventana a la que este controlador da servicio;
     *           lectorConfiguracion, lector del archivo de configuracion, o
     *           nulo para usar los valores por defecto sin archivo
     * Salidas: el controlador construido
     * Restricciones: la vista no debe ser nula; el controlador queda ya
     *                registrado como observador del procesador. Si el archivo
     *                tiene errores no falla: usa los valores por defecto y
     *                guarda los problemas para informarlos en
     *                inicializarVista, cuando la ventana ya existe
     * Descripcion: lee la configuracion, crea el procesador y el disco con
     *              esos tamanos, los modelos de tabla, los renderers y el
     *              temporizador. Los renderers se crean aqui y no en la
     *              ventana porque necesitan consultar la memoria y el disco.
     */
    public ControladorPrincipal(VistaPrincipal vista, LectorConfiguracion lectorConfiguracion) {
        this.vista = vista;
        this.lectorConfiguracion = lectorConfiguracion;
        this.erroresConfiguracion = new ArrayList<>();
        this.configuracion = leerConfiguracion();

        this.cpu = new Procesador();
        this.cpu.configurarMemoria(configuracion.getTamanoMemoria(),
                configuracion.getLimiteKernel());
        this.disco = new Disco(configuracion.getTamanoDisco(),
                configuracion.getTamanoMemoriaVirtual());
        this.cargador = new CargadorASM();
        this.ensamblador = new Ensamblador();
        this.nombreArchivo = "(ninguno)";

        this.modeloInstrucciones = new ModeloTablaInstrucciones();
        this.modeloMemoria = new ModeloTablaMemoria(cpu.getMemoria());
        this.modeloDisco = new ModeloTablaDisco(disco);
        this.renderInstrucciones = new RenderInstruccionActual();
        this.renderMemoria = new RenderZonaMemoria(cpu.getMemoria());
        this.renderDisco = new RenderZonaDisco(disco);

        this.temporizador = new Timer(configuracion.getMsPorSegundo(),
                e -> alTicDelTemporizador());
        this.cpu.agregarObservador(this);
    }

    /**
     * Nombre: leerConfiguracion
     * Entradas: ninguna; usa el lector recibido en el constructor
     * Salidas: la configuracion a aplicar
     * Restricciones: nunca lanza excepciones; ante cualquier problema devuelve
     *                los valores por defecto y anota el motivo
     * Descripcion: si no hay lector, usa los valores por defecto. Si el
     *              archivo tiene valores invalidos o no se puede leer, tambien,
     *              para que el programa arranque igual y el usuario pueda
     *              corregir el archivo o la configuracion desde el dialogo.
     */
    private Configuracion leerConfiguracion() {
        if (lectorConfiguracion == null) {
            return Configuracion.porDefecto();
        }
        try {
            return lectorConfiguracion.cargar();
        } catch (ConfiguracionException e) {
            erroresConfiguracion.addAll(e.getErrores());
        } catch (IOException e) {
            erroresConfiguracion.add("No se pudo leer "
                    + lectorConfiguracion.getArchivo().toAbsolutePath() + ": " + e.getMessage());
        }
        erroresConfiguracion.add("Se usan los valores por defecto.");
        return Configuracion.porDefecto();
    }

    // Acciones de los botones

    /**
     * Nombre: alCargarArchivos
     * Entradas: ninguna; los archivos los pide a la vista
     * Salidas: ninguna; deja los programas validos guardados en el disco y
     *          la vista actualizada
     * Restricciones: si el usuario cancela el dialogo no ocurre nada. Un
     *                archivo con problemas no se guarda, pero no impide que
     *                se guarden los demas
     * Descripcion: carga uno o varios archivos .asm. Por cada uno encadena
     *              leerlo, ensamblarlo y guardarlo en el disco, que es donde
     *              viven los programas antes de ejecutarse. Los problemas de
     *              todos los archivos se juntan en un solo cuadro, cada uno
     *              con el nombre de su archivo, para que el usuario los
     *              corrija en una pasada. Despues pasa el primer programa
     *              guardado del disco a la memoria para poder ejecutarlo.
     */
    public void alCargarArchivos() {
        List<File> archivos = vista.seleccionarArchivosAsm();
        if (archivos == null || archivos.isEmpty()) {
            return;
        }

        List<String> errores = new ArrayList<>();
        List<EntradaIndice> guardados = new ArrayList<>();
        for (File archivo : archivos) {
            EntradaIndice entrada = guardarEnDisco(archivo, errores);
            if (entrada != null) {
                guardados.add(entrada);
            }
        }
        vista.refrescarDisco();

        if (!guardados.isEmpty()) {
            cargarEnMemoria(guardados.get(0), errores);
            if (guardados.size() > 1) {
                vista.escribirEnConsola((guardados.size() - 1)
                        + " programa(s) mas quedan guardados en el disco.");
            }
        }
        if (!errores.isEmpty()) {
            vista.mostrarErrores("Errores al cargar archivos", errores);
        }
        actualizarVista();
    }

    /**
     * Nombre: guardarEnDisco
     * Entradas: archivo, archivo .asm elegido; errores, lista donde anotar
     *           los problemas encontrados
     * Salidas: la entrada del indice creada, o nulo si el archivo no se guardo
     * Restricciones: no lanza excepciones; cada fallo queda en la lista con el
     *                nombre del archivo
     * Descripcion: valida el archivo en el orden en que puede fallar: que se
     *              pueda leer y tenga extension .asm, que su sintaxis sea
     *              correcta y que haya lugar en el disco. Si ya hay un archivo
     *              con el mismo nombre, guarda una copia numerada.
     */
    private EntradaIndice guardarEnDisco(File archivo, List<String> errores) {
        String nombre = archivo.getName();
        try {
            List<Instruccion> programa = ensamblador.ensamblar(cargador.leer(archivo));
            EntradaIndice entrada = disco.guardarPrograma(disco.nombreDisponible(nombre),
                    programa);
            vista.escribirEnConsola("Guardado en disco: " + entrada.getNombre()
                    + ", posiciones " + entrada.getDireccionInicio() + " a "
                    + entrada.getDireccionFin() + ".");
            return entrada;

        } catch (SintaxisException e) {
            errores.add(nombre + ": " + e.cantidad() + " error(es) de sintaxis");
            for (String error : e.getErrores()) {
                errores.add("    " + error);
            }
            vista.escribirEnConsola("El archivo " + nombre + " tiene " + e.cantidad()
                    + " error(es) de sintaxis. No se guardo.");

        } catch (DiscoException e) {
            errores.add(nombre + ": " + e.getMessage());
            vista.escribirEnConsola(e.getMessage());

        } catch (IOException | IllegalArgumentException e) {
            errores.add(nombre + ": " + e.getMessage());
            vista.escribirEnConsola("Error al leer " + nombre + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Nombre: cargarEnMemoria
     * Entradas: entrada, archivo del disco a cargar; errores, lista donde
     *           anotar el problema si no cabe
     * Salidas: ninguna
     * Restricciones: si el programa no cabe, queda en el disco y la memoria no
     *                cambia
     * Descripcion: lee el programa del disco y lo carga en la memoria
     *              principal. Mientras no exista el planificador de trabajos,
     *              esta es la forma de pasar un programa del disco a la
     *              memoria.
     */
    private void cargarEnMemoria(EntradaIndice entrada, List<String> errores) {
        try {
            List<Instruccion> programa = disco.leerPrograma(entrada.getNombre());
            cpu.cargar(programa, entrada.getNombre());

            nombreArchivo = entrada.getNombre();
            modeloInstrucciones.cargar(programa);
            vista.mostrarInstrucciones(programa);
            vista.escribirEnConsola("Programa " + entrada.getNombre()
                    + " cargado en memoria en la posicion " + cpu.getDireccionBase()
                    + ". " + programa.size() + " instrucciones.");

        } catch (MemoriaInsuficienteException e) {
            errores.add(entrada.getNombre() + ": se guardo en el disco, pero no cabe en la"
                    + " memoria. " + e.getMessage());
            vista.escribirEnConsola(e.getMessage());

        } catch (DiscoException e) {
            // El programa se acaba de guardar, asi que no puede faltar.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Nombre: alEjecutar
     * Entradas: ninguna
     * Salidas: ninguna; arranca el temporizador
     * Restricciones: no hace nada si no hay programa, si ya termino, o si la
     *                ejecucion automatica ya esta en marcha
     * Descripcion: arranca la ejecucion automatica hasta el final del programa.
     */
    public void alEjecutar() {
        if (!cpu.hayPrograma() || cpu.haTerminado() || temporizador.isRunning()) {
            return;
        }
        vista.escribirEnConsola("Ejecucion automatica iniciada.");
        temporizador.start();
        actualizarVista();
    }

    /**
     * Nombre: alPasoAPaso
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no hace nada si no hay programa, si ya termino, o si la
     *                ejecucion automatica esta en marcha
     * Descripcion: ejecuta una sola instruccion, que es el modo de ejecucion
     *              que el enunciado exige.
     */
    public void alPasoAPaso() {
        if (!cpu.hayPrograma() || cpu.haTerminado() || temporizador.isRunning()) {
            return;
        }
        ejecutarUnPaso();
    }

    /**
     * Nombre: alReiniciar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: detiene antes la ejecucion automatica si estaba corriendo
     * Descripcion: vuelve al inicio del programa sin descargarlo de memoria.
     */
    public void alReiniciar() {
        detener();
        if (!cpu.hayPrograma()) {
            return;
        }
        cpu.reset();
        vista.escribirEnConsola("Procesador reiniciado en la posicion "
                + cpu.getDireccionBase() + ".");
    }

    /**
     * Nombre: alLimpiar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: detiene antes la ejecucion automatica si estaba corriendo
     * Descripcion: descarga el programa, borra los archivos del disco y deja
     *              la memoria de usuario, los registros, las tablas y la
     *              consola en blanco.
     */
    public void alLimpiar() {
        detener();
        cpu.limpiar();
        disco.formatear();
        nombreArchivo = "(ninguno)";
        modeloInstrucciones.limpiar();
        vista.mostrarInstrucciones(Collections.emptyList());
        vista.refrescarDisco();
        vista.limpiarConsola();
        vista.escribirEnConsola("Memoria de usuario, disco, registros y tablas vaciados.");
        actualizarVista();
    }

    /**
     * Nombre: alConfigurar
     * Entradas: nueva, configuracion ya validada
     * Salidas: ninguna
     * Restricciones: descarga el programa actual y borra el disco, porque
     *                redimensionarlos invalida las direcciones ya asignadas.
     *                Si el archivo no se puede escribir, la configuracion se
     *                aplica igual y se informa el problema
     * Descripcion: aplica la configuracion a la memoria, al disco y al
     *              temporizador, y la guarda en el archivo de configuracion
     *              para que se conserve la proxima vez que se abra el programa.
     */
    public void alConfigurar(Configuracion nueva) {
        detener();
        cpu.configurarMemoria(nueva.getTamanoMemoria(), nueva.getLimiteKernel());
        disco.redimensionar(nueva.getTamanoDisco(), nueva.getTamanoMemoriaVirtual());
        temporizador.setDelay(nueva.getMsPorSegundo());
        configuracion = nueva;

        nombreArchivo = "(ninguno)";
        modeloInstrucciones.limpiar();
        vista.mostrarInstrucciones(Collections.emptyList());
        vista.refrescarDisco();
        vista.escribirEnConsola("Configuracion aplicada. " + describirConfiguracion());

        if (lectorConfiguracion != null) {
            try {
                lectorConfiguracion.guardar(nueva);
                vista.escribirEnConsola("Configuracion guardada en "
                        + lectorConfiguracion.getArchivo().toAbsolutePath() + ".");
            } catch (IOException e) {
                vista.mostrarErrores("No se pudo guardar la configuracion",
                        Collections.singletonList(e.getMessage()));
            }
        }
        actualizarVista();
    }

    /**
     * Nombre: describirConfiguracion
     * Entradas: ninguna
     * Salidas: la configuracion actual en una frase
     * Restricciones: ninguna
     * Descripcion: se usa en la consola al arrancar y al reconfigurar.
     */
    private String describirConfiguracion() {
        int limite = configuracion.getLimiteKernel();
        int tamano = configuracion.getTamanoMemoria();
        return "Memoria de " + tamano + " posiciones (kernel de 0 a " + (limite - 1)
                + ", usuario de " + limite + " a " + (tamano - 1) + "). Disco de "
                + disco.getTamano() + " posiciones (indice de 0 a "
                + (Disco.ENTRADAS_INDICE - 1) + ", archivos de " + disco.getInicioArchivos()
                + " a " + (disco.getInicioMemoriaVirtual() - 1) + ", memoria virtual: "
                + disco.getTamanoMemoriaVirtual() + "). Segundo de CPU: "
                + configuracion.getMsPorSegundo() + " ms.";
    }

    // ------------------------------------------------------------------
    // Ejecucion
    // ------------------------------------------------------------------

    /**
     * Nombre: alTicDelTemporizador
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: se ejecuta en el hilo de despacho de eventos
     * Descripcion: cada disparo del temporizador ejecuta una instruccion y, si
     *              ya no quedan, detiene la ejecucion automatica.
     */
    private void alTicDelTemporizador() {
        if (!ejecutarUnPaso()) {
            detener();
        }
    }

    /**
     * Nombre: ejecutarUnPaso
     * Entradas: ninguna
     * Salidas: true si queda alguna instruccion por ejecutar
     * Restricciones: atrapa DesbordamientoException, de modo que el error no
     *                se propaga hacia Swing
     * Descripcion: ejecuta una instruccion y atiende los dos finales posibles:
     *              que el programa termine normalmente o que se detenga por
     *              desbordamiento. En el segundo caso el procesador ya dejo el
     *              proceso en BLOQUEADO_ERROR y aviso a los observadores, asi
     *              que la pantalla ya refleja el estado y aqui solo falta
     *              informar al usuario.
     */
    private boolean ejecutarUnPaso() {
        try {
            boolean quedan = cpu.paso();
            if (!quedan) {
                vista.escribirEnConsola("Ejecucion terminada. "
                        + cpu.getInstruccionesEjecutadas() + " instrucciones ejecutadas.");
                actualizarVista();
            }
            return quedan;

        } catch (DesbordamientoException e) {
            // El procesador ya dejo el proceso en BLOQUEADO_ERROR y aviso a
            // los observadores, asi que la pantalla ya refleja el estado.
            detener();
            vista.escribirEnConsola("ERROR: " + e.getMessage());
            vista.mostrarErrores("Error de ejecucion",
                    Collections.singletonList(e.getMessage()));
            actualizarVista();
            return false;
        }
    }

    /**
     * Nombre: detener
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no hace nada si el temporizador no estaba corriendo
     * Descripcion: detiene la ejecucion automatica y refresca la vista. El
     *              refresco final no es opcional: mientras el temporizador
     *              corre, el procesador notifica a los observadores antes de
     *              que este metodo lo detenga, de modo que esas notificaciones
     *              ven todavia isRunning() en true y dejan los botones
     *              deshabilitados. Sin este ultimo refresco la ventana se
     *              queda bloqueada al terminar el programa.
     */
    private void detener() {
        if (temporizador.isRunning()) {
            temporizador.stop();
            actualizarVista();
        }
    }

    // ------------------------------------------------------------------
    // Observador del procesador
    // ------------------------------------------------------------------

    /**
     * Nombre: alCambiarEstado
     * Entradas: procesador, el que cambio de estado; fase, momento del ciclo
     * Salidas: ninguna
     * Restricciones: se invoca desde el hilo que ejecuta la instruccion
     * Descripcion: el procesador avisa que algo cambio y el controlador se
     *              limita a refrescar la vista completa. No distingue la fase
     *              porque el refresco es el mismo en todas.
     */
    @Override
    public void alCambiarEstado(Procesador procesador, Fase fase) {
        actualizarVista();
    }

    /**
     * Nombre: actualizarVista
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: vuelca el estado actual del procesador sobre la ventana:
     *              resaltado, tabla de memoria, panel del BCP, barra de
     *              contexto, ocupacion de memoria y estado de los botones.
     *              Antes actualiza los renderers, que necesitan saber que fila
     *              y que direccion destacar.
     */
    private void actualizarVista() {
        int indice = cpu.getIndiceInstruccionActual();
        renderInstrucciones.setFilaActual(indice);
        renderMemoria.setDireccionActual(cpu.hayPrograma() && indice >= 0
                ? cpu.getPc() : -1);

        vista.resaltarInstruccion(indice);
        vista.refrescarMemoria();
        vista.mostrarBCP(cpu.getBcp());
        vista.actualizarBarraContexto(nombreArchivo, textoDelEstado());
        vista.actualizarUsoMemoria(cpu.getMemoria().getPorcentajeUso());
        vista.actualizarBotones(cpu.hayPrograma(), temporizador.isRunning(),
                cpu.haTerminado());
    }

    /**
     * Nombre: textoDelEstado
     * Entradas: ninguna
     * Salidas: el estado del proceso en texto
     * Restricciones: ninguna
     * Descripcion: devuelve el nombre del estado, o la leyenda SIN PROGRAMA
     *              cuando no hay nada cargado, que no es un estado del proceso
     *              sino la ausencia de proceso.
     */
    private String textoDelEstado() {
        return cpu.hayPrograma() ? cpu.getEstado().name() : "SIN PROGRAMA";
    }

    // ------------------------------------------------------------------
    // Acceso para la ventana
    // ------------------------------------------------------------------

    /**
     * Nombre: getModeloInstrucciones
     * Entradas: ninguna
     * Salidas: el modelo de la tabla de instrucciones
     * Restricciones: ninguna
     * Descripcion: la ventana se lo asigna a su tabla al construirse.
     */
    public ModeloTablaInstrucciones getModeloInstrucciones() {
        return modeloInstrucciones;
    }

    /**
     * Nombre: getModeloMemoria
     * Entradas: ninguna
     * Salidas: el modelo de la tabla de memoria
     * Restricciones: ninguna
     * Descripcion: la ventana se lo asigna a su tabla al construirse.
     */
    public ModeloTablaMemoria getModeloMemoria() {
        return modeloMemoria;
    }

    /**
     * Nombre: getModeloDisco
     * Entradas: ninguna
     * Salidas: el modelo de la tabla del disco
     * Restricciones: ninguna
     * Descripcion: la ventana se lo asigna a su tabla al construirse.
     */
    public ModeloTablaDisco getModeloDisco() {
        return modeloDisco;
    }

    /**
     * Nombre: getRenderDisco
     * Entradas: ninguna
     * Salidas: el renderer que colorea las zonas del disco
     * Restricciones: ninguna
     * Descripcion: la ventana se lo asigna a su tabla al construirse.
     */
    public RenderZonaDisco getRenderDisco() {
        return renderDisco;
    }

    /**
     * Nombre: getDisco
     * Entradas: ninguna
     * Salidas: el disco de la minicomputadora
     * Restricciones: ninguna
     * Descripcion: lo necesitan las pruebas para verificar lo que se guardo.
     */
    public Disco getDisco() {
        return disco;
    }

    /**
     * Nombre: getConfiguracion
     * Entradas: ninguna
     * Salidas: la configuracion aplicada en este momento
     * Restricciones: ninguna
     * Descripcion: el dialogo de configuracion la usa para mostrar los
     *              valores actuales al abrirse.
     */
    public Configuracion getConfiguracion() {
        return configuracion;
    }

    /**
     * Nombre: getRenderInstrucciones
     * Entradas: ninguna
     * Salidas: el renderer que resalta la instruccion actual
     * Restricciones: ninguna
     * Descripcion: la ventana se lo asigna a su tabla al construirse.
     */
    public RenderInstruccionActual getRenderInstrucciones() {
        return renderInstrucciones;
    }

    /**
     * Nombre: getRenderMemoria
     * Entradas: ninguna
     * Salidas: el renderer que colorea las zonas de memoria
     * Restricciones: ninguna
     * Descripcion: la ventana se lo asigna a su tabla al construirse.
     */
    public RenderZonaMemoria getRenderMemoria() {
        return renderMemoria;
    }

    /**
     * Nombre: obtenerEstadisticas
     * Entradas: ninguna
     * Salidas: la contabilidad de la ejecucion
     * Restricciones: ninguna
     * Descripcion: la consulta el dialogo de estadisticas.
     */
    public Estadisticas obtenerEstadisticas() {
        return cpu.getEstadisticas();
    }

    /**
     * Nombre: getProcesador
     * Entradas: ninguna
     * Salidas: el procesador que el controlador coordina
     * Restricciones: ninguna
     * Descripcion: lo necesitan los dialogos para leer la memoria y el BCP,
     *              y las pruebas para verificar el resultado de la ejecucion.
     */
    public Procesador getProcesador() {
        return cpu;
    }

    /**
     * Nombre: getVelocidadMs
     * Entradas: ninguna
     * Salidas: milisegundos reales que dura cada segundo de CPU en automatico
     * Restricciones: ninguna
     * Descripcion: el dialogo de configuracion lo usa para mostrar el valor
     *              actual al abrirse.
     */
    public int getVelocidadMs() {
        return temporizador.getDelay();
    }

    /**
     * Nombre: inicializarVista
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: debe llamarse una vez, al final del constructor de la
     *                ventana, cuando sus componentes ya existen
     * Descripcion: deja la ventana en su estado inicial, escribe en la
     *              consola la configuracion con la que arranco y, si el
     *              archivo de configuracion tenia problemas, los informa.
     */
    public void inicializarVista() {
        actualizarVista();
        vista.refrescarDisco();
        vista.escribirEnConsola("Mini PC listo. " + describirConfiguracion());
        if (lectorConfiguracion != null && erroresConfiguracion.isEmpty()) {
            vista.escribirEnConsola("Configuracion leida de "
                    + lectorConfiguracion.getArchivo().toAbsolutePath() + ".");
        }
        if (!erroresConfiguracion.isEmpty()) {
            vista.escribirEnConsola("La configuracion tiene errores. "
                    + "Se usan los valores por defecto.");
            vista.mostrarErrores("Configuracion invalida", erroresConfiguracion);
        }
    }
}
