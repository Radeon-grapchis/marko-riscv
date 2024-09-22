package markorv

import chisel3._
import chisel3.util._

class BranchPredUnit extends Module {
    val io = IO(new Bundle {
        val bpu_instr = Flipped(Decoupled(new Bundle {
            val instr = UInt(32.W)
            val pc = UInt(64.W)
        }))

        val bpu_result = Decoupled(new Bundle{
            val instr = UInt(32.W)
            val pc = UInt(64.W)
        })
    })

    
}