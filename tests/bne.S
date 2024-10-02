addi x2, x0, 16
loop:
sb x1, 1016(x0)
addi x1, x1, 1
bne x1, x2, loop
sb x0, 1016(x0)