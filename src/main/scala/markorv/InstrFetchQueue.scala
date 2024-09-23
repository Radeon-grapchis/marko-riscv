package markorv

import chisel3._
import chisel3.util._

import markorv.BranchPredUnit

class FetchQueueEntities extends Bundle {
    val items = Vec(2, new Bundle {
        val instr = UInt(32.W)
        val is_branch = Bool()
        val pred_taken = Bool()
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

        val debug_peek = Output(UInt(64.W))
    })

    val bpu0 = Module(new BranchPredUnit)
    val bpu1 = Module(new BranchPredUnit)

    val instr_queue = Module(new Queue(
        new FetchQueueEntities, 
        queue_size,
        flow=true))
    val end_pc_reg = RegInit(0.U(64.W))
    val end_pc = Wire(UInt(64.W))

    val new_entity = WireInit(0.U.asTypeOf(new FetchQueueEntities))
    val new_entity_cat_cache = RegInit(0.U.asTypeOf(new FetchQueueEntities))
    val new_entity_cat_pc = RegInit(0.U(64.W))
    val new_entity_cat_flag = RegInit(false.B)

    io.fetch_bundle <> instr_queue.io.deq
    instr_queue.io.enq.valid := false.B
    instr_queue.io.enq.bits := 0.U.asTypeOf(new FetchQueueEntities)

    bpu0.io.bpu_instr.instr := 0.U
    bpu0.io.bpu_instr.pc := 0.U
    bpu1.io.bpu_instr.instr := 0.U
    bpu1.io.bpu_instr.pc := 0.U

    io.mem_read_data.ready := false.B
    io.mem_read_addr := 0.U

    io.debug_peek := instr_queue.io.count

    when(instr_queue.io.count === 0.U) {
        end_pc := io.next_fetch_pc
    }.otherwise {
        end_pc := end_pc_reg
    }

    when(instr_queue.io.enq.ready) {
        io.mem_read_data.ready := true.B

        when(new_entity_cat_flag) {
            io.mem_read_addr := new_entity_cat_pc
        }.otherwise {
            io.mem_read_addr := end_pc_reg
        }

        // get new command if queue isn't full.
        when(io.mem_read_data.valid && ~new_entity_cat_flag) {
            bpu0.io.bpu_instr.pc := end_pc_reg
            bpu1.io.bpu_instr.pc := end_pc_reg + 4.U
            bpu0.io.bpu_instr.instr := io.mem_read_data.bits(31,0)
            bpu1.io.bpu_instr.instr := io.mem_read_data.bits(63,32)

            when(bpu0.io.bpu_result.is_branch) {
                new_entity.items(0).instr := io.mem_read_data.bits(31,0)
                new_entity.items(0).is_branch := bpu0.io.bpu_result.is_branch
                new_entity.items(0).pred_taken := bpu0.io.bpu_result.pred_taken
                new_entity.items(0).pred_pc := bpu0.io.bpu_result.pred_pc
                new_entity.items(0).recovery_pc := bpu0.io.bpu_result.recovery_pc

                // cat next cycle.
                new_entity_cat_cache := new_entity
                new_entity_cat_pc := bpu0.io.bpu_result.pred_pc
                new_entity_cat_flag := true.B
            }.otherwise {
                new_entity.items(0).instr := io.mem_read_data.bits(31,0)
                new_entity.items(0).is_branch := bpu0.io.bpu_result.is_branch
                new_entity.items(0).pred_taken := bpu0.io.bpu_result.pred_taken
                new_entity.items(0).pred_pc := bpu0.io.bpu_result.pred_pc
                new_entity.items(0).recovery_pc := bpu0.io.bpu_result.recovery_pc

                new_entity.items(1).instr := io.mem_read_data.bits(63,32)
                new_entity.items(1).is_branch := bpu1.io.bpu_result.is_branch
                new_entity.items(1).pred_taken := bpu1.io.bpu_result.pred_taken
                new_entity.items(1).pred_pc := bpu1.io.bpu_result.pred_pc
                new_entity.items(1).recovery_pc := bpu1.io.bpu_result.recovery_pc

                instr_queue.io.enq.valid := true.B
                instr_queue.io.enq.bits := new_entity
                end_pc_reg := bpu1.io.bpu_result.pred_pc
            }
        }

        when(io.mem_read_data.valid && new_entity_cat_flag) {
            bpu0.io.bpu_instr.pc := new_entity_cat_pc
            bpu1.io.bpu_instr.pc := new_entity_cat_pc + 4.U
            bpu0.io.bpu_instr.instr := io.mem_read_data.bits(31,0)
            bpu1.io.bpu_instr.instr := io.mem_read_data.bits(63,32)

            new_entity.items(0) := new_entity_cat_cache.items(0)

            new_entity.items(1).instr := io.mem_read_data.bits(31,0)
            new_entity.items(1).is_branch := bpu0.io.bpu_result.is_branch
            new_entity.items(1).pred_taken := bpu0.io.bpu_result.pred_taken
            new_entity.items(1).pred_pc := bpu0.io.bpu_result.pred_pc
            new_entity.items(1).recovery_pc := bpu0.io.bpu_result.recovery_pc

            instr_queue.io.enq.valid := true.B
            instr_queue.io.enq.bits := new_entity
            end_pc_reg := bpu0.io.bpu_result.pred_pc

            new_entity_cat_flag := false.B
        }
    }
}