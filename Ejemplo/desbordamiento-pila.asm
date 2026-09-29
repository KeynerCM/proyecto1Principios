; ==========================================================
; desbordamiento-pila.asm
; La pila tiene capacidad para 5 valores. Los primeros cinco
; PUSH funcionan; el sexto provoca un desbordamiento de pila
; y el proceso queda en BLOQUEADO_ERROR.
; ==========================================================

MOV AX, 1
PUSH AX
PUSH AX
PUSH AX
PUSH AX
PUSH AX
PUSH AX           ; sexto valor: desbordamiento
INT 20H
