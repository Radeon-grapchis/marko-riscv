package markorv

import chisel3._
import chisel3.util._

class MemoryIO(data_width: Int, addr_width: Int) extends Bundle {
  val addr = Input(UInt(addr_width.W))
  val data_out = Decoupled(UInt(data_width.W))
  val write_enable = Input(Bool())
  val write_data = Input(UInt(data_width.W))
}

class Memory(data_width: Int = 64, addr_width: Int = 64, size: Int = 1024) extends Module {
  val io = IO(new Bundle {
    val port1 = new MemoryIO(data_width, addr_width)
    val port2 = new MemoryIO(data_width, addr_width)
    val peek = Output(UInt(data_width.W))
  })

  val mem = Mem(size, UInt(8.W))

  val arbiter = Module(new Arbiter(Bool(), 2))
  arbiter.io.in(0).valid := io.port1.write_enable || io.port1.data_out.ready
  arbiter.io.in(1).valid := io.port2.write_enable || io.port2.data_out.ready
  arbiter.io.in(0).bits := true.B
  arbiter.io.in(1).bits := true.B

  val arbiterOut = Wire(Bool())
  arbiterOut := arbiter.io.out.ready
  arbiter.io.out.ready := true.B

  io.port1.data_out.bits := 0.U
  io.port1.data_out.valid := false.B
  io.port2.data_out.bits := 0.U
  io.port2.data_out.valid := false.B

  io.peek := mem(size-1)

  when(arbiter.io.chosen === 0.U) {
    when(io.port1.write_enable) {
      for (i <- 0 until data_width / 8) {
        mem(io.port1.addr + i.U) := io.port1.write_data((i + 1) * 8 - 1, i * 8)
      }
    }.elsewhen(io.port1.data_out.ready) {
      io.port1.data_out.bits := Cat((0 until data_width / 8).map(i => mem(io.port1.addr + i.U)))
      io.port1.data_out.valid := true.B
    }
  }

  when(arbiter.io.chosen === 1.U) {
    when(io.port2.write_enable) {
      for (i <- 0 until data_width / 8) {
        mem(io.port2.addr + i.U) := io.port2.write_data((i + 1) * 8 - 1, i * 8)
      }
    }.elsewhen(io.port2.data_out.ready) {
      io.port2.data_out.bits := Cat((0 until data_width / 8).map(i => mem(io.port2.addr + i.U)))
      io.port2.data_out.valid := true.B
    }
  }
}