package com.mycompany.minipc.hardware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Nombre: Memoria
 * Entradas: el tamano total y el limite entre zonas
 * Salidas: no aplica
 * Restricciones: el kernel ocupa al menos una posicion y a los programas
 *                les deben quedar al menos 32
 * Descripcion: memoria principal del Mini PC, dividida en zona de kernel y
 *              zona de usuario. Es un arreglo de texto: cada posicion guarda
 *              una palabra, que puede ser una instruccion ("MOV AX, 5"), un
 *              numero ("163") o nada (cadena vacia). La memoria no sabe que
 *              significa cada palabra; eso lo decide quien la lee, igual que
 *              en una computadora real, donde la CPU interpreta como
 *              instruccion lo que trae con el PC y como dato lo demas. Los
 *              valores numericos se convierten a int al leerlos con
 *              leerEntero y vuelven a texto al escribirlos con escribirEntero.
 */
public class Memoria {

    /** Tamano con el que arranca la aplicacion, el que fija el enunciado. */
    public static final int TAMANO_POR_DEFECTO = 256;

    /** Minimo de posiciones que deben quedar para los programas. */
    public static final int ESPACIO_USUARIO_MINIMO = 32;

    /** Contenido de una posicion libre. */
    public static final String VACIA = "";

    private String[] celdas;
    private int tamano;
    private int limiteKernel;

    /**
     * Nombre: Memoria
     * Entradas: tamano, cantidad total de posiciones; limiteKernel, cuantas
     *           posiciones del inicio pertenecen al sistema operativo
     * Salidas: la memoria construida, con todas las posiciones vacias
     * Restricciones: lanza IllegalArgumentException si la combinacion no es
     *                valida (ver validar)
     * Descripcion: el tamano del kernel no se configura: lo calcula el
     *              sistema operativo segun cuantos BCP necesita guardar.
     */
    public Memoria(int tamano, int limiteKernel) {
        redimensionar(tamano, limiteKernel);
    }

    /**
     * Nombre: validar
     * Entradas: tamano, cantidad total de posiciones; limiteKernel, primera
     *           direccion de la zona de usuario
     * Salidas: la lista de problemas encontrados, vacia si todo es valido
     * Restricciones: ninguna, no lanza excepciones
     * Descripcion: concentra las reglas de tamano de la memoria en un solo
     *              lugar: el kernel ocupa al menos una posicion y a los
     *              programas les deben quedar al menos 32. La usa
     *              redimensionar y tambien la configuracion, que necesita
     *              reportar los errores sin construir la memoria.
     */
    public static List<String> validar(int tamano, int limiteKernel) {
        List<String> errores = new ArrayList<>();
        if (limiteKernel < 1) {
            errores.add("El kernel debe ocupar al menos una posicion, se recibio "
                    + limiteKernel);
        } else if (tamano - limiteKernel < ESPACIO_USUARIO_MINIMO) {
            errores.add("El tamano de memoria debe ser de al menos "
                    + (limiteKernel + ESPACIO_USUARIO_MINIMO) + " (" + limiteKernel
                    + " para el kernel y " + ESPACIO_USUARIO_MINIMO
                    + " para los programas), se recibio " + tamano);
        }
        return errores;
    }

    /**
     * Nombre: redimensionar
     * Entradas: tamano, cantidad total de posiciones; limiteKernel, primera
     *           direccion de la zona de usuario
     * Salidas: ninguna
     * Restricciones: lanza IllegalArgumentException si la combinacion no es
     *                valida (ver validar). Descarta todo el contenido anterior
     * Descripcion: reconstruye el arreglo con todas las posiciones vacias. Es
     *              final porque el constructor la invoca.
     */
    public final void redimensionar(int tamano, int limiteKernel) {
        List<String> errores = validar(tamano, limiteKernel);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errores));
        }

        this.tamano = tamano;
        this.limiteKernel = limiteKernel;
        this.celdas = new String[tamano];
        Arrays.fill(celdas, VACIA);
    }

    /**
     * Nombre: getTamano
     * Entradas: ninguna
     * Salidas: cantidad total de posiciones de la memoria
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura a la configuracion actual.
     */
    public int getTamano() {
        return tamano;
    }

    /**
     * Nombre: getLimiteKernel
     * Entradas: ninguna
     * Salidas: primera direccion de la zona de usuario
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura a la configuracion actual.
     */
    public int getLimiteKernel() {
        return limiteKernel;
    }

    /**
     * Nombre: getEspacioUsuario
     * Entradas: ninguna
     * Salidas: cuantas posiciones tiene la zona de usuario
     * Restricciones: ninguna
     * Descripcion: es el tamano total menos el limite de kernel. Determina el
     *              programa mas largo que se puede cargar.
     */
    public int getEspacioUsuario() {
        return tamano - limiteKernel;
    }

    /**
     * Nombre: esDireccionKernel
     * Entradas: direccion, posicion a evaluar
     * Salidas: true si pertenece a la zona del sistema operativo
     * Restricciones: una direccion negativa devuelve false, no falla
     * Descripcion: lo consultan el renderer de la tabla y la lectura en modo
     *              usuario, para decidir color y permiso respectivamente.
     */
    public boolean esDireccionKernel(int direccion) {
        return direccion >= 0 && direccion < limiteKernel;
    }

    /**
     * Nombre: leer
     * Entradas: direccion, posicion a leer
     * Salidas: el texto guardado en esa posicion, vacio si esta libre
     * Restricciones: lanza IndexOutOfBoundsException si la direccion no existe
     * Descripcion: lectura sin restriccion de zona. La usan el sistema
     *              operativo y la interfaz, que debe poder mostrar la memoria
     *              completa, incluida la parte del kernel, aunque el proceso
     *              no pueda leerla.
     */
    public String leer(int direccion) {
        validarDireccion(direccion);
        return celdas[direccion];
    }

    /**
     * Nombre: leerComoUsuario
     * Entradas: direccion, posicion a leer
     * Salidas: el texto guardado en esa posicion
     * Restricciones: lanza IndexOutOfBoundsException si la direccion no
     *                existe, e IllegalArgumentException si pertenece al kernel
     * Descripcion: lectura en nombre del proceso de usuario. Ademas de validar
     *              el rango, rechaza el acceso a la zona del kernel. Es el
     *              equivalente didactico de una violacion de segmento: el
     *              proceso no puede leer la memoria del sistema operativo.
     */
    public String leerComoUsuario(int direccion) {
        validarDireccion(direccion);
        if (esDireccionKernel(direccion)) {
            throw new IllegalArgumentException("Acceso denegado: la direccion " + direccion
                    + " pertenece a la zona de kernel (0 a " + (limiteKernel - 1) + ")");
        }
        return celdas[direccion];
    }

    /**
     * Nombre: leerEntero
     * Entradas: direccion, posicion a leer
     * Salidas: el numero guardado en esa posicion, o cero si esta vacia
     * Restricciones: lanza IndexOutOfBoundsException si la direccion no
     *                existe, e IllegalStateException si la posicion guarda un
     *                texto que no es un numero
     * Descripcion: convierte a int el texto de la celda, para los valores que
     *              se usan en calculos (PC, registros, direcciones). Una celda
     *              vacia vale cero, como una palabra de memoria sin escribir.
     */
    public int leerEntero(int direccion) {
        String texto = leer(direccion).trim();
        if (texto.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("La posicion " + direccion
                    + " no guarda un numero: \"" + texto + "\"", e);
        }
    }

    /**
     * Nombre: escribir
     * Entradas: direccion, posicion a escribir; texto, contenido a guardar
     * Salidas: ninguna
     * Restricciones: lanza IndexOutOfBoundsException si la direccion no
     *                existe; un texto nulo se guarda como posicion vacia
     * Descripcion: escribe en cualquier posicion, sin restriccion de zona.
     */
    public void escribir(int direccion, String texto) {
        validarDireccion(direccion);
        celdas[direccion] = texto == null ? VACIA : texto;
    }

    /**
     * Nombre: escribirEntero
     * Entradas: direccion, posicion a escribir; valor, numero a guardar
     * Salidas: ninguna
     * Restricciones: lanza IndexOutOfBoundsException si la direccion no existe
     * Descripcion: guarda el numero como texto, que es como la memoria guarda
     *              todo.
     */
    public void escribirEntero(int direccion, int valor) {
        escribir(direccion, String.valueOf(valor));
    }

    /**
     * Nombre: estaLibre
     * Entradas: direccion, posicion a consultar
     * Salidas: true si la posicion no guarda nada
     * Restricciones: lanza IndexOutOfBoundsException si la direccion no existe
     * Descripcion: una posicion esta libre cuando su texto es vacio.
     */
    public boolean estaLibre(int direccion) {
        return leer(direccion).isEmpty();
    }

    /**
     * Nombre: limpiarZonaUsuario
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: no toca la zona del kernel; es final porque otros metodos
     *                publicos la invocan
     * Descripcion: deja libres todas las posiciones desde el limite de kernel
     *              hasta el final de la memoria.
     */
    public final void limpiarZonaUsuario() {
        Arrays.fill(celdas, limiteKernel, tamano, VACIA);
    }

    /**
     * Nombre: getPosicionesUsadas
     * Entradas: ninguna
     * Salidas: cuantas posiciones de la zona de usuario estan ocupadas
     * Restricciones: recorre la zona completa, de modo que su costo crece con
     *                el tamano de la memoria
     * Descripcion: cuenta las celdas que no estan libres, para las
     *              estadisticas y la barra de ocupacion.
     */
    public int getPosicionesUsadas() {
        int usadas = 0;
        for (int i = limiteKernel; i < tamano; i++) {
            if (!celdas[i].isEmpty()) {
                usadas++;
            }
        }
        return usadas;
    }

    /**
     * Nombre: getPorcentajeUso
     * Entradas: ninguna
     * Salidas: porcentaje de la zona de usuario ocupado, de 0 a 100
     * Restricciones: devuelve cero si la zona de usuario no tiene posiciones
     * Descripcion: se calcula sobre la zona de usuario y no sobre la memoria
     *              total, porque el kernel esta siempre reservado y contarlo
     *              daria una cifra que nunca baja de cierto piso.
     */
    public int getPorcentajeUso() {
        int disponibles = getEspacioUsuario();
        return disponibles == 0 ? 0 : (getPosicionesUsadas() * 100) / disponibles;
    }

    /**
     * Nombre: validarDireccion
     * Entradas: direccion, posicion a comprobar
     * Salidas: ninguna si la direccion es valida
     * Restricciones: lanza IndexOutOfBoundsException si queda fuera del rango
     * Descripcion: comprobacion comun a todos los accesos, con un mensaje que
     *              nombra el rango valido para que el error sea diagnosticable.
     */
    private void validarDireccion(int direccion) {
        if (direccion < 0 || direccion >= tamano) {
            throw new IndexOutOfBoundsException("Direccion fuera de la memoria: " + direccion
                    + ", el rango valido es 0 a " + (tamano - 1));
        }
    }
}
