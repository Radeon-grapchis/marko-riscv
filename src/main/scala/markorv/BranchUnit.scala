package markorv

import chisel3._
import chisel3.util._

class BranchUnit extends Module {
    val io = IO(new Bundle {
        val bpu_instr = Input(new Bundle {
            val instr = UInt(32.W)
            val pc = UInt(64.W)
        })

        val bpu_result = Output(new Bundle{
            val result = Bool()
            val next_pc = UInt(64.W)
        })
    })

    switch(io.bpu_instr.instr(6,0)) {
        is("b1101111".U) {
            // jal
            io.bpu_result.result := true.B
            io.bpu_result.next_pc := pc + (Cat(io.bpu_instr.instr(31),io.bpu_instr.instr(19,12),io.bpu_instr.instr(20),io.bpu_instr.instr(30,21)).asSInt.pad(64)).asUInt
        }
        is("b1100111".U) {
            // jalr
            // ignore predict and block pipeline for now.
            io.bpu_result.result := true.B
            io.bpu_result.next_pc := 0.U
        }
        default {
            io.bpu_result.result := false.B
            io.bpu_result.next_pc := io.bpu_instr.pc + 4.U
        }
    }
}