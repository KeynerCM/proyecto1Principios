# Proyecto 1: Gestor de Procesos (Mini PC)
## Integrantes:
### 2024108270 Keyner Cerdas Morales

### Estado del proyecto: 1
### Enlace del video: PENDIENTE (agregar el enlace de YouTube)

**Curso:** IC-6600 Principios de Sistemas Operativos, Centro Académico Limón
**Profesor:** Ing. Cristian Campos Agüero

---

## Descripción

Simulador de una minicomputadora con CPU, memoria principal, disco, pantalla y teclado, y de un sistema operativo que administra la ejecución de varios programas escritos en un mini ensamblador (`.asm`).

Al cargar uno o varios archivos, el sistema:

1. valida la sintaxis de cada línea con expresiones regulares;
2. guarda los programas en el disco, con su entrada en el índice;
3. los pone en la **lista de trabajos** (hasta 20);
4. los admite como **procesos** (hasta 5), cada uno con su **BCP en la memoria del kernel**;
5. los ejecuta con planificación **FCFS**, pasando por el **despachador** y el **cambio de contexto**.

Cada instrucción dura tantos segundos de CPU como su peso. La ejecución puede avanzar de a un segundo con **Siguiente** o de forma **automática** con **Ejecutar**. Al final se muestran las estadísticas de cada proceso.

El diseño sigue los conceptos de Stallings, *Operating Systems: Internals and Design Principles*, 9.ª edición: ciclo de instrucción, interrupciones, modelo de 7 estados, BCP, despachador, cambio de contexto, memoria virtual y planificación.

## Requisitos

- **JDK 17** o superior.
- **Maven** (NetBeans lo trae incorporado). La única dependencia externa es FlatLaf 3.7.2 (apariencia de la interfaz), que Maven descarga sola. Si no estuviera disponible, el programa usa la apariencia del sistema.
- Sistema operativo con entorno gráfico.

## Instrucciones de ejecución

### Desde NetBeans

1. `File` -> `Open Project...` y seleccionar la carpeta `Programa/minipc`.
2. Clic derecho sobre el proyecto -> `Run`.

### Desde la terminal

```bash
cd Programa/minipc
mvn clean package
mvn exec:java
```

### Pruebas automáticas

```bash
cd Programa/minipc
mvn test
```

## Uso

| Botón | Atajo | Qué hace |
|---|---|---|
| **Cargar archivos** | Ctrl+O | Abre el selector para elegir uno o varios `.asm` (Ctrl o Shift + clic, o el botón "Seleccionar todos los .asm"). Los valida, los guarda en el disco y los agrega a la lista de trabajos. Volver a usarlo agrega más trabajos. |
| **Ejecutar** | F5 | Ejecuta todos los procesos de forma automática hasta su finalización. |
| **Siguiente** | F8 | Avanza un segundo de CPU. |
| **Pausar** | | Detiene la ejecución automática. |
| **Reiniciar** | | Devuelve todos los trabajos a NUEVO para ejecutarlos de nuevo. |
| **Limpiar** | | Vacía la lista de trabajos, la memoria, el disco y la pantalla. |
| **Estadísticas** | | Muestra el resumen de la ejecución. También se abre solo al terminar el último proceso. |
| **Configuración** | | Tamaño de la memoria, del disco y de la memoria virtual, velocidad de la ejecución automática y algoritmo de planificación. |

La ventana muestra:
- la lista de trabajos con su estado;
- la lista de procesos en memoria y sus colas (en CPU, preparados, en espera y suspendidos);
- el BCP del proceso en ejecución con sus registros, IR, AC, PC y pila;
- la memoria principal y el disco celda por celda;
- la pantalla con el teclado, la consola del sistema operativo y el programa en ejecución;
- el reloj simulado.

## Configuración

Los valores no quedan en el código: se leen del archivo [`Programa/minipc/config.properties`](Programa/minipc/config.properties), que se puede editar a mano o desde el menú Configuración.

| Clave | Por defecto | Significado |
|---|---:|---|
| `memoria.tamano` | 256 | Celdas de la memoria principal (mínimo 160: 128 del kernel y 32 para programas) |
| `disco.tamano` | 512 | Celdas del disco (las primeras 20 son el índice) |
| `disco.memoriaVirtual` | 64 | Celdas al final del disco reservadas para la memoria virtual |
| `ejecucion.msPorSegundo` | 1000 | Milisegundos reales que dura cada segundo de CPU en la ejecución automática |
| `planificacion.algoritmo` | FCFS | Algoritmo de planificación de procesos |

## Formato del archivo `.asm`

- Una instrucción por línea. Las líneas vacías se ignoran.
- Comentarios con `;` o `//`, al inicio de la línea o después de una instrucción.
- Los operandos se separan con **exactamente una coma**: `MOV AX, 5`. Formas como `MOV AX 5` o `MOV AX,, 5` se rechazan.
- No distingue mayúsculas de minúsculas.
- Los números pueden ser decimales (`-8`) o hexadecimales con sufijo `h` (`3Ch`).
- Registros: `AX`, `BX`, `CX`, `DX`, y `AH` y `AL` (mitades alta y baja de AX).

Los errores de sintaxis se reportan todos juntos, cada uno con su número de línea y un mensaje claro.

## Juego de instrucciones

| Instrucción | Peso | Descripción |
|---|---:|---|
| `LOAD reg` | 2 | AC = reg |
| `STORE reg` | 2 | reg = AC |
| `MOV reg, reg` / `MOV reg, num` | 1 | Copia un valor al registro |
| `MOV DX, "archivo.txt"` | 1 | Deja en DX la posición del nombre del archivo (para `INT 21H`) |
| `ADD reg` | 3 | AC = AC + reg |
| `SUB reg` | 3 | AC = AC - reg |
| `INC` / `INC reg` | 1 | Suma 1 al AC o al registro |
| `DEC` / `DEC reg` | 1 | Resta 1 al AC o al registro |
| `SWAP reg, reg` | 1 | Intercambia los valores de dos registros |
| `INT 20H` | 2 | Finaliza el programa |
| `INT 10H` | 2 | Imprime en la pantalla el valor de DX |
| `INT 09H` | variable | Pide un valor al teclado (0 a 255) y lo guarda en DX al presionar ENTER |
| `INT 21H` | 5 | Archivos: AH = 3Ch crear, 3Dh abrir, 4Dh leer, 40h escribir, 41h eliminar; el byte va en AL |
| `JMP ±desp` | 2 | Salta según el desplazamiento |
| `CMP reg, reg` | 2 | Compara dos registros |
| `JE ±desp` / `JNE ±desp` | 2 | Salta si eran iguales / distintos |
| `PARAM v1[, v2[, v3]]` | 3 | Guarda hasta 3 valores en la pila |
| `PUSH reg` | 1 | Guarda el registro en la pila |
| `POP reg` | 1 | Saca un valor de la pila al registro |

## Diseño

Resumen de la estructura. La explicación completa y el diagrama de paquetes están en el PDF de diseño.

- **Paquetes:**
  - `hardware`: CPU, memoria, disco, pantalla, registros y pila;
  - `isa`: instrucciones y ensamblador;
  - `so`: sistema operativo, con los subpaquetes:
    - `trabajos`: lista de trabajos y planificador de trabajos;
    - `procesos`: BCP, proceso y lista de procesos;
    - `planificacion`: algoritmos;
    - `despacho`: despachador y cambio de contexto;
    - `memoria`: memoria, memoria virtual e intercambio;
    - `interrupciones`;
    - `archivos`;
  - `gui`: interfaz;
  - `config`: configuración externa.
- **Memoria y kernel:** la memoria es un arreglo de celdas de texto. El tamaño del kernel se calcula como K = 3 + 5 x 25 = 128 (cabecera + 5 BCP de 25 campos). Los programas van en la zona de usuario (128 a 255).
- **BCP en memoria:** la información de cada proceso vive en las celdas del kernel, no en objetos de Java. Cada elemento de la pila ocupa su propia celda. Los BCP forman una lista enlazada con el campo "siguiente BCP".
- **Estados:** NUEVO, PREPARADO, EJECUCION, EN_ESPERA, SUSPENDIDO_PREPARADO, SUSPENDIDO_EN_ESPERA y FINALIZADO.
- **Planificación:** FCFS con el patrón Estrategia, para poder agregar otros algoritmos en el Proyecto 2 sin cambiar el resto del sistema.
- **Teclado:** con `INT 09H` el proceso pasa a EN_ESPERA y la CPU lo espera hasta el ENTER. Los procesos se ejecutan de uno en uno, en orden de llegada.
- **Memoria virtual:** si un proceso admitido no cabe en la memoria principal, su programa pasa al área de memoria virtual del disco (SUSPENDIDO_PREPARADO) y vuelve a la memoria cuando se libera espacio.
- **Disco:** índice en las primeras 20 celdas (`nombre|inicio|tamaño`), luego el área de archivos y al final la memoria virtual.
- **Protección y seguridad:**
  - cada proceso solo puede ejecutar dentro de su región (registros base y alcance);
  - el kernel no se puede leer desde un programa;
  - DX es relativo a la base del programa;
  - `INT 21H` no puede borrar programas;
  - un error termina solo al proceso que lo causó.
- **Tiempo:** reloj simulado que avanza un segundo por cada segundo de CPU; las estadísticas se miden con él.

## Programas de ejemplo

En la carpeta [`Ejemplo/`](Ejemplo/):

| Archivo | Qué muestra |
|---|---|
| `file.asm` | El ejemplo del enunciado |
| `ejemplo2.asm` | Programa corto |
| `registros.asm` | MOV, LOAD, STORE, ADD, SUB, INC, DEC y SWAP |
| `saltos.asm` | Bucle con CMP y JNE |
| `pila.asm` | PARAM, PUSH y POP |
| `desbordamiento-pila.asm` | Desbordamiento de la pila de 5 posiciones |
| `pantalla.asm` | INT 10H |
| `teclado.asm` | INT 09H |
| `archivos.asm` | INT 21H: crea, escribe y lee un archivo |
| `programa-largo.asm` | Programa de 35 instrucciones |
| `error-comas.asm`, `error-sintaxis.asm` | Mensajes de error de la validación |
| `memoria-virtual-a.asm` a `memoria-virtual-d.asm` | Cargados juntos: el último no cabe en memoria y pasa a la memoria virtual |

## Objetivos alcanzados

- Carga de uno o varios archivos `.asm`, con validación de sintaxis por expresiones regulares y mensajes claros con número de línea.
- Programas guardados en el disco, con índice en sus primeras posiciones.
- Lista de trabajos, planificador de trabajos y lista de procesos enlazada en memoria.
- BCP completo en la memoria del kernel, con tamaño del kernel calculado: estados, PC, registros AC, AX, BX, CX, DX, IR, pila de 5 con control de desbordamiento, información contable (CPU, tiempo de inicio, tiempo empleado), archivos abiertos, enlace al siguiente BCP, base, alcance y prioridad.
- Los 7 estados del proceso.
- Ejecución de las instrucciones según su peso, en los dos modos (Siguiente y Ejecutar).
- Interrupciones y llamadas al sistema: INT 20H, 10H, 09H y 21H (manejo de archivos completo).
- Pantalla y teclado simulados.
- Despachador y cambio de contexto, con cada paso en la consola del sistema operativo.
- Planificación FCFS modular.
- Memoria virtual: los procesos que no caben en memoria pasan al disco y vuelven cuando hay espacio.
- Protección y seguridad: base y alcance, kernel inaccesible, DX relativo y programas protegidos.
- Visualización del BCP actual, de cómo se guardan los BCP en memoria, de los registros (IR, AC, PC), de la lista de trabajos y su estado, y del tiempo de ejecución.
- Configuración externa en un archivo y menú de configuración.
- Estadísticas al final: proceso, hora de inicio, hora final y duración en segundos, además del tiempo de CPU, la espera y la ocupación máxima de la memoria.
- 280 pruebas automáticas con JUnit 5.

## Objetivos no alcanzados

Todos los requisitos del enunciado están implementados.

- Por indicación del profesor, en este proyecto la CPU espera al proceso que pidió el teclado en lugar de pasar a otro. Por eso la transición EN_ESPERA -> SUSPENDIDO_EN_ESPERA (suspender un proceso bloqueado para traer otro) está implementada y probada, pero no se usa todavía; queda lista para el Proyecto 2.
- Los algoritmos SPN, SRT, RR y HRRN corresponden al Proyecto 2; el diseño ya permite agregarlos.

## Referencia

Stallings, W. *Operating Systems: Internals and Design Principles*, 9.ª edición. Capítulos 1 (ciclo de instrucción e interrupciones), 3 (procesos, estados y BCP), 7 (gestión de memoria) y 9 (planificación).
