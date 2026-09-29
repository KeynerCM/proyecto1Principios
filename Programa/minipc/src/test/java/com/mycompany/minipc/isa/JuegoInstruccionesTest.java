package com.mycompany.minipc.isa;

import com.mycompany.minipc.excepciones.SintaxisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del juego completo de instrucciones del enunciado: que cada forma
 * valida se reconozca con sus operandos, y que cada linea mal escrita se
 * rechace. Los casos rechazados y aceptados son los de la seccion 7 del
 * plan ("Validacion con regex").
 */
class JuegoInstruccionesTest {

    private Ensamblador ensamblador;

    @BeforeEach
    void preparar() {
        ensamblador = new Ensamblador();
    }

    private Instruccion una(String linea) throws SintaxisException {
        return ensamblador.ensamblar(List.of(linea)).get(0);
    }

    private String errorDe(String... lineas) {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of(lineas)));
        return e.getMessage();
    }

    @Test
    @DisplayName("Las instrucciones de registros se reconocen con sus operandos")
    void instruccionesDeRegistros() throws SintaxisException {
        Instruccion mov = una("MOV BX, AX");
        assertEquals(Forma.REGISTRO_REGISTRO, mov.getForma());
        assertEquals(RegistroID.BX, mov.getRegistro(0));
        assertEquals(RegistroID.AX, mov.getRegistro(1));

        Instruccion movNumero = una("MOV BX, 5");
        assertEquals(Forma.REGISTRO_NUMERO, movNumero.getForma());
        assertEquals(5, movNumero.getValor(1));

        Instruccion swap = una("SWAP AX, BX");
        assertEquals(OpCode.SWAP, swap.getOpcode());
        assertEquals(RegistroID.BX, swap.getRegistro(1));

        Instruccion cmp = una("CMP CX, DX");
        assertEquals(OpCode.CMP, cmp.getOpcode());
        assertEquals(RegistroID.CX, cmp.getRegistro(0));
    }

    @Test
    @DisplayName("INC y DEC se aceptan con y sin registro")
    void incrementosYDecrementos() throws SintaxisException {
        assertEquals(Forma.SIN_OPERANDOS, una("INC").getForma());
        assertEquals(Forma.REGISTRO, una("INC AX").getForma());
        assertEquals(Forma.SIN_OPERANDOS, una("dec").getForma());
        assertEquals(RegistroID.DX, una("DEC DX").getRegistro(0));
    }

    @Test
    @DisplayName("Las cuatro interrupciones se reconocen con su peso")
    void interrupciones() throws SintaxisException {
        assertEquals(Interrupcion.FIN_PROGRAMA, una("INT 20H").getInterrupcion());
        assertEquals(Interrupcion.PANTALLA, una("INT 10H").getInterrupcion());
        assertEquals(Interrupcion.TECLADO, una("int 09h").getInterrupcion());
        assertEquals(Interrupcion.ARCHIVOS, una("INT 21H").getInterrupcion());
        assertEquals(5, una("INT 21H").getPeso());
    }

    @Test
    @DisplayName("Los saltos guardan su desplazamiento con signo")
    void saltos() throws SintaxisException {
        List<Instruccion> programa = ensamblador.ensamblar(List.of(
                "MOV AX, 1", "CMP AX, BX", "JE +1", "JNE -3", "JMP 0", "INT 20H"));
        assertEquals(1, programa.get(2).getValor(0));
        assertEquals(-3, programa.get(3).getValor(0));
        assertEquals(0, programa.get(4).getValor(0));
    }

    @Test
    @DisplayName("PARAM admite de uno a tres valores y la pila se usa con PUSH y POP")
    void parametrosYPila() throws SintaxisException {
        assertEquals(1, una("PARAM 7").getOperandos().size());
        Instruccion tres = una("PARAM 1,2,-3");
        assertEquals(3, tres.getOperandos().size());
        assertEquals(-3, tres.getValor(2));
        assertEquals(RegistroID.AX, una("PUSH AX").getRegistro(0));
        assertEquals(RegistroID.CX, una("POP CX").getRegistro(0));
    }

    @ParameterizedTest(name = "acepta \"{0}\"")
    @DisplayName("Las lineas bien escritas del plan se aceptan")
    @ValueSource(strings = {
        "mov bx,5", "MOV   BX ,   5", "MOV BX, -8", "INC", "INC AX", "JNE +2",
        "PARAM 7", "PARAM 1,2,3", "INT 09H", "LOAD AX", "STORE BX", "ADD CX", "SUB DX",
        "SWAP AX, BX", "CMP AX, BX", "PUSH DX", "POP AX", "MOV CX, DX"
    })
    void aceptaLineasBienEscritas(String linea) throws SintaxisException {
        // Los saltos necesitan destino dentro del programa, por eso se
        // agregan instrucciones de relleno alrededor.
        List<Instruccion> programa = ensamblador.ensamblar(
                List.of("INC", "INC", "INC", linea, "INC", "INC", "INC"));
        assertEquals(7, programa.size());
    }

    @ParameterizedTest(name = "rechaza \"{0}\"")
    @DisplayName("Las lineas mal escritas del plan se rechazan")
    @ValueSource(strings = {
        "MOV AX,,, 5", "MOV AX , , 5", "MOV, AX, 5", "ADD BX,", ",LOAD AX", "MOV AX 5",
        "MOV AX, BX, CX", "PARAM 1,,2", "PARAM 1, 2, 3, 4", "PARAM", "JMP +-3", "JMP 3,",
        "INT 21", "INT 21H, AX", "SWAP AX", "INC AX, BX", "LOADAX", "INT", "JE",
        "CMP AX, 5", "SWAP AX, 5", "PUSH 5", "POP", "JMP AX", "INC 5", "MOV AX, cinco"
    })
    void rechazaLineasMalEscritas(String linea) {
        assertThrows(SintaxisException.class, () -> ensamblador.ensamblar(List.of(linea)));
    }

    @Test
    @DisplayName("Un codigo de interrupcion desconocido lista los validos")
    void codigoDeInterrupcionDesconocido() {
        String mensaje = errorDe("INT 21");
        assertTrue(mensaje.contains("codigo de interrupcion desconocido \"21\""), mensaje);
        assertTrue(mensaje.contains("20H, 10H, 09H, 21H"), mensaje);
    }

    @Test
    @DisplayName("PARAM con mas de tres valores lo dice")
    void demasiadosParametros() {
        String mensaje = errorDe("PARAM 1, 2, 3, 4");
        assertTrue(mensaje.contains("como maximo 3 parametros"), mensaje);
    }

    @Test
    @DisplayName("Un desplazamiento mal escrito lo dice")
    void desplazamientoInvalido() {
        String mensaje = errorDe("JMP +-3");
        assertTrue(mensaje.contains("desplazamiento invalido"), mensaje);
    }

    @Test
    @DisplayName("En MOV, un segundo operando que no es registro ni numero lo dice")
    void movConOperandoInvalido() {
        String mensaje = errorDe("MOV AX, cinco");
        assertTrue(mensaje.contains("\"cinco\" no es un registro ni un numero"), mensaje);
        assertTrue(mensaje.contains("MOV REG, REG"), mensaje);
        assertTrue(mensaje.contains("MOV REG, NUMERO"), mensaje);
    }

    @Test
    @DisplayName("Un salto que sale del programa se rechaza con su linea")
    void saltoFueraDelPrograma() {
        String adelante = errorDe("MOV AX, 1", "JMP +5", "INT 20H");
        assertTrue(adelante.startsWith("Linea 2: el salto \"JMP +5\" sale del programa"), adelante);

        String atras = errorDe("JNE -2", "INT 20H");
        assertTrue(atras.contains("sale del programa"), atras);
    }

    @Test
    @DisplayName("Un salto a la primera o a la ultima instruccion es valido")
    void saltosEnLosBordes() throws SintaxisException {
        assertEquals(3, ensamblador.ensamblar(List.of("INC", "JMP -2", "INT 20H")).size());
        assertEquals(3, ensamblador.ensamblar(List.of("JMP +1", "INC", "INT 20H")).size());
    }
}
