package markorv

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

import markorv._

class MarkoRvCore extends Module {
    val io = IO(new Bundle {
        val pc = Output(UInt(64.W))
        val instr_now = Output(UInt(64.W))
        val peek = Output(UInt(64.W))
    })

    val mem = Module(new Memory(64, 64, 128))
    val instr_fetch_unit = Module(new InstrFetchUnit)
    val instr_decoder = Module(new InstrDecoder)
    val load_store_unit = Module(new LoadStoreUnit)
    val register_file = Module(new RegFile)

    io.pc := instr_fetch_unit.io.instr_bundle.bits.pc
    instr_fetch_unit.io.pc_in := 0.U(64.W)
    instr_fetch_unit.io.set_pc := false.B

    mem.io.port2.write_enable := false.B
    mem.io.port2.write_data := 0.U(64.W)

    load_store_unit.io.mem_write.ready := true.B

    mem.io.port2.addr <> instr_fetch_unit.io.mem_read_addr
    mem.io.port2.data_out <> instr_fetch_unit.io.mem_read_data
    io.instr_now <> instr_fetch_unit.io.instr_bundle.bits.instr
    io.peek <> mem.io.peek

    instr_decoder.io.reg_read1 <> register_file.io.read_addr1
    instr_decoder.io.reg_read2 <> register_file.io.read_addr2
    instr_decoder.io.reg_data1 <> register_file.io.read_data1
    instr_decoder.io.reg_data2 <> register_file.io.read_data2
    instr_decoder.io.acquire_reg <> register_file.io.acquire_reg
    instr_decoder.io.acquired <> register_file.io.acquired
    instr_decoder.io.occupied_regs <> register_file.io.peek_occupied

    load_store_unit.io.write_register <> register_file.io.write_addr
    load_store_unit.io.write_back_data <> register_file.io.write_data
    load_store_unit.io.write_back_enable <> register_file.io.write_enable
    load_store_unit.io.release_reg <> register_file.io.release_reg
    mem.io.port1.write_data <> load_store_unit.io.mem_write.bits
    mem.io.port1.write_enable <> load_store_unit.io.mem_write.valid
    mem.io.port1.data_out <> load_store_unit.io.mem_read
    mem.io.port1.addr <> load_store_unit.io.mem_addr
    mem.io.port1.write_outfire <> load_store_unit.io.mem_write_outfire

    PipelineConnect(instr_fetch_unit.io.instr_bundle, instr_decoder.io.instr_bundle, false.B, false.B)
    PipelineConnect(instr_decoder.io.lsu_out, load_store_unit.io.lsu_instr, load_store_unit.io.state_peek === 0.U , false.B)
}

object MarkoRvCore extends App {
    ChiselStage.emitSystemVerilogFile(
        new MarkoRvCore,
        firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}