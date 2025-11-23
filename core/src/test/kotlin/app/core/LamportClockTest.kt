package app.core

import kotlin.test.Test
import kotlin.test.assertEquals

class LamportClockTest {
    @Test
    fun testTick() {
        val clock = LamportClock()
        assertEquals(0, clock.getTime())
        assertEquals(1, clock.tick())
        assertEquals(1, clock.getTime())
        assertEquals(2, clock.tick())
    }
    
    @Test
    fun testReceive() {
        val clock = LamportClock()
        clock.tick() // time = 1
        clock.tick() // time = 2
        
        val received = clock.receive(5)
        assertEquals(6, received)
        assertEquals(6, clock.getTime())
    }
    
    @Test
    fun testReceiveSmallerTimestamp() {
        val clock = LamportClock()
        clock.tick() // time = 1
        clock.tick() // time = 2
        clock.tick() // time = 3
        
        val received = clock.receive(1)
        assertEquals(4, received) // Should increment from current time
        assertEquals(4, clock.getTime())
    }
}

