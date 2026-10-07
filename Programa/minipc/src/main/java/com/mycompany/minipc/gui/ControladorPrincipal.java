package com.mycompany.minipc.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Timer;

import com.mycompany.minipc.config.Configuracion;
import com.mycompany.minipc.config.LectorConfiguracion;
import com.mycompany.minipc.excepciones.ConfiguracionException;
import com.mycompany.minipc.excepciones.DiscoException;
import com.mycompany.minipc.excepciones.SintaxisException;
import com.mycompany.minipc.gui.modelo.MapaMemoria;
import com.mycompany.minipc.gui.modelo.ModeloTablaDisco;
import com.mycompany.minipc.gui.modelo.ModeloTablaInstrucciones;
import com.mycompany.minipc.gui.modelo.ModeloTablaMemoria;
import com.mycompany.minipc.gui.modelo.ModeloTablaTrabajos;
import com.mycompany.minipc.gui.modelo.RenderInstruccionActual;
import com.mycompany.minipc.gui.modelo.RenderZonaDisco;
import com.mycompany.minipc.gui.modelo.RenderZonaMemoria;
import com.mycompany.minipc.hardware.Disco;
import com.mycompany.minipc.hardware.EntradaIndice;
import com.mycompany.minipc.hardware.Estadisticas;
import com.mycompany.minipc.hardware.Memoria;
import com.mycompany.minipc.hardware.Procesador;
import com.mycompany.minipc.io.CargadorASM;
import com.mycompany.minipc.isa.Ensamblador;
import com.mycompany.minipc.isa.Instruccion;
import com.mycompany.minipc.so.SistemaOperativo;
import com.mycompany.minipc.so.planificacion.FabricaAlgoritmos;
import com.mycompany.minipc.so.procesos.Proceso;

/**
 * Nombre: ControladorPrincipal
 * Entradas: la vista a la que da servicio y las acciones que el usuario pulsa
 * Salidas: las actualizaciones que envia a la vista
 * Restricciones: es lo unico que conoce a los dos lados; el sistema operativo
 *                nunca sabe que existe Swing y la vista nunca conoce al
 *                sistema operativo
 * Descripcion: coordina la ventana con el sistema operativo. Recibe lo que el
 *              usuario pulsa, se lo pide al sistema operativo y refresca la
 *              vista. Cada "Siguiente" es un tick() del sistema operativo, es
 *              decir un segundo de CPU.
 */
public class ControladorPrincipal {

    private final VistaPrincipal vista;
    private final SistemaOperativo so;
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
    private final ModeloTablaTrabajos modeloTrabajos;
    private final RenderInstruccionActual renderInstrucciones;
    private final RenderZonaMemoria renderMemoria;
    private final RenderZonaDisco renderDisco;

    /**
     * Temporizador de la ejecucion automatica.
     *
     * Se usa un javax.swing.Timer y no un bucle porque sus disparos ocurren
     * en el hilo de despacho de eventos. Un while llamando a tick() dentro
     * de ese hilo congelaria la ventana hasta terminar, y no se veria nada
     * de la ejecucion, que es justo lo que hay que mostrar.
     */
    private final Timer temporizador;

    /** Proceso cuyo programa muestra la tabla de instrucciones. */
    private Proceso procesoMostrado;

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
     * Restricciones: la vista no debe ser nula. Si el archivo tiene errores no
     *                falla: usa los valores por defecto y guarda los problemas
     *                para informarlos en inicializarVista, cuando la ventana
     *                ya existe
     * Descripcion: lee la configuracion, crea el sistema operativo con esos
     *              tamanos y ese algoritmo, los modelos de tabla, los
     *              renderers y el temporizador. Conecta la bitacora del
     *              sistema operativo a la consola de la vista.
     */
    public ControladorPrincipal(VistaPrincipal vista, LectorConfiguracion lectorConfiguracion) {
        this.vista = vista;
        this.lectorConfiguracion = lectorConfiguracion;
        this.erroresConfiguracion = new ArrayList<>();
        this.configuracion = leerConfiguracion();

        this.so = new SistemaOperativo(configuracion.getTamanoMemoria(),
                configuracion.getPorcentajeKernel(), configuracion.getTamanoDisco(),
                configuracion.getTamanoMemoriaVirtual(),
                FabricaAlgoritmos.crear(configuracion.getAlgoritmo()));
        this.so.setBitacora(vista::escribirEnConsola);
        this.cargador = new CargadorASM();
        this.ensamblador = new Ensamblador();

        MapaMemoria mapa = new MapaMemoria(so.getMemoria(), so.getTablaBCP());
        this.modeloInstrucciones = new ModeloTablaInstrucciones();
        this.modeloMemoria = new ModeloTablaMemoria(so.getMemoria(), mapa);
        this.modeloDisco = new ModeloTablaDisco(so.getDisco(), so.getTablaBCP());
        this.modeloTrabajos = new ModeloTablaTrabajos(so.getListaTrabajos());
        this.renderInstrucciones = new RenderInstruccionActual();
        this.renderMemoria = new RenderZonaMemoria(so.getMemoria(), mapa);
        this.renderDisco = new RenderZonaDisco(so.getDisco());

        this.temporizador = new Timer(configuracion.getMsPorSegundo(),
                e -> alTicDelTemporizador());
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
     * Salidas: ninguna; deja los programas validos en el disco y en la lista
     *          de trabajos, y la vista actualizada
     * Restricciones: si el usuario cancela el dialogo no ocurre nada. Un
     *                archivo con problemas no se guarda, pero no impide que
     *                se guarden los demas
     * Descripcion: carga uno o varios archivos .asm. Por cada uno encadena
     *              leerlo, ensamblarlo, guardarlo en el disco y agregarlo a la
     *              lista de trabajos. Los problemas de todos los archivos se
     *              juntan en un solo cuadro, cada uno con el nombre de su
     *              archivo. Al final el planificador de trabajos admite lo que
     *              quepa, para que los programas se vean en memoria.
     */
    public void alCargarArchivos() {
        List<File> archivos = vista.seleccionarArchivosAsm();
        if (archivos == null || archivos.isEmpty()) {
            return;
        }

        List<String> errores = new ArrayList<>();
        for (File archivo : archivos) {
            EntradaIndice entrada = guardarEnDisco(archivo, errores);
            if (entrada != null) {
                agregarALaListaDeTrabajos(entrada, errores);
            }
        }
        so.admitir();
        vista.refrescarDisco();
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
     *              correcta, que quepa en la memoria de usuario y que haya
     *              lugar en el disco. En el disco queda el texto de cada
     *              instruccion, sin comentarios ni lineas vacias. Si ya hay un
     *              archivo con el mismo nombre, guarda una copia numerada.
     */
    private EntradaIndice guardarEnDisco(File archivo, List<String> errores) {
        String nombre = archivo.getName();
        try {
            List<Instruccion> programa = ensamblador.ensamblar(cargador.leer(archivo));
            List<String> lineas = new ArrayList<>();
            for (Instruccion instruccion : programa) {
                lineas.add(instruccion.getTextoFuente());
            }
            int espacioUsuario = so.getMemoria().getEspacioUsuario();
            if (lineas.size() > espacioUsuario) {
                String mensaje = "el programa tiene " + lineas.size() + " instrucciones y la"
                        + " memoria de usuario solo tiene " + espacioUsuario + " posiciones";
                errores.add(nombre + ": " + mensaje);
                vista.escribirEnConsola("No se cargo " + nombre + ": " + mensaje + ".");
                return null;
            }
            EntradaIndice entrada = so.getDisco().guardarPrograma(
                    so.getDisco().nombreDisponible(nombre), lineas);
            vista.escribirEnConsola("Guardado en disco: " + entrada.getNombre()
                    + ", posiciones " + entrada.getDireccionInicio() + " a "
                    + entrada.getDireccionFin() + ".");
            if (programa.stream().noneMatch(Instruccion::esFinDePrograma)) {
                vista.escribirEnConsola("Advertencia: " + entrada.getNombre()
                        + " no tiene INT 20H; el programa terminara al llegar a su ultima"
                        + " instruccion.");
            }
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
     * Nombre: agregarALaListaDeTrabajos
     * Entradas: entrada, archivo ya guardado en el disco; errores, lista donde
     *           anotar el problema si no se puede agregar
     * Salidas: ninguna
     * Restricciones: si la lista de trabajos esta llena, el archivo se borra
     *                del disco para que ambos queden coherentes
     * Descripcion: el programa entra a la lista de trabajos como NUEVO.
     */
    private void agregarALaListaDeTrabajos(EntradaIndice entrada, List<String> errores) {
        try {
            so.agregarTrabajo(entrada.getNombre());
        } catch (IllegalStateException | IllegalArgumentException e) {
            errores.add(entrada.getNombre() + ": " + e.getMessage());
            vista.escribirEnConsola(e.getMessage());
            try {
                so.getDisco().eliminar(entrada.getNombre());
            } catch (DiscoException ignorada) {
                // Se acaba de guardar, asi que existe.
            }
        }
    }

    /**
     * Nombre: alEnviarTeclado
     * Entradas: texto, lo que el usuario escribio en el teclado
     * Salidas: ninguna
     * Restricciones: si el valor no es un numero de 0 a 255, o si nadie espera
     *                el teclado, lo informa y no cambia nada
     * Descripcion: el ENTER del teclado: el sistema operativo entrega el
     *              valor al proceso que ejecuto INT 09H, que vuelve a
     *              PREPARADO.
     */
    public void alEnviarTeclado(String texto) {
        try {
            so.entradaTeclado(texto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            vista.mostrarErrores("Teclado", Collections.singletonList(e.getMessage()));
        }
        actualizarVista();
    }

    /**
     * Nombre: alEjecutar
     * Entradas: ninguna
     * Salidas: ninguna; arranca el temporizador
     * Restricciones: no hace nada si no hay trabajos pendientes o si la
     *                ejecucion automatica ya esta en marcha
     * Descripcion: ejecuta todos los procesos cargados hasta su finalizacion,
     *              un segundo de CPU por disparo del temporizador.
     */
    public void alEjecutar() {
        if (!so.hayPendientes() || temporizador.isRunning()) {
            return;
        }
        vista.escribirEnConsola("Ejecucion automatica iniciada.");
        temporizador.start();
        actualizarVista();
    }

    /**
     * Nombre: alPausar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no hace nada si la ejecucion automatica no esta en marcha
     * Descripcion: detiene la ejecucion automatica; se puede seguir con
     *              Siguiente o volver a Ejecutar.
     */
    public void alPausar() {
        if (temporizador.isRunning()) {
            detener();
            vista.escribirEnConsola("Ejecucion automatica en pausa.");
        }
    }

    /**
     * Nombre: alPasoAPaso
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no hace nada si no hay trabajos pendientes o si la
     *                ejecucion automatica esta en marcha
     * Descripcion: el boton "Siguiente": un segundo de CPU.
     */
    public void alPasoAPaso() {
        if (!so.hayPendientes() || temporizador.isRunning()) {
            return;
        }
        ejecutarUnPaso();
    }

    /**
     * Nombre: alReiniciar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: detiene antes la ejecucion automatica si estaba corriendo
     * Descripcion: vuelve todos los trabajos a NUEVO y el reloj a cero, sin
     *              borrar el disco, y admite de nuevo los que quepan.
     */
    public void alReiniciar() {
        detener();
        if (so.getListaTrabajos().getTrabajos().isEmpty()) {
            return;
        }
        so.reiniciar();
        so.admitir();
        actualizarVista();
    }

    /**
     * Nombre: alLimpiar
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: detiene antes la ejecucion automatica si estaba corriendo
     * Descripcion: descarga los procesos, vacia la lista de trabajos, borra
     *              los archivos del disco y deja la consola en blanco.
     */
    public void alLimpiar() {
        detener();
        so.limpiar();
        so.getDisco().formatear();
        vista.refrescarDisco();
        vista.limpiarConsola();
        vista.escribirEnConsola("Memoria, disco, lista de trabajos y registros vaciados.");
        actualizarVista();
    }

    /**
     * Nombre: alConfigurar
     * Entradas: nueva, configuracion ya validada
     * Salidas: ninguna
     * Restricciones: descarta los procesos, los trabajos y el disco, porque
     *                redimensionarlos invalida las direcciones ya asignadas.
     *                Si el archivo no se puede escribir, la configuracion se
     *                aplica igual y se informa el problema
     * Descripcion: aplica la configuracion al sistema operativo y al
     *              temporizador, y la guarda en el archivo de configuracion
     *              para que se conserve la proxima vez que se abra el programa.
     */
    public void alConfigurar(Configuracion nueva) {
        detener();
        so.reconfigurar(nueva.getTamanoMemoria(), nueva.getPorcentajeKernel(),
                nueva.getTamanoDisco(), nueva.getTamanoMemoriaVirtual(),
                FabricaAlgoritmos.crear(nueva.getAlgoritmo()));
        temporizador.setDelay(nueva.getMsPorSegundo());
        configuracion = nueva;

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
        Memoria memoria = so.getMemoria();
        Disco disco = so.getDisco();
        return "Memoria de " + memoria.getTamano() + " posiciones: kernel de 0 a "
                + (memoria.getLimiteKernel() - 1) + " (" + so.describirKernel()
                + "), usuario de " + memoria.getLimiteKernel() + " a "
                + (memoria.getTamano() - 1) + ". Disco de " + disco.getTamano()
                + " posiciones (indice de 0 a " + (Disco.ENTRADAS_INDICE - 1)
                + ", archivos de " + disco.getInicioArchivos() + " a "
                + (disco.getInicioMemoriaVirtual() - 1) + ", memoria virtual: "
                + disco.getTamanoMemoriaVirtual() + "). Planificacion: "
                + so.getAlgoritmo().getNombre() + ". Segundo de CPU: "
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
     * Descripcion: cada disparo del temporizador es un segundo de CPU; si ya
     *              no quedan trabajos, detiene la ejecucion automatica.
     */
    private void alTicDelTemporizador() {
        if (!ejecutarUnPaso()) {
            detener();
        }
    }

    /**
     * Nombre: ejecutarUnPaso
     * Entradas: ninguna
     * Salidas: true si quedan trabajos sin finalizar
     * Restricciones: si un proceso falla, detiene la ejecucion automatica y
     *                muestra el error; los demas procesos siguen pendientes
     * Descripcion: un tick del sistema operativo. Los mensajes del despachador
     *              y de los planificadores llegan a la consola por la bitacora.
     */
    private boolean ejecutarUnPaso() {
        boolean quedan = so.tick();
        List<String> errores = so.tomarErrores();
        if (!errores.isEmpty()) {
            detener();
            vista.mostrarErrores("Error de ejecucion", errores);
        }
        if (!quedan) {
            vista.escribirEnConsola("Todos los trabajos finalizaron. Tiempo total: "
                    + SistemaOperativo.formatearReloj(so.getReloj()) + ".");
        }
        actualizarVista();
        if (!quedan) {
            vista.mostrarEstadisticas();
        }
        return quedan;
    }

    /**
     * Nombre: detener
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no hace nada si el temporizador no estaba corriendo
     * Descripcion: detiene la ejecucion automatica y refresca la vista para
     *              que los botones vuelvan a habilitarse.
     */
    private void detener() {
        if (temporizador.isRunning()) {
            temporizador.stop();
            actualizarVista();
        }
    }

    /**
     * Nombre: actualizarVista
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: vuelca el estado del sistema operativo sobre la ventana.
     *              Todo lo que se muestra del proceso en ejecucion se lee de su
     *              BCP en memoria. La tabla de instrucciones muestra el
     *              programa del proceso en ejecucion, leido de su region de
     *              memoria.
     */
    private void actualizarVista() {
        Proceso actual = so.getEnEjecucion();
        mostrarPrograma(actual);
        int indice = -1;
        int direccionActual = -1;
        if (actual != null) {
            direccionActual = actual.getPc();
            Procesador cpu = so.getCpu();
            if (cpu.getSegundosCumplidos() < cpu.getPesoActual()) {
                // A mitad de su peso se resalta la instruccion en curso, no la siguiente.
                direccionActual = cpu.getDireccionIr();
            }
            int relativo = direccionActual - actual.getBase();
            indice = relativo >= 0 && relativo < actual.getAlcance() ? relativo : -1;
        }
        renderInstrucciones.setFilaActual(indice);
        renderMemoria.setDireccionActual(indice >= 0 ? direccionActual : -1);

        vista.resaltarInstruccion(indice);
        vista.refrescarMemoria();
        modeloTrabajos.refrescar();
        vista.mostrarBCP(actual);
        vista.actualizarBarraContexto(actual != null ? actual.getPrograma() : "(CPU libre)",
                textoDelEstado());
        vista.actualizarUsoMemoria(so.getMemoria().getPorcentajeUso());
        vista.mostrarPantalla(so.getPantalla().getLineas());
        Proceso destino = so.getEsperandoTeclado();
        vista.habilitarTeclado(destino != null, destino == null ? null : destino.toString());
        vista.mostrarReloj(SistemaOperativo.formatearReloj(so.getReloj()));
        vista.mostrarColas(so.getListaProcesos().recorrer());
        Disco disco = so.getDisco();
        int usoDisco = 100 - (disco.getPosicionesLibres() * 100) / Math.max(1, disco.getEspacioArchivos());
        vista.mostrarResumen(usoDisco, so.getTablaBCP().getProcesosAdmitidos());
        boolean hayTrabajos = !so.getListaTrabajos().getTrabajos().isEmpty();
        vista.actualizarBotones(hayTrabajos, temporizador.isRunning(), !so.hayPendientes());
    }

    /**
     * Nombre: mostrarPrograma
     * Entradas: actual, proceso en ejecucion, o nulo
     * Salidas: ninguna
     * Restricciones: solo recarga la tabla si cambio el proceso
     * Descripcion: lee de la memoria las instrucciones de la region del
     *              proceso (de la base a base + alcance - 1).
     */
    private void mostrarPrograma(Proceso actual) {
        if (actual == null ? procesoMostrado == null : actual.equals(procesoMostrado)) {
            return;
        }
        procesoMostrado = actual;
        List<String> lineas = new ArrayList<>();
        if (actual != null) {
            for (int i = 0; i < actual.getAlcance(); i++) {
                lineas.add(so.getMemoria().leer(actual.getBase() + i));
            }
        }
        modeloInstrucciones.cargar(lineas);
        vista.mostrarInstrucciones(lineas);
    }

    /**
     * Nombre: textoDelEstado
     * Entradas: ninguna
     * Salidas: el estado del sistema en texto
     * Restricciones: ninguna
     * Descripcion: el estado del proceso en ejecucion, o si todo termino, o
     *              si no hay trabajos.
     */
    private String textoDelEstado() {
        Proceso actual = so.getEnEjecucion();
        if (actual != null) {
            return actual.getEstado().name();
        }
        if (so.getListaTrabajos().getTrabajos().isEmpty()) {
            return "SIN PROGRAMA";
        }
        return so.hayPendientes() ? "CPU LIBRE" : "FINALIZADO";
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
     * Nombre: getModeloTrabajos
     * Entradas: ninguna
     * Salidas: el modelo de la tabla de la lista de trabajos
     * Restricciones: ninguna
     * Descripcion: la ventana se lo asigna a su tabla al construirse.
     */
    public ModeloTablaTrabajos getModeloTrabajos() {
        return modeloTrabajos;
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
     * Nombre: getDisco
     * Entradas: ninguna
     * Salidas: el disco de la minicomputadora
     * Restricciones: ninguna
     * Descripcion: lo necesitan las pruebas para verificar lo que se guardo.
     */
    public Disco getDisco() {
        return so.getDisco();
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
     * Nombre: obtenerEstadisticas
     * Entradas: ninguna
     * Salidas: los contadores de la CPU
     * Restricciones: ninguna
     * Descripcion: la consulta el dialogo de estadisticas.
     */
    public Estadisticas obtenerEstadisticas() {
        return so.getCpu().getEstadisticas();
    }

    /**
     * Nombre: getSistemaOperativo
     * Entradas: ninguna
     * Salidas: el sistema operativo que el controlador coordina
     * Restricciones: ninguna
     * Descripcion: lo necesitan los dialogos y las pruebas.
     */
    public SistemaOperativo getSistemaOperativo() {
        return so;
    }

    /**
     * Nombre: getProcesador
     * Entradas: ninguna
     * Salidas: la CPU
     * Restricciones: ninguna
     * Descripcion: acceso comodo para la ventana y las pruebas.
     */
    public Procesador getProcesador() {
        return so.getCpu();
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
