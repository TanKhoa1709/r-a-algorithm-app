package cs.resources

import java.util.concurrent.atomic.AtomicLong

/**
 * Bank account resource (simulated)
 */
class BankAccountResource(
    override val resourceId: String = "bank-account",
    override val resourceName: String = "Bank Account",
    initialBalance: Long = 1000
) : SharedResource {
    private val balance = AtomicLong(initialBalance)
    private val accessCount = AtomicLong(0)
    private var lastAccessTime: Long? = null
    
    override suspend fun access(nodeId: String, requestId: String): ResourceAccessResult {
        val currentBalance = balance.get()
        accessCount.incrementAndGet()
        lastAccessTime = System.currentTimeMillis()
        
        // Simulate transaction
        val amount = 100L
        val newBalance = balance.addAndGet(amount)
        
        return ResourceAccessResult(
            success = true,
            message = "Deposited $amount, new balance: $newBalance",
            data = mapOf(
                "balance" to newBalance,
                "transaction" to amount
            )
        )
    }
    
    override suspend fun release(nodeId: String, requestId: String) {
        // Transaction completed
    }
    
    override fun getState(): ResourceState {
        return ResourceState(
            resourceId = resourceId,
            currentUser = null,
            accessCount = accessCount.get(),
            lastAccessTime = lastAccessTime,
            metadata = mapOf("balance" to balance.get())
        )
    }
    
    fun getBalance(): Long = balance.get()
    fun withdraw(amount: Long): Boolean {
        return balance.updateAndGet { current ->
            if (current >= amount) current - amount else current
        } != balance.get()
    }
}

