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

class Cache(n_set: Int = 8, n_way: Int = 4, n_byte: Int = 16) extends Module {
    val io = IO(new Bundle {
        val mem_read_addr = Decoupled(UInt(64.W))
        val mem_read_data = Flipped(Decoupled(UInt(64.W)))

        val read_addr = Flipped(Decoupled((UInt(64.W))))
        val read_cache_line = Decoupled(new CacheLine(n_set, n_way, n_byte))
    })

    val offset_width = log2Ceil(n_byte)
    val set_width = log2Ceil(n_set)
    val tag_width = 64 - offset_width - set_width

    val cache_line = Reg(new CacheLine(n_set, n_way, n_byte))
    val cache_mems = SyncReadMem(n_set, new CacheWay(n_set, n_way, n_byte))

    val read_tag = Reg(UInt((64 - set_width + offset_width).W))
    val read_set = Reg(UInt(set_width.W))

    object State extends ChiselEnum {
        val stat_idle, stat_look_up, stat_read_upstream, stat_write_upstream = Value
    }
    val state = RegInit(State.stat_idle)

    switch(state) {
        is(State.stat_idle) {
            io.read_addr.ready := true.B
            when(io.read_addr.valid) {
                read_tag := io.read_addr.bits(63, set_width + offset_width)
                read_set := io.read_addr.bits(set_width + offset_width - 1, offset_width)
                cache_line := cache_mems.read(read_set)
                state := State.stat_look_up
            }
        }
        is(State.stat_look_up) {
            when(cache_line.valid && read_tag === cache_line.tag) {
                io.read_cache_line.valid := true.B
                io.read_cache_line.bits := cache_line
                state := State.stat_idle
            }.otherwise {
                state := State.stat_read_upstream
            }
        }
        is(State.stat_read_upstream) {
            // TODO
        }
        is(State.stat_write_upstream) {
            // TODO
        }
    }
}