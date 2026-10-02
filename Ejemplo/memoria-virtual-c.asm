; ==========================================================
; memoria-virtual-c.asm
; Memoria virtual: cargar juntos memoria-virtual-a.asm,
; memoria-virtual-b.asm, memoria-virtual-c.asm y
; memoria-virtual-d.asm, en ese orden.
; Ocupa 40 posiciones. INT 09H lo deja EN_ESPERA. Cuando a, b
; y c esperan el teclado no hay procesos PREPARADO, y d espera
; en el disco; entonces el sistema suspende al ultimo proceso
; EN_ESPERA: su imagen pasa a la memoria virtual
; (SUSPENDIDO_EN_ESPERA) y deja lugar para d. Con su ENTER pasa
; a SUSPENDIDO_PREPARADO y vuelve cuando haya espacio.
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
