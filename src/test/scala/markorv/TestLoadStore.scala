package markorv

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class TestLoadStore extends AnyFreeSpec with Matchers {
  "FF should show in io.peek" in {
    simulate(new MarkoRvCore()) { cpu =>
      for (i <- 0 until 16) {
        val peek_value = cpu.io.peek1.peek().litValue
        val pc_value = cpu.io.pc.peek().litValue
        println(s"Cycle $i: peek1 = $peek_value pc = $pc_value")
        cpu.clock.step()
      }
    }
  }
}