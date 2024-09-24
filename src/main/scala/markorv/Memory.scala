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

    // Use SyncReadMem instead of Mem
    val mem = SyncReadMem(size, UInt(8.W))

    // Initialize the memory with initial values
    val init_values = Seq(
        "h3e200c23".U(32.W),
        "h00110113".U(32.W),
        "hff9ff0ef".U(32.W)
    )

    // Little endian initialization
    for (i <- 0 until init_values.length) {
        for (j <- 0 until 4) {
        mem.write((i * 4 + j).U, (init_values(i) >> (j * 8))(7, 0))
        }
    }

    // Arbiter to manage access between two ports
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

    val port1_last_read_addr = RegInit(0.U(data_width.W))
    val port2_last_read_addr = RegInit(0.U(data_width.W))
    val port1_last_read_data = RegInit(0.U(data_width.W))
    val port2_last_read_data = RegInit(0.U(data_width.W))
    val port1_read_available = RegInit(false.B)
    val port2_read_available = RegInit(false.B)
    val mem_available = RegInit(false.B)

    // Peek operation to view specific memory content
    io.peek := Cat(mem.read(1023.U), mem.read(1022.U), mem.read(1021.U), mem.read(1020.U), 
                    mem.read(1019.U), mem.read(1018.U), mem.read(1017.U), mem.read(1016.U))

    // Port 1 operations
    when(arbiter.io.chosen === 0.U) {
        when(io.port1.write_enable) {
            switch(io.port1.write_width) {
                is(0.U) {
                    mem.write(io.port1.addr, io.port1.write_data(7, 0))
                }
                is(1.U) {
                    for (i <- 0 until 2) {
                        mem.write(io.port1.addr + i.U, io.port1.write_data((i * 8) + 7, i * 8))
                    }
                }
                is(2.U) {
                    for (i <- 0 until 4) {
                        mem.write(io.port1.addr + i.U, io.port1.write_data((i * 8) + 7, i * 8))
                    }
                }
                is(3.U) {
                    for (i <- 0 until 8) {
                        mem.write(io.port1.addr + i.U, io.port1.write_data((i * 8) + 7, i * 8))
                    }
                }
            }
            io.port1.write_outfire := true.B
            port2_read_available := false.B
        }.elsewhen(io.port1.data_out.ready && mem_available) {
            // Read from SyncReadMem, data available in the next cycle
            val read_data = Wire(Vec(data_width / 8, UInt(8.W)))
            for (i <- 0 until data_width / 8) {
                read_data(i) := mem.read(io.port1.addr + i.U)
            }
            io.port1.data_out.bits := Cat(read_data.reverse)
            port1_last_read_addr := io.port1.addr
            port2_read_available := false.B
            when(port1_last_read_addr === io.port1.addr && port1_read_available) {
                io.port1.data_out.valid := true.B
            }.elsewhen(port1_last_read_addr === io.port1.addr) {
                port1_read_available := true.B
            }
        }.elsewhen(io.port1.data_out.ready) {
            mem_available := true.B
        }
    }

    // Port 2 operations
    when(arbiter.io.chosen === 1.U) {
        when(io.port2.write_enable) {
            switch(io.port2.write_width) {
                is(0.U) {
                    mem.write(io.port2.addr, io.port2.write_data(7, 0))
                }
                is(1.U) {
                    for (i <- 0 until 2) {
                        mem.write(io.port2.addr + i.U, io.port2.write_data((i * 8) + 7, i * 8))
                    }
                }
                is(2.U) {
                    for (i <- 0 until 4) {
                        mem.write(io.port2.addr + i.U, io.port2.write_data((i * 8) + 7, i * 8))
                    }
                }
                is(3.U) {
                    for (i <- 0 until 8) {
                        mem.write(io.port2.addr + i.U, io.port2.write_data((i * 8) + 7, i * 8))
                    }
                }
            }
            io.port2.write_outfire := true.B
            port1_read_available := false.B
        }.elsewhen(io.port2.data_out.ready && mem_available) {
            // Read from SyncReadMem, data available in the next cycle
            val read_data = Wire(Vec(data_width / 8, UInt(8.W)))
            for (i <- 0 until data_width / 8) {
                read_data(i) := mem.read(io.port2.addr + i.U)
            }
            io.port2.data_out.bits := Cat(read_data.reverse)
            port2_last_read_addr := io.port2.addr
            port1_read_available := false.B
            when (port2_last_read_addr === io.port2.addr && port2_read_available) {
                io.port2.data_out.valid := true.B
            }.elsewhen(port2_last_read_addr === io.port2.addr) {
                port2_read_available := true.B
            }
        }.elsewhen(io.port2.data_out.ready) {
            mem_available := true.B
        }
    }
}
