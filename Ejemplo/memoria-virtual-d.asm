; ==========================================================
; memoria-virtual-d.asm
; Memoria virtual: cargar despues de memoria-virtual-a.asm,
; memoria-virtual-b.asm y memoria-virtual-c.asm.
; Ocupa 24 posiciones. Con la memoria de 256 la zona de
; usuario es de 128; a, b y c ya usan 120, asi que este
; programa no cabe y el planificador de trabajos lo admite en
; la memoria virtual del disco (NUEVO -> SUSPENDIDO_PREPARADO).
; Cuando a termina y libera sus 40 posiciones, el intercambio
; lo trae a la memoria principal (SUSPENDIDO_PREPARADO ->
; PREPARADO) y se ejecuta despues de c, en orden de llegada.
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
