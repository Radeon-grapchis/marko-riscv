.section .text
.global _start
_start:
    li t0, 0x55
    csrrw a0, 0x800, t0
    li t0, 0x0F
    csrrs a0, 0x800, t0
    li t0, 0x0F
    csrrc a0, 0x800, t0

    csrrsi a0, 0x800, 0x3
    csrrci a0, 0x800, 0x2
    csrrwi a0, 0x800, 0x1F

    csrr a0, 0x800
end:
    j end
