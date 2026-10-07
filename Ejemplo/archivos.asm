; ==========================================================
; archivos.asm
; INT 21H maneja archivos. AH elige el servicio, DX apunta al
; nombre del archivo y AL lleva el byte que se lee o escribe:
;   3Ch crear, 3Dh abrir, 4Dh leer, 40h escribir, 41h eliminar
; MOV DX, "nombre" deja en DX la direccion de memoria donde
; queda guardado el nombre (la celda de esa instruccion).
; Crea notas.txt, escribe 72 y 105, lo vuelve a leer y
; muestra los dos valores en la pantalla. El archivo queda en
; el disco, con su entrada en el indice.
; ==========================================================

MOV DX, "notas.txt"
MOV AH, 3Ch       ; crear
INT 21H
MOV AH, 3Dh       ; abrir
INT 21H
MOV AL, 72
MOV AH, 40h       ; escribir 72
INT 21H
MOV AL, 105
INT 21H           ; escribir 105
MOV AH, 4Dh       ; leer: AL = 72
INT 21H
MOV CX, AL
INT 21H           ; leer: AL = 105
MOV BX, AL
SWAP DX, CX       ; DX = 72 para imprimir; CX guarda el puntero al nombre
INT 10H           ; pantalla: 72
MOV DX, BX
INT 10H           ; pantalla: 105
INT 20H
