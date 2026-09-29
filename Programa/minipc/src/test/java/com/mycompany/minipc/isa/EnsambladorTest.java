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
 * Pruebas del ensamblador.
 *
 * La prueba central reproduce el programa de ejemplo del enunciado y
 * verifica que las siete lineas den la operacion, el registro y el operando
 * esperados.
 */
class EnsambladorTest {

    private Ensamblador ensamblador;

    @BeforeEach
    void prepararEnsamblador() {
        ensamblador = new Ensamblador();
    }

    /** Las siete lineas del archivo de ejemplo del enunciado. */
    private static List<String> programaDelEnunciado() {
        return List.of(
                "MOV AX, 5",
                "MOV BX, 3",
                "LOAD AX",
                "ADD BX",
                "SUB AX",
                "STORE AX",
                "MOV BX, -8");
    }

    @Test
    @DisplayName("El programa del enunciado produce las siete instrucciones esperadas")
    void ensamblaElProgramaDelEnunciado() throws SintaxisException {
        List<Instruccion> programa = ensamblador.ensamblar(programaDelEnunciado());

        assertEquals(7, programa.size());
        verificar(programa.get(0), OpCode.MOV, RegistroID.AX, 5);
        verificar(programa.get(1), OpCode.MOV, RegistroID.BX, 3);
        verificar(programa.get(2), OpCode.LOAD, RegistroID.AX, 0);
        verificar(programa.get(3), OpCode.ADD, RegistroID.BX, 0);
        verificar(programa.get(4), OpCode.SUB, RegistroID.AX, 0);
        verificar(programa.get(5), OpCode.STORE, RegistroID.AX, 0);
        verificar(programa.get(6), OpCode.MOV, RegistroID.BX, -8);
    }

    private static void verificar(Instruccion i, OpCode op, RegistroID reg, int operando) {
        assertEquals(op, i.getOpcode(), i.toString());
        assertEquals(reg, i.getRegistro(), i.toString());
        assertEquals(operando, i.getOperando(), i.toString());
    }

    @Test
    @DisplayName("Cada instruccion recuerda su numero de linea en el archivo")
    void conservaElNumeroDeLinea() throws SintaxisException {
        List<Instruccion> programa = ensamblador.ensamblar(programaDelEnunciado());
        for (int i = 0; i < programa.size(); i++) {
            assertEquals(i + 1, programa.get(i).getNumeroLinea());
        }
    }

    @Test
    @DisplayName("Las lineas vacias y los comentarios no ocupan memoria")
    void descartaVaciasYComentarios() throws SintaxisException {
        List<Instruccion> programa = ensamblador.ensamblar(List.of(
                "; programa de prueba",
                "",
                "MOV AX, 5   ; carga inicial",
                "   ",
                "// suma el segundo registro",
                "MOV BX, 3",
                "ADD BX"));

        assertEquals(3, programa.size());
        assertEquals("MOV AX, 5", programa.get(0).getTextoFuente());
        assertEquals(3, programa.get(0).getNumeroLinea());
        assertEquals(6, programa.get(1).getNumeroLinea());
        assertEquals(7, programa.get(2).getNumeroLinea());
    }

    @Test
    @DisplayName("Los espacios alrededor de la coma y las mayusculas no importan")
    void aceptaVariantesDeEscritura() throws SintaxisException {
        List<Instruccion> programa = ensamblador.ensamblar(List.of(
                "mov ax,5",
                "MOV   bx ,   3",
                "  load   Ax  "));

        assertEquals(3, programa.size());
        verificar(programa.get(0), OpCode.MOV, RegistroID.AX, 5);
        verificar(programa.get(1), OpCode.MOV, RegistroID.BX, 3);
        verificar(programa.get(2), OpCode.LOAD, RegistroID.AX, 0);
    }

    @Test
    @DisplayName("El archivo de errores del enunciado reporta los tres problemas juntos")
    void reportaTodosLosErroresDeUnaVez() {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of(
                        "MOV AX, 5",
                        "JUMP 100",
                        "ADD EX",
                        "MOV BX, tres")));

        assertEquals(3, e.cantidad());
        assertTrue(e.getErrores().get(0).contains("Linea 2"), e.getErrores().get(0));
        assertTrue(e.getErrores().get(0).contains("JUMP"), e.getErrores().get(0));
        assertTrue(e.getErrores().get(1).contains("Linea 3"), e.getErrores().get(1));
        assertTrue(e.getErrores().get(1).contains("EX"), e.getErrores().get(1));
        assertTrue(e.getErrores().get(2).contains("Linea 4"), e.getErrores().get(2));
        assertTrue(e.getErrores().get(2).contains("tres"), e.getErrores().get(2));
    }

    @Test
    @DisplayName("Falta el registro")
    void detectaRegistroFaltante() {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of("LOAD")));
        assertTrue(e.getMessage().contains("falta el registro"), e.getMessage());
    }

    @Test
    @DisplayName("Falta el valor inmediato de MOV")
    void detectaInmediatoFaltante() {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of("MOV AX")));
        assertTrue(e.getMessage().contains("faltan operandos para MOV"), e.getMessage());
        assertTrue(e.getMessage().contains("MOV REG, NUMERO"), e.getMessage());
    }

    @ParameterizedTest(name = "rechaza \"{0}\"")
    @DisplayName("Las comas mal colocadas se rechazan")
    @ValueSource(strings = {
        "MOV AX,,, 5",
        "MOV AX , , 5",
        "MOV AX ,, 5",
        "MOV, AX, 5",
        "MOV,AX,5",
        "ADD BX,",
        "MOV AX, 5,",
        ",LOAD AX",
        ", MOV AX, 5"
    })
    void rechazaComasMalColocadas(String linea) {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of(linea)));
        assertTrue(e.getMessage().contains("coma"), e.getMessage());
    }

    @ParameterizedTest(name = "rechaza \"{0}\"")
    @DisplayName("Las lineas mal formadas se rechazan aunque tengan pocas comas")
    @ValueSource(strings = {
        "MOV AX 5",
        "MOV AX, 5, 6",
        "MOV AX, BX, CX",
        "ADD BX 5",
        "ADD BX, CX",
        "LOADAX",
        "LOAD 5",
        "MOV 5, AX",
        "MOV AX, 5 6",
        "MOV AX, 5x"
    })
    void rechazaLineasMalFormadas(String linea) {
        assertThrows(SintaxisException.class, () -> ensamblador.ensamblar(List.of(linea)));
    }

    @Test
    @DisplayName("Si solo falta la coma, el mensaje lo dice")
    void detectaComaFaltante() {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of("MOV AX 5")));
        assertTrue(e.getMessage().contains("falta la coma"), e.getMessage());
    }

    @Test
    @DisplayName("Las tres comas seguidas de la Tarea 1 se reportan con numero de linea")
    void reportaLasTresComas() {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of("MOV BX, 3", "MOV AX,,, 5")));
        assertEquals(1, e.cantidad());
        assertTrue(e.getErrores().get(0).startsWith("Linea 2: comas mal colocadas"),
                e.getErrores().get(0));
    }

    @ParameterizedTest(name = "acepta \"{0}\"")
    @DisplayName("Las lineas bien escritas se aceptan")
    @ValueSource(strings = {
        "mov bx,5",
        "MOV   BX ,   5",
        "MOV BX, -8",
        "MOV BX, +8",
        "Load Ax",
        "STORE DX",
        "ADD\tCX"
    })
    void aceptaLineasBienEscritas(String linea) throws SintaxisException {
        assertEquals(1, ensamblador.ensamblar(List.of(linea)).size());
    }

    @Test
    @DisplayName("Sobran operandos en una operacion que no lleva inmediato")
    void detectaOperandosSobrantes() {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of("ADD BX 5")));
        assertTrue(e.getMessage().contains("sobran operandos"), e.getMessage());
    }

    @Test
    @DisplayName("El valor inmediato tiene que ser numerico")
    void detectaValorNoNumerico() {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of("MOV AX, cinco")));
        assertTrue(e.getMessage().contains("valor no numerico"), e.getMessage());
    }

    @Test
    @DisplayName("Un archivo sin instrucciones se rechaza")
    void rechazaArchivoSinInstrucciones() {
        SintaxisException e = assertThrows(SintaxisException.class,
                () -> ensamblador.ensamblar(List.of("; solo comentarios", "", "   ")));
        assertTrue(e.getMessage().contains("no contiene ninguna instruccion"), e.getMessage());
    }

    @Test
    @DisplayName("Los valores negativos del rango se aceptan")
    void aceptaNegativos() throws SintaxisException {
        List<Instruccion> programa = ensamblador.ensamblar(List.of(
                "MOV DX, -25",
                "MOV CX, -127"));
        assertEquals(-25, programa.get(0).getOperando());
        assertEquals(-127, programa.get(1).getOperando());
    }
}
