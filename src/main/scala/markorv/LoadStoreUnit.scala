package markorv

import chisel3._
import chisel3.util._

class LoadStoreUnit(data_width: Int = 64, addr_width: Int = 64) extends Module {
    val io = IO(new Bundle {
        // lsu_opcode encoding:
        // Bit [2]   - Source: Determines whether the data source is from memory or an immediate value.
        //             0 = Memory (Mem)
        //             1 = Immediate (Imm)
        //
        // Bit [1]   - Sign: Indicates if the data is signed or unsigned.
        //             0 = Signed integer (SInt)
        //             1 = Unsigned integer (UInt)
        //
        // Bits [0:0] - Size: Specifies the size of the data being loaded or stored.
        //             00 = Byte (8 bits)
        //             01 = Halfword (16 bits)
        //             10 = Word (32 bits)
        //             11 = Doubleword (64 bits)
        //
        // Overall structure of lsu_opcode:
        // { Source [2], Sign [1], Size [1:0] }
        //
        // Example:
        //  - lsu_opcode = 3'b101:
        //    Source = 1 (Immediate), Sign = 0 (Signed), Size = 01 (Halfword)
        //    This means the operation uses an immediate value, with signed halfword data type.
        val lsu_opcode = Flipped(Decoupled(UInt(4.W)))

        val immediate = Input(SInt(data_width.W))
        val rs1 = Input(UInt(5.W))
        val rs2 = Input(UInt(5.W))
        val rd = Input(UInt(5.W))

        val mem_write = Decoupled(UInt(data_width.W))
        val mem_addr = Output(UInt(addr_width.W))
    })

    val io.mem_write.valid = false.Bit
    val io.mem_write.bits = 0.U(data_width.W)

    
}