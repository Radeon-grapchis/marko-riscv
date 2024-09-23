package markorv

import chisel3._
import chisel3.util._

import markorv.FetchQueueEntities

class InstrIPBundle extends Bundle {
    val instr = Output(UInt(32.W))
    val pred_taken = Output(Bool())
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

    val instr_buffer_now = RegInit(0.U(1.W))
    val instr_buffer = Reg(Vec(2, new FetchQueueEntities))
    val buffer_valid = RegInit(false.B)
    val buffer_at = RegInit(0.U(1.W))

    val prefetch_flag = RegInit(false.B)

    // init default values
    io.instr_bundle.valid := false.B
    io.instr_bundle.bits.instr := 0.U(32.W)
    io.instr_bundle.bits.pred_taken := false.B
    io.instr_bundle.bits.pc := pc
    io.instr_bundle.bits.recovery_pc := pc

    io.fetch_bundle.ready := false.B
    io.peek_pc := pc

    when(!buffer_valid && !prefetch_flag) {
        // get new item
        io.fetch_bundle.ready := true.B
        when(io.fetch_bundle.valid) {
            instr_buffer(instr_buffer_now) := io.fetch_bundle.bits
            buffer_valid := true.B
            buffer_at := 0.U
        }
        next_pc := pc
    }.otherwise {
        io.instr_bundle.valid := true.B
        io.instr_bundle.bits.instr := instr_buffer(instr_buffer_now).items(buffer_at).instr
        io.instr_bundle.bits.pred_taken := instr_buffer(instr_buffer_now).items(buffer_at).pred_taken
        io.instr_bundle.bits.recovery_pc := instr_buffer(instr_buffer_now).items(buffer_at).recovery_pc
        when(io.instr_bundle.ready && !prefetch_flag) {
            buffer_valid := (buffer_at =/= 1.U)
            buffer_at := buffer_at + 1.U
            next_pc := instr_buffer(instr_buffer_now).items(buffer_at).pred_pc

            when((buffer_at + 1.U) === 1.U) {
                // prefetch
                io.fetch_bundle.ready := true.B
                when(io.fetch_bundle.valid) {
                    instr_buffer(~instr_buffer_now) := io.fetch_bundle.bits
                    prefetch_flag := true.B
                }
            }
        }.elsewhen(io.instr_bundle.ready && prefetch_flag) {
            instr_buffer_now := ~instr_buffer_now
            prefetch_flag := false.B
            buffer_valid := true.B
            buffer_at := 0.U
            next_pc := instr_buffer(instr_buffer_now).items(buffer_at).pred_pc
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