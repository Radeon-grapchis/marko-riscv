package markorv

import chisel3._
import chisel3.util._

class InstrFetchUnit(queue_size: Int = 16) extends Module {
    val io = IO(new Bundle {
        val mem_read_data = Flipped(Decoupled((UInt(data_width.W))))
        val mem_read_addr = Output(UInt(addr_width.W))

        val bpu_instr = Decoupled(new Bundle {
            val instr = UInt(32.W)
            val pc = UInt(64.W)
        })

        val bpu_result = Flipped(Decoupled(new Bundle{
            val instr = UInt(32.W)
            val pc = UInt(64.W)
        }))

        val fetch_task = Decoupled(new Bundle {
            val instr = UInt(32.W)
            val pred = Bool()
            val pred_addr = UInt(64.W)
        })
    })

    val instr_queue = Module(new Queue(UInt(32.W), queue_size))
}