package markorv

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

import markorv._

class MarkoRvCore extends Module {
    val io = IO(new Bundle {
    })

    val mem = Module(new Memory(64, 64, 1024))
    val instr_fetch_unit = Module(new InstrFetchUnit)
    val instr_decoder = Module(new InstrDecoder)

    mem.io.addr_in <> instr_fetch_unit.io.mem_read_addr
    PipelineConnect(mem.io.data_out, instr_fetch_unit.io.mem_read_data, true.B, false.B)
    PipelineConnect(instr_fetch_unit.io.instr_bundle, instr_decoder.io.instr_bundle, true.B, false.B)

    mem.io.write_enable := false.B
    mem.io.write_data := 0.U(64.W)
}

object MarkoRvCore extends App {
    ChiselStage.emitSystemVerilogFile(
        new MarkoRvCore,
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}