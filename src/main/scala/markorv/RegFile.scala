package markorv

import chisel3._
import chisel3.util._

class RegFile(data_width: Int = 64) extends Module {
  val io = IO(new Bundle {
    val read_addr1 = Input(UInt(5.W))
    val read_addr2 = Input(UInt(5.W))
    val write_addr2 = Input(UInt(5.W))
    val write_data = Input(UInt(data_width.W))
    val write_enable = Input(Bool())
    val read_data1 = Output(UInt(data_width.W))
    val read_data2 = Output(UInt(data_width.W))
  })
  val regs = RegInit(VecInit(Seq.fill(32)(0.U(data_width.W))))

  io.read_data1 := Mux(io.read_addr1 === 0.U, 0.U, regs(io.read_addr1))
  io.read_data2 := Mux(io.read_addr2 === 0.U, 0.U, regs(io.read_addr2))

  when(io.write_enable && io.write_addr2 =/= 0.U) {
    regs(io.write_addr2) := io.write_data
  }
}
