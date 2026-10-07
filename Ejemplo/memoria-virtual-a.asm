; ==========================================================
; memoria-virtual-a.asm
; Memoria virtual: cargar juntos memoria-virtual-a.asm,
; memoria-virtual-b.asm, memoria-virtual-c.asm y
; memoria-virtual-d.asm, en ese orden.
; Ocupa 40 posiciones: a, b y c juntos llenan 120 de las 128
; de la zona de usuario, y por eso d va a la memoria virtual.
; INT 09H pide un valor; la CPU espera el ENTER antes de seguir
; con este mismo proceso, e INT 10H lo imprime.
; ==========================================================

INT 09H           ; pantalla: >> Ingresar valor:
INT 10H           ; imprime el valor recibido en DX
MOV AX, 0
; --- relleno: 36 instrucciones para ocupar memoria ---
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INC AX
INT 20H
