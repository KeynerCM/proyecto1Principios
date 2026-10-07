; ==========================================================
; programa-largo.asm
; Ejercita las cinco operaciones y los cuatro registros.
; 35 instrucciones.
; ==========================================================

; --- Bloque 1: carga inicial de los cuatro registros ---
MOV AX, 10
MOV BX, 20
MOV CX, 30
MOV DX, 40

; --- Bloque 2: suma acumulada de los cuatro ---
LOAD AX
ADD BX
ADD CX
ADD DX
STORE AX

; --- Bloque 3: restas sucesivas ---
LOAD AX
SUB BX
SUB CX
SUB DX
STORE BX

; --- Bloque 4: valores negativos ---
MOV CX, -25
MOV DX, -50
LOAD CX
ADD DX
STORE CX

; --- Bloque 5: valores grandes y negativos ---
MOV AX, 127
LOAD AX
SUB AX
STORE DX
MOV BX, -127
LOAD BX
STORE AX

; --- Bloque 6: resultado final ---
MOV AX, 7
MOV BX, 14
LOAD AX
ADD BX
STORE CX
MOV DX, -3
LOAD CX
ADD DX
STORE DX
