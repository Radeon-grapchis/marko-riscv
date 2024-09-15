package markorv

import chisel3._
import chisel3.util._

class InstrDecoder(data_width: Int = 64, addr_width: Int = 64) extends Module {
    val io = IO(new Bundle {
        val pc_in = Input(UInt(addr_width.W))
        val instr = Flipped(Decoupled(UInt(32.W)))

        // {Mem:0/Imm:1}[3]{SInt:0/UInt:1}[2]{Size}[1,0]
        val lsu_opcode = Decoupled(UInt(3.W))
        val alu_opcode = Decoupled(UInt(3.W))

        val immediate = Output(SInt(data_width.W))
        val rs1 = Output(UInt(5.W))
        val rs2 = Output(UInt(5.W))
        val rd = Output(UInt(5.W))
    })

    val opcode = Wire(UInt(7.W))

    opcode := io.instr.bits(6,0)
    io.instr.ready := io.lsu_opcode.ready & io.alu_opcode.ready
    io.lsu_opcode.bits := 0.U(3.W)
    io.alu_opcode.bits := 0.U(3.W)
    io.lsu_opcode.valid := false.B
    io.alu_opcode.valid := false.B
    io.rs1 := 0.U(5.W)
    io.rs2 := 0.U(5.W)
    io.rd := 0.U(5.W)

    when(io.instr.valid) {
        switch(opcode) {
            is("b0000011".U) {
                // Load Memory
                io.lsu_opcode := Cat(0.U(1.W), opcode(14,12))
                io.immediate := opcode(31,20).asSInt
                io.rs1 := opcode(19,15)
                io.rd := opcode(11,7)

                io.lsu_opcode.valid := true.B
            }
        }
    }
}