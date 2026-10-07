; ==========================================================
; teclado.asm
; INT 09H pide un valor al teclado (0 a 255). El proceso pasa
; a EN_ESPERA y la CPU lo espera hasta el ENTER: en este
; proyecto no pasa a otro proceso mientras tanto. Con el ENTER
; el valor llega a DX y el proceso vuelve a PREPARADO.
; Despues imprime el doble del valor con INT 10H.
; ==========================================================

INT 09H           ; pantalla: >> Ingresar valor:
LOAD DX           ; AC = valor leido
ADD DX            ; AC = el doble
STORE DX
INT 10H           ; pantalla: el doble del valor
INT 20H
