package markorv

import chisel3._
import _root_.circt.stage.ChiselStage
import markorv._

class MarkoRvCore extends Module {
    val io = IO(new Bundle {
    })

    val mem = Module(new Memory(64, 64, 1024))
    val instr_fetch_unit = Module(new InstrFetchUnit(64, 64))

    mem.io.write_enable := false.B
    mem.io.write_data := 0.U(64.W)

    instr_fetch_unit.io.mem_read_data <> mem.io.data_out
    instr_fetch_unit.io.mem_read_addr <> mem.io.addr_in

    instr_fetch_unit.io.instr.ready := true.B
    instr_fetch_unit.io.set_pc := false.B
    instr_fetch_unit.io.pc_in := 0.U(64.W)
}

object MarkoRvCore extends App {
    ChiselStage.emitSystemVerilogFile(
        new MarkoRvCore,
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}