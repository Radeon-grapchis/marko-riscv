package markorv

import chisel3._
import chisel3.util._

class MemoryIO(data_width: Int, addr_width: Int) extends Bundle {
    val addr = Input(UInt(addr_width.W))
    val data_out = Decoupled(UInt(data_width.W))
    val write_enable = Input(Bool())
    val write_data = Input(UInt(data_width.W))
    val write_width = Input(UInt(2.W))
    val write_outfire = Output(Bool())
}

class Memory(data_width: Int = 64, addr_width: Int = 64, size: Int = 128) extends Module {
    val io = IO(new Bundle {
        val port1 = new MemoryIO(data_width, addr_width)
        val port2 = new MemoryIO(data_width, addr_width)
        val peek = Output(UInt(data_width.W))
    })

    val mem = Mem(size, UInt(8.W))

    // Little endian
    val init_values = Seq(
"h123450b7".U(32.W),
"h00001117".U(32.W),
"h67808193".U(32.W),
"h0091021b".U(32.W),
"h1001a293".U(32.W),
"h1001b313".U(32.W),
"h5551c393".U(32.W),
"h5551e413".U(32.W),
"h7ff1f493".U(32.W),
"h00419513".U(32.W),
"h0021959b".U(32.W),
"h0031d613".U(32.W),
"h0011d69b".U(32.W),
"h4021d713".U(32.W),
"h4031d79b".U(32.W),
"h00308833".U(32.W),
"h403088b3".U(32.W),
"h0030893b".U(32.W),
"h403089bb".U(32.W),
"h00309a33".U(32.W),
"h00309abb".U(32.W),
"h0030ab33".U(32.W),
"h0030bbb3".U(32.W),
"h0030cc33".U(32.W),
"h0030dcb3".U(32.W),
"h0030dd3b".U(32.W),
"h4030ddb3".U(32.W),
"h4030de3b".U(32.W),
"h0030eeb3".U(32.W),
"h0030ff33".U(32.W),
"h3fe03c23".U(32.W),
"h3fd03c23".U(32.W),
"h3fc03c23".U(32.W),
"h3fb03c23".U(32.W),
"h3fa03c23".U(32.W),
"h3f903c23".U(32.W),
"h3f803c23".U(32.W),
"h3f703c23".U(32.W),
"h3f603c23".U(32.W),
"h3f503c23".U(32.W),
"h3f403c23".U(32.W),
"h3f303c23".U(32.W),
"h3f203c23".U(32.W),
"h3f103c23".U(32.W),
"h3f003c23".U(32.W),
"h3ef03c23".U(32.W),
"h3ee03c23".U(32.W),
"h3ed03c23".U(32.W),
"h3ec03c23".U(32.W),
"h3eb03c23".U(32.W),
"h3ea03c23".U(32.W),
"h3e903c23".U(32.W),
"h3e803c23".U(32.W),
"h3e703c23".U(32.W),
"h3e603c23".U(32.W),
"h3e503c23".U(32.W),
"h3e403c23".U(32.W),
"h3e303c23".U(32.W),
"h3e203c23".U(32.W),
"h3e103c23".U(32.W),
"h3e103c23".U(32.W),
    )

    for (i <- 0 until init_values.length) {
        for (j <- 0 until 4) {
            mem(i * 4 + j) := (init_values(i) >> (j * 8))(7, 0)
        }
    }

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
    io.port1.write_outfire := false.B
    io.port2.data_out.bits := 0.U
    io.port2.data_out.valid := false.B
    io.port2.write_outfire := false.B

    io.peek := Cat(mem(1023), mem(1022), mem(1021), mem(1020), mem(1019), mem(1018), mem(1017), mem(1016))

    when(arbiter.io.chosen === 0.U) {
        when(io.port1.write_enable) {
            switch(io.port1.write_width) {
                is(0.U) {
                    mem(io.port1.addr) := io.port1.write_data(7, 0)
                }
                is(1.U) {
                    for (i <- 0 until 2) {
                        mem(io.port1.addr + i.U) := io.port1.write_data((i * 8) + 7, i * 8)
                    }
                }
                is(2.U) {
                    for (i <- 0 until 4) {
                        mem(io.port1.addr + i.U) := io.port1.write_data((i * 8) + 7, i * 8)
                    }
                }
                is(3.U) {
                    for (i <- 0 until 8) {
                        mem(io.port1.addr + i.U) := io.port1.write_data((i * 8) + 7, i * 8)
                    }
                }
            }
            io.port1.write_outfire := true.B
        }.elsewhen(io.port1.data_out.ready) {
            io.port1.data_out.bits := Cat((0 until data_width / 8).reverse.map(i => mem(io.port1.addr + i.U)))
            io.port1.data_out.valid := true.B
        }
    }

    when(arbiter.io.chosen === 1.U) {
        when(io.port2.write_enable) {
            switch(io.port2.write_width) {
                is(0.U) {
                    mem(io.port2.addr) := io.port2.write_data(7, 0)
                }
                is(1.U) {
                    for (i <- 0 until 2) {
                        mem(io.port2.addr + i.U) := io.port2.write_data((i * 8) + 7, i * 8)
                    }
                }
                is(2.U) {
                    for (i <- 0 until 4) {
                        mem(io.port2.addr + i.U) := io.port2.write_data((i * 8) + 7, i * 8)
                    }
                }
                is(3.U) {
                    for (i <- 0 until 8) {
                        mem(io.port2.addr + i.U) := io.port2.write_data((i * 8) + 7, i * 8)
                    }
                }
            }
            io.port2.write_outfire := true.B
        }.elsewhen(io.port2.data_out.ready) {
            io.port2.data_out.bits := Cat((0 until data_width / 8).reverse.map(i => mem(io.port2.addr + i.U)))
            io.port2.data_out.valid := true.B
        }
    }
}