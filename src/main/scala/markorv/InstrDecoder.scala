package markorv

import chisel3._
import chisel3.util._

class DecoderOutParams(data_width: Int) extends Bundle {
  val immediate = SInt(data_width.W)
  val rs1 = UInt(5.W)
  val rs2 = UInt(5.W)
  val rd = UInt(5.W)
}

class InstrDecoder(data_width: Int = 64, addr_width: Int = 64) extends Module {
    val io = IO(new Bundle {
        val pc_in = Input(UInt(addr_width.W))
        val instr = Flipped(Decoupled(UInt(32.W)))

        val lsu_out = Decoupled(new Bundle {
            // {Mem:0/Imm:1}[3]{SInt:0/UInt:1}[2]{Size}[1,0]
            val lsu_opcode = UInt(4.W)
            val params = new DecoderOutParams(data_width)
        })
    })

    val opcode = Wire(UInt(7.W))
    opcode := io.instr.bits(6,0)

    io.instr.ready := io.lsu_out.ready

    io.lsu_out.valid := false.B
    io.lsu_out.bits.lsu_opcode := 0.U(4.W)
    io.lsu_out.bits.params.immediate := 0.S(data_width.W)
    io.lsu_out.bits.params.rs1 := 0.U(5.W)
    io.lsu_out.bits.params.rs2 := 0.U(5.W)
    io.lsu_out.bits.params.rd := 0.U(5.W)

    when(io.instr.valid) {
        switch(opcode) {
            is("b0000011".U) {
                // Load Memory
                io.lsu_out.bits.lsu_opcode := Cat(0.U(1.W), opcode(14,12))
                io.lsu_out.bits.params.immediate := opcode(31,20).asSInt
                io.lsu_out.bits.params.rs1 := opcode(19,15)
                io.lsu_out.bits.params.rd := opcode(11,7)

                io.lsu_out.valid := true.B
            }
        }
    }
}