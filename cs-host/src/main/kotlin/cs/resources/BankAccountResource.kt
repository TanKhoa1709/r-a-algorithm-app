package cs.resources

import java.util.concurrent.atomic.AtomicLong

/**
 * Bank account resource - Shared bank account with initial balance 100000
 */
class BankAccountResource(
    override val resourceId: String = "bank-account",
    override val resourceName: String = "Bank Account",
    initialBalance: Long = 100000L
) : SharedResource {
    private val balance = AtomicLong(initialBalance)
    private val accessCount = AtomicLong(0)
    private var lastAccessTime: Long? = null
    
    override suspend fun access(nodeId: String, requestId: String): ResourceAccessResult {
        return ResourceAccessResult(
            success = false,
            message = "Use withdraw or deposit methods instead"
        )
    }
    
    override suspend fun release(nodeId: String, requestId: String) {
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
        if (amount <= 0) return false
        
        while (true) {
            val current = balance.get()
            if (current < amount) {
                return false
            }
            if (balance.compareAndSet(current, current - amount)) {
                return true
            }
        }
    }
    
    fun deposit(amount: Long): Boolean {
        if (amount <= 0) return false
        balance.addAndGet(amount)
        return true
    }
}

