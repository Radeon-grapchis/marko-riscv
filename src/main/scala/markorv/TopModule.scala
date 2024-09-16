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
    val load_store_unit = Module(new LoadStoreUnit)
    val register_file = Module(new RegFile)

    mem.io.addr_in <> instr_fetch_unit.io.mem_read_addr
    mem.io.data_out <> instr_fetch_unit.io.mem_read_data
    
    register_file.io.write_enable := false.B
    register_file.io.write_data := 0.U(64.W)

    instr_decoder.io.reg_read1 <> register_file.io.read_addr1
    instr_decoder.io.reg_read2 <> register_file.io.read_addr2
    instr_decoder.io.reg_data1 <> register_file.io.read_data1
    instr_decoder.io.reg_data2 <> register_file.io.read_data1
    PipelineConnect(instr_fetch_unit.io.instr_bundle, instr_decoder.io.instr_bundle, true.B, false.B)
    PipelineConnect(instr_decoder.io.lsu_out, load_store_unit.io.lsu_instr, true.B, false.B)

    mem.io.write_enable := false.B
    mem.io.write_data := 0.U(64.W)
}

object MarkoRvCore extends App {
    ChiselStage.emitSystemVerilogFile(
        new MarkoRvCore,
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}