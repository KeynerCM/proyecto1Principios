package com.mycompany.minipc.isa;

/**
 * Nombre: Operando
 * Entradas: el tipo de operando y su valor
 * Salidas: no aplica
 * Restricciones: es inmutable; se crea con los metodos de fabrica, que
 *                garantizan que el valor corresponde al tipo
 * Descripcion: un operando de una instruccion ya interpretado: un registro,
 *              un numero, un desplazamiento de salto, un codigo de
 *              interrupcion o un texto entre comillas.
 */
public final class Operando {

    private final Forma.TipoOperando tipo;
    private final RegistroID registro;
    private final int valor;
    private final Interrupcion interrupcion;
    private final String texto;

    private Operando(Forma.TipoOperando tipo, RegistroID registro, int valor,
            Interrupcion interrupcion, String texto) {
        this.tipo = tipo;
        this.registro = registro;
        this.valor = valor;
        this.interrupcion = interrupcion;
        this.texto = texto;
    }

    /**
     * Nombre: registro
     * Entradas: id, registro al que se refiere el operando
     * Salidas: el operando construido
     * Restricciones: el registro no debe ser nulo
     * Descripcion: fabrica un operando de tipo registro.
     */
    public static Operando registro(RegistroID id) {
        if (id == null) {
            throw new IllegalArgumentException("El registro es obligatorio");
        }
        return new Operando(Forma.TipoOperando.REGISTRO, id, 0, null, null);
    }

    /**
     * Nombre: numero
     * Entradas: valor, numero inmediato
     * Salidas: el operando construido
     * Restricciones: ninguna
     * Descripcion: fabrica un operando de tipo numero.
     */
    public static Operando numero(int valor) {
        return new Operando(Forma.TipoOperando.NUMERO, null, valor, null, null);
    }

    /**
     * Nombre: desplazamiento
     * Entradas: valor, cuantas instrucciones saltar, con signo
     * Salidas: el operando construido
     * Restricciones: ninguna
     * Descripcion: fabrica un operando de salto.
     */
    public static Operando desplazamiento(int valor) {
        return new Operando(Forma.TipoOperando.DESPLAZAMIENTO, null, valor, null, null);
    }

    /**
     * Nombre: interrupcion
     * Entradas: interrupcion, servicio pedido con INT
     * Salidas: el operando construido
     * Restricciones: la interrupcion no debe ser nula
     * Descripcion: fabrica un operando de interrupcion.
     */
    public static Operando interrupcion(Interrupcion interrupcion) {
        if (interrupcion == null) {
            throw new IllegalArgumentException("La interrupcion es obligatoria");
        }
        return new Operando(Forma.TipoOperando.INTERRUPCION, null, 0, interrupcion, null);
    }

    /**
     * Nombre: texto
     * Entradas: texto, contenido que iba entre comillas
     * Salidas: el operando construido
     * Restricciones: el texto no debe ser nulo ni vacio
     * Descripcion: fabrica un operando de texto, como el nombre de un archivo.
     */
    public static Operando texto(String texto) {
        if (texto == null || texto.isEmpty()) {
            throw new IllegalArgumentException("El texto es obligatorio");
        }
        return new Operando(Forma.TipoOperando.TEXTO, null, 0, null, texto);
    }

    /**
     * Nombre: getTipo
     * Entradas: ninguna
     * Salidas: el tipo del operando
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Forma.TipoOperando getTipo() {
        return tipo;
    }

    /**
     * Nombre: getRegistro
     * Entradas: ninguna
     * Salidas: el registro, o nulo si el operando no es un registro
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public RegistroID getRegistro() {
        return registro;
    }

    /**
     * Nombre: getValor
     * Entradas: ninguna
     * Salidas: el numero o el desplazamiento; cero en los demas tipos
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public int getValor() {
        return valor;
    }

    /**
     * Nombre: getInterrupcion
     * Entradas: ninguna
     * Salidas: la interrupcion, o nulo si el operando no es de ese tipo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public Interrupcion getInterrupcion() {
        return interrupcion;
    }

    /**
     * Nombre: getTexto
     * Entradas: ninguna
     * Salidas: el texto, o nulo si el operando no es de ese tipo
     * Restricciones: ninguna
     * Descripcion: acceso de solo lectura al campo correspondiente.
     */
    public String getTexto() {
        return texto;
    }

    /**
     * Nombre: toString
     * Entradas: ninguna
     * Salidas: el operando como se escribiria en el .asm
     * Restricciones: ninguna
     * Descripcion: pensado para depuracion y para los mensajes de las pruebas.
     */
    @Override
    public String toString() {
        switch (tipo) {
            case REGISTRO:
                return registro.name();
            case INTERRUPCION:
                return interrupcion.getCodigo();
            case DESPLAZAMIENTO:
                return (valor >= 0 ? "+" : "") + valor;
            case TEXTO:
                return "\"" + texto + "\"";
            default:
                return String.valueOf(valor);
        }
    }
}
