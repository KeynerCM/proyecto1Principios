; ==========================================================
; error-comas.asm
; Demuestra la validacion de sintaxis con expresiones regulares.
; Solo la primera linea es valida; cada una de las demas se
; rechaza con su numero de linea. Entre dos operandos va
; exactamente una coma.
; ==========================================================

MOV AX, 5
MOV AX,,, 5
MOV, BX, 3
ADD BX,
,LOAD AX
MOV CX 7
MOV DX, 1, 2
