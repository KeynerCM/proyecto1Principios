; ==========================================================
; memoria-virtual-d.asm
; Memoria virtual: cargar despues de memoria-virtual-a.asm,
; memoria-virtual-b.asm y memoria-virtual-c.asm.
; Ocupa 24 posiciones. Con la memoria de 256 la zona de
; usuario es de 128; a, b y c ya usan 120, asi que este
; programa no cabe y el planificador de trabajos lo admite en
; la memoria virtual del disco (NUEVO -> SUSPENDIDO_PREPARADO).
; Cuando a, b y c esperan el teclado, c se suspende y este
; programa entra a la memoria en su lugar.
; Cuenta hasta 20 en CX y lo imprime.
; ==========================================================

MOV CX, 0
; --- 20 incrementos ---
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
INC CX
MOV DX, CX
INT 10H           ; pantalla: 20
INT 20H
