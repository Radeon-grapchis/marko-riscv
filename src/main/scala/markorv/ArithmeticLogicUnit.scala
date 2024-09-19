package markorv

import chisel3._
import chisel3.util._

import markorv.DecoderOutParams

class ArithmeticLogicUnit extends Module {
    val io = IO(new Bundle {
        val alu_instr = Flipped(Decoupled(new Bundle {
            val alu_opcode = UInt(5.W)
            val params = new DecoderOutParams(64)
        }))

        val write_back = Decoupled(new Bundle {
            val reg = Input(UInt(5.W))
            val data = Input(UInt(64.W))
        })
    })

    io.alu_instr.ready := io.write_back.ready
    io.write_back.valid := false.B
    io.write_back.bits.reg := 0.U
    io.write_back.bits.data := 0.U

    val ALU_ADD = "b00001".U
    val ALU_SUB = "b00010".U

    switch(io.alu_instr.bits.alu_opcode) {
        is(ALU_ADD) {

        }
        is(ALU_SUB) {

        }
    }
}