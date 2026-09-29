; ==========================================================
; pila.asm
; PARAM guarda los parametros en la pila en el orden escrito;
; POP los saca del ultimo al primero. PUSH guarda un registro.
; Al final: AX = 30, BX = 20, CX = 10, DX = 8.
; ==========================================================

PARAM 10, 20, 30
POP AX            ; AX = 30
POP BX            ; BX = 20
POP CX            ; CX = 10
MOV DX, 8
PUSH DX
MOV DX, 0
POP DX            ; DX vuelve a 8
INT 20H
