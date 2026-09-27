package com.mycompany.minipc.core;

import com.mycompany.minipc.isa.RegistroID;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Nombre: BancoRegistros
 * Entradas: no aplica, siempre crea los mismos cuatro registros
 * Salidas: no aplica
 * Restricciones: el conjunto de registros es fijo; no se agregan ni se
 *                quitan durante la ejecucion
 * Descripcion: agrupa los cuatro registros de proposito general del Mini PC.
 *              Se usa un EnumMap porque la clave es una enumeracion:
 *              internamente es un arreglo indexado por el ordinal, de modo
 *              que el acceso es directo y el orden de recorrido es siempre
 *              AX, BX, CX, DX, que es como conviene mostrarlos en la interfaz.
 */
public class BancoRegistros {

    private final Map<RegistroID, Registro> registros;

    /**
     * Nombre: BancoRegistros
     * Entradas: ninguna
     * Salidas: el banco construido, con los cuatro registros en cero
     * Restricciones: ninguna
     * Descripcion: crea un registro por cada valor de RegistroID, de modo que
     *              agregar un registro al juego de instrucciones no obliga a
     *              tocar esta clase.
     */
    public BancoRegistros() {
        registros = new EnumMap<>(RegistroID.class);
        for (RegistroID id : RegistroID.values()) {
            registros.put(id, new Registro(id));
        }
    }

    /**
     * Nombre: leer
     * Entradas: id, registro a consultar
     * Salidas: el valor actual de ese registro
     * Restricciones: el identificador debe existir; si no, lanza
     *                IllegalArgumentException
     * Descripcion: lectura directa del contenido, usada por el procesador en
     *              las operaciones LOAD, ADD y SUB.
     */
    public int leer(RegistroID id) {
        return obtener(id).getValor();
    }

    /**
     * Nombre: escribir
     * Entradas: id, registro a modificar; valor, entero a guardar
     * Salidas: ninguna
     * Restricciones: el identificador debe existir; si no, lanza
     *                IllegalArgumentException
     * Descripcion: escritura del contenido, usada por el procesador en las
     *              operaciones MOV y STORE.
     */
    public void escribir(RegistroID id, int valor) {
        obtener(id).setValor(valor);
    }

    /**
     * Nombre: obtener
     * Entradas: id, registro a consultar
     * Salidas: el objeto Registro completo
     * Restricciones: si el identificador no existe lanza
     *                IllegalArgumentException
     * Descripcion: da acceso al objeto y no solo a su valor, para cuando hace
     *              falta su identidad.
     */
    public Registro obtener(RegistroID id) {
        Registro registro = registros.get(id);
        if (registro == null) {
            throw new IllegalArgumentException("Registro inexistente: " + id);
        }
        return registro;
    }

    /**
     * Nombre: reset
     * Entradas: ninguna
     * Salidas: ninguna
     * Restricciones: ninguna
     * Descripcion: pone los cuatro registros en cero, como al cargar un
     *              programa nuevo o al reiniciar el actual.
     */
    public void reset() {
        for (Registro registro : registros.values()) {
            registro.reset();
        }
    }

    /**
     * Nombre: todos
     * Entradas: ninguna
     * Salidas: los cuatro registros en orden AX, BX, CX, DX
     * Restricciones: la coleccion devuelta es de solo lectura
     * Descripcion: permite recorrer el banco para mostrarlo sin exponer la
     *              estructura interna a modificaciones.
     */
    public Collection<Registro> todos() {
        return Collections.unmodifiableCollection(registros.values());
    }

    /**
     * Nombre: instantanea
     * Entradas: ninguna
     * Salidas: un mapa independiente con el valor de cada registro
     * Restricciones: la copia no sigue los cambios posteriores del banco
     * Descripcion: copia el contenido actual para guardarlo en el BCP. Es el
     *              equivalente al guardado de contexto que hace el sistema
     *              operativo: una foto del momento, no una referencia viva.
     */
    public Map<RegistroID, Integer> instantanea() {
        Map<RegistroID, Integer> copia = new EnumMap<>(RegistroID.class);
        for (Map.Entry<RegistroID, Registro> entrada : registros.entrySet()) {
            copia.put(entrada.getKey(), entrada.getValue().getValor());
        }
        return copia;
    }
}
