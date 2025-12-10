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
        accessCount.incrementAndGet()
        lastAccessTime = System.currentTimeMillis()
        
        // Simulate transaction
        val amount = 100L
        val newBalance = balance.addAndGet(amount)
        val data: Map<String, String> = mapOf(
            "balance" to newBalance.toString(),
            "transaction" to amount.toString()
        )
        return ResourceAccessResult(
            success = true,
            message = "Deposited $amount, new balance: $newBalance",
            data = data
        )
    }
    
    override suspend fun release(nodeId: String, requestId: String) {
        // Transaction completed
    }
    
    override fun getState(): ResourceState {
        val metadata: Map<String, String> = mapOf("balance" to balance.get().toString())
        return ResourceState(
            resourceId = resourceId,
            currentUser = null,
            accessCount = accessCount.get(),
            lastAccessTime = lastAccessTime,
            metadata = metadata
        )
    }
    
    fun getBalance(): Long = balance.get()
    fun withdraw(amount: Long): Boolean {
        return balance.updateAndGet { current ->
            if (current >= amount) current - amount else current
        } != balance.get()
    }
}

