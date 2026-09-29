; ==========================================================
; registros.asm
; Usa las instrucciones que operan sobre registros y el AC:
; MOV (con registro y con numero), LOAD, STORE, ADD, SUB,
; INC, DEC y SWAP.
; Al final: AX = 13, BX = 7, CX = 4, DX = 7, AC = 12.
; ==========================================================

MOV AX, 7
MOV BX, AX        ; BX = 7
MOV CX, 3
INC CX            ; CX = 4
LOAD AX           ; AC = 7
ADD BX            ; AC = 14
SUB CX            ; AC = 10
INC               ; AC = 11
INC               ; AC = 12
STORE DX          ; DX = 12
DEC DX            ; DX = 11
SWAP AX, DX       ; AX = 11, DX = 7
INC AX
INC AX            ; AX = 13
INT 20H
