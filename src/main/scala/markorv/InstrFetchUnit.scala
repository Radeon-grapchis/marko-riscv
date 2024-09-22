package markorv

import chisel3._
import chisel3.util._

import markorv.FetchQueueEntities

class InstrIPBundle extends Bundle {
    val instr = Output(UInt(32.W))
    val pc = Output(UInt(64.W))
    val recovery_pc = Output(UInt(64.W))
}

class InstrFetchUnit extends Module {
    val io = IO(new Bundle {
        val fetch_bundle = Flipped(Decoupled(new FetchQueueEntities))

        val instr_bundle = Decoupled(new InstrIPBundle)

        val peek_pc = Output(UInt(64.W))
        val pc_in = Input(UInt(64.W))
        val set_pc = Input(Bool())
    })

    val pc = RegInit(0.U(64.W))
    val next_pc = Wire(UInt(64.W))

    val instr_buffer = Reg(new FetchQueueEntities)
    val buffer_valid = RegInit(false.B)
    val buffer_at = RegInit(0.U(1.W))
    io.instr_bundle.valid := false.B
    io.instr_bundle.bits.instr := 0.U(32.W)
    io.instr_bundle.bits.pc := pc
    io.instr_bundle.bits.recovery_pc := pc

    io.fetch_bundle.ready := false.B
    io.peek_pc := pc

    when(!buffer_valid) {
        // get new item
        io.fetch_bundle.ready := true.B
        when(io.fetch_bundle.valid) {
            instr_buffer := io.fetch_bundle.bits
            buffer_valid := true.B
            buffer_at := 0.U
        }
        next_pc := pc
    }.otherwise {
        io.instr_bundle.valid := true.B

        io.instr_bundle.bits.instr := instr_buffer.items(buffer_at).instr
        io.instr_bundle.bits.recovery_pc := instr_buffer.items(buffer_at).recovery_pc
        when(io.instr_bundle.ready) {
            buffer_at := buffer_at + 1.U
            buffer_valid := (buffer_at =/= 1.U)
            next_pc := instr_buffer.items(buffer_at).pred_pc
        }.otherwise {
            next_pc := pc
        }
    }

    when(io.set_pc) {
        pc := io.pc_in
        buffer_at := 0.U
        buffer_valid := false.B 
    }.otherwise {
        pc := next_pc
    }
}