package markorv

import chisel3._
import chisel3.util._

import markorv.BranchPredUnit

class FetchQueueEntities extends Bundle {
    val items = Vec(2, new Bundle {
        val valid = Bool()
        val instr = UInt(32.W)
        val is_branch = Bool()
        val pred_pc = UInt(64.W)
        val recovery_pc = UInt(64.W)
    })
}

class InstrFetchQueue(queue_size: Int = 16) extends Module {
    val io = IO(new Bundle {
        val mem_read_data = Flipped(Decoupled(UInt(64.W)))
        val mem_read_addr = Output(UInt(64.W))

        val fetch_bundle = Decoupled(new FetchQueueEntities)

        val next_fetch_pc = Input(UInt(64.W))
    })

    val bpu0 = Module(new BranchPredUnit)
    val bpu1 = Module(new BranchPredUnit)

    val instr_queue = Module(new Queue(
        new FetchQueueEntities, 
        queue_size, 
        hasFlush=true))
    val end_pc = RegInit(0.U(64.W))

    val new_entity = WireInit(0.U.asTypeOf(new FetchQueueEntities))

    when(instr_queue.io.enq.ready && instr_queue.io.count =/= 0.U) {
        // get new command if queue isn't full.

    }.elsewhen(instr_queue.io.count === 0.U) {
        // init fetch queue.
        end_pc := io.next_fetch_pc
        io.mem_read_addr := io.next_fetch_pc
        when(io.mem_read_data.valid) {
            bpu0.io.bpu_instr.pc := io.next_fetch_pc
            bpu1.io.bpu_instr.pc := io.next_fetch_pc + 4.U
            bpu0.io.bpu_instr.instr := io.mem_read_data.bits(63,32)
            bpu1.io.bpu_instr.instr := io.mem_read_data.bits(31,0)

            when(io.bpu0.bpu_result.is_branch) {
                new_entity.items(0).valid := true.B
                new_entity.items(0).instr := 
                new_entity.items(0).is_branch := bpu0.io.bpu_result.is_branch
                new_entity.items(0).pred_pc := bpu0.io.bpu_result.pred_pc
                new_entity.items(0).recovery_pc := bpu0.io.bpu_result.recovery_pc
            }.elsewhen {
            }
        }
    }
}