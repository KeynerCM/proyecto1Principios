; ==========================================================
; pantalla.asm
; INT 10H imprime en la pantalla el valor de DX.
; Imprime 7, 14 y 21 (una tabla del 7).
; Pesos: MOV 1, INT 10H 2, ADD 3, STORE 2, INT 20H 2.
; ==========================================================

MOV DX, 7
MOV BX, 7
INT 10H           ; pantalla: 7
LOAD DX
ADD BX
STORE DX
INT 10H           ; pantalla: 14
LOAD DX
ADD BX
STORE DX
INT 10H           ; pantalla: 21
INT 20H
