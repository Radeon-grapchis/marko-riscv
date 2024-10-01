package markorv

import chisel3._
import chisel3.util._

import markorv.BranchPredUnit

class FetchQueueEntities extends Bundle {
    val instr = UInt(32.W)
    val is_branch = Bool()
    val pred_taken = Bool()
    val pred_pc = UInt(64.W)
    val recovery_pc = UInt(64.W)
}

class InstrFetchQueue(queue_size: Int = 16) extends Module {
    val io = IO(new Bundle {
        val mem_read_data = Flipped(Decoupled(UInt(64.W)))
        val mem_read_addr = Decoupled(UInt(64.W))

        val fetch_bundle = Decoupled(new FetchQueueEntities)

        val pc = Input(UInt(64.W))
    })

    val bpu = Module(new BranchPredUnit)

    val instr_queue = Module(new Queue(
        new FetchQueueEntities, 
        queue_size,
        flow=true))
    val end_pc_reg = RegInit(0.U(64.W))
    val end_pc = Wire(UInt(64.W))

    io.fetch_bundle <> instr_queue.io.deq

    instr_queue.io.enq.valid := false.B
    instr_queue.io.enq.bits := 0.U.asTypeOf(new FetchQueueEntities)

    bpu.io.bpu_instr.instr := 0.U
    bpu.io.bpu_instr.pc := 0.U

    io.mem_read_data.ready := false.B
    io.mem_read_addr.valid := false.B
    io.mem_read_addr.bits := 0.U

    when(instr_queue.io.count === 0.U) {
        end_pc := io.pc
    }.otherwise {
        end_pc := end_pc_reg
    }

    when(instr_queue.io.enq.ready) {
        io.mem_read_data.ready := true.B
        io.mem_read_addr.valid := true.B
        io.mem_read_addr.bits := end_pc

        when(io.mem_read_data.valid) {
            bpu.io.bpu_instr.pc := end_pc
            bpu.io.bpu_instr.instr := io.mem_read_data.bits(31,0)

            instr_queue.io.enq.bits.instr := io.mem_read_data.bits(31,0)
            instr_queue.io.enq.bits.is_branch := bpu.io.bpu_result.is_branch
            instr_queue.io.enq.bits.pred_taken := bpu.io.bpu_result.pred_taken
            instr_queue.io.enq.bits.pred_pc := bpu.io.bpu_result.pred_pc
            instr_queue.io.enq.bits.recovery_pc := bpu.io.bpu_result.recovery_pc

            instr_queue.io.enq.valid := true.B
        }
    }
}