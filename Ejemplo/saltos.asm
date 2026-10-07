; ==========================================================
; saltos.asm
; Bucle con CMP y JNE: cuenta en CX de 1 a 5 y suma en AC.
; El desplazamiento se cuenta desde la instruccion siguiente
; al salto: JNE -6 vuelve a INC CX.
; Al final: CX = 5, AC = 15 (1 + 2 + 3 + 4 + 5).
; ==========================================================

MOV CX, 0
MOV DX, 5
INC CX            ; inicio del bucle
LOAD AX
ADD CX
STORE AX          ; AX acumula la suma
CMP CX, DX
JNE -6            ; si CX distinto de 5, vuelve a INC CX
JMP +1            ; salta la linea siguiente
MOV AX, 0         ; nunca se ejecuta
LOAD AX
INT 20H
