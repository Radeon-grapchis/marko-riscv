package markorv.cache

import chisel3._
import chisel3.util._

class CacheLine(n_set: Int, n_way: Int, n_byte: Int) extends Bundle {
    val data = UInt((8*n_byte).W)
    val tag = UInt((64-log2Ceil(n_set)-log2Ceil(n_way)-log2Ceil(n_byte)).W)
    val valid = Bool()
}

class CacheWay(n_set: Int, n_way: Int, n_byte: Int) extends Bundle {
    val data = Vec(n_way, new CacheLine(n_set, n_way, n_byte))
}

class Cache(n_set: Int = 8, n_way: Int = 4, n_byte: Int = 16, upstream_bandwidth: Int = 64) extends Module {
    val io = IO(new Bundle {
        val mem_read_addr = Decoupled(UInt(64.W))
        val mem_read_data = Flipped(Decoupled(UInt(64.W)))

        val read_addr = Flipped(Decoupled((UInt(64.W))))
        val read_cache_line = Decoupled(new CacheLine(n_set, n_way, n_byte))

        val pk_tmp = Output(new CacheLine(n_set, n_way, n_byte))
        val pk_sk = Output(UInt(64.W))
    })

    val offset_width = log2Ceil(n_byte)
    val set_width = log2Ceil(n_set)
    val tag_width = 64 - offset_width - set_width

    val cache_mems = SyncReadMem(n_set, new CacheWay(n_set, n_way, n_byte))
    val temp_cache_line = Reg(new CacheLine(n_set, n_way, n_byte))
    val temp_cache_way = Reg(new CacheWay(n_set, n_way, n_byte))

    val replace_way = Reg(UInt(log2Ceil(n_way).W))

    val read_tag = Reg(UInt((64 - set_width + offset_width).W))
    val read_set = Reg(UInt(set_width.W))

    val read_ptr = Reg(UInt(log2Ceil(n_byte).W))

    object State extends ChiselEnum {
        val stat_idle, stat_look_up, stat_read_upstream, stat_write_upstream, stat_replace = Value
    }
    val state = RegInit(State.stat_idle)

    io.pk_tmp := temp_cache_line
    io.pk_sk := state.asUInt

    io.read_cache_line.valid := false.B
    io.read_cache_line.bits := 0.U.asTypeOf(new CacheLine(n_set, n_way, n_byte))
    io.read_addr.ready := state === State.stat_idle

    io.mem_read_addr.valid := false.B
    io.mem_read_addr.bits := 0.U(64.W)

    io.mem_read_data.ready := false.B

    switch(state) {
        is(State.stat_idle) {
            when(io.read_addr.valid) {
                read_tag := io.read_addr.bits(63, set_width + offset_width)
                read_set := io.read_addr.bits(set_width + offset_width - 1, offset_width)
                temp_cache_way := cache_mems.read(read_set)
                state := State.stat_look_up
            }
        }
        is(State.stat_look_up) {
            val hit = Wire(Bool())
            hit := false.B
            
            for (i <- 0 until n_way) {
                when(temp_cache_way.data(i).tag === read_tag && temp_cache_way.data(i).valid) {
                    // Hit
                    io.read_cache_line.valid := true.B
                    io.read_cache_line.bits := temp_cache_way.data(i)
                    hit := true.B
                    state := State.stat_idle
                }
            }
            
            when(!hit) {
                // Miss
                io.mem_read_data.ready := true.B
                io.mem_read_addr.valid := true.B
                io.mem_read_addr.bits := Cat(read_tag, read_set, 0.U(log2Ceil(n_byte).W))

                read_ptr := 0.U
                temp_cache_line.valid := true.B
                temp_cache_line.tag := read_tag
                state := State.stat_read_upstream
            }
        }
        is(State.stat_read_upstream) {
            io.mem_read_data.ready := true.B
            io.mem_read_addr.valid := true.B
            io.mem_read_addr.bits := Cat(read_tag, read_set, read_ptr)
            when(io.mem_read_data.valid) {
                // Read upstream data
                val next_cache_line = Wire(Vec((8*n_byte)/upstream_bandwidth, UInt(upstream_bandwidth.W)))
                
                for (i <- 0 until (8*n_byte)/upstream_bandwidth) {
                    when((i*upstream_bandwidth/8).U === read_ptr) {
                        next_cache_line(i) := io.mem_read_data.bits
                    }.otherwise {
                        next_cache_line(i) := temp_cache_line.data((i+1)*upstream_bandwidth - 1, i*upstream_bandwidth)
                    }
                }
                temp_cache_line.data := Cat(next_cache_line.reverse)
                
                read_ptr := read_ptr + (1 << log2Ceil(upstream_bandwidth/8)).U
                when(read_ptr === (1 << (log2Ceil(((8*n_byte/upstream_bandwidth)*upstream_bandwidth/8)-upstream_bandwidth/8))).U) {
                    // Make sure wont replace the same
                    replace_way := replace_way + 1.U
                    temp_cache_way.data(replace_way) := temp_cache_line
                    state := State.stat_replace
                }
            }
        }
        is(State.stat_write_upstream) {
            // TODO
        }
        is(State.stat_replace) {
            io.read_cache_line.valid := true.B
            io.read_cache_line.bits := temp_cache_line
            temp_cache_line.data := 0.U
            temp_cache_line.tag := 0.U
            temp_cache_line.valid := false.B
            cache_mems.write(read_set, temp_cache_way)
            state := State.stat_idle
        }
    }
}