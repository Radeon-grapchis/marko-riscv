package markorv

import chisel3._
import chisel3.util._

class InstrIPBundle() extends Bundle {
    val instr = Output(UInt(32.W))
    val pc = Output(UInt(addr_width.W))
}

class InstrFetchUnit() extends Module {
    val io = IO(new Bundle {
        val fetch_task = Flipped(Decoupled(new Bundle {
            val instr = UInt(32.W)
            val pred = Bool()
            val pred_addr = UInt(64.W)
        }))

        val instr_bundle = Decoupled(new InstrIPBundle)

        val peek_pc = Output(UInt(addr_width.W))
        val pc_in = Input(UInt(addr_width.W))
        val set_pc = Input(Bool())
    })

    val pc = RegInit(0.U(addr_width.W))

    io.peek_pc := pc

    when(io.set_pc) {
        pc := io.pc_in
    }.otherwise {
        pc := next_pc
    }
    io.instr_bundle.bits.pc := pc
}