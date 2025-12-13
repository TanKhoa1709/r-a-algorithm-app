package cs.resources

import java.util.concurrent.atomic.AtomicLong

/**
 * Bank account resource - Shared bank account with initial balance 100000
 */
class BankAccountResource(
    override val resourceId: String = "bank-account",
    override val resourceName: String = "Bank Account",
    initialBalance: Long = 100000L  // Số dư ban đầu: 100000
) : SharedResource {
    private val balance = AtomicLong(initialBalance)
    private val accessCount = AtomicLong(0)
    private var lastAccessTime: Long? = null
    
    override suspend fun access(nodeId: String, requestId: String): ResourceAccessResult {
        // Không dùng method này nữa, dùng withdraw/deposit trực tiếp
        return ResourceAccessResult(
            success = false,
            message = "Use withdraw or deposit methods instead"
        )
    }
    
    override suspend fun release(nodeId: String, requestId: String) {
        // Không dùng method này nữa
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
    
    /**
     * Get current balance
     */
    fun getBalance(): Long = balance.get()
    
    /**
     * Withdraw money from account
     * Chỉ rút được nếu số dư >= số tiền muốn rút
     * @return true if successful, false if insufficient balance
     */
    fun withdraw(amount: Long): Boolean {
        if (amount <= 0) return false
        
        // Sử dụng compareAndSet để đảm bảo thread-safe
        while (true) {
            val current = balance.get()
            if (current < amount) {
                // Không đủ tiền
                return false
            }
            // Thử trừ tiền
            if (balance.compareAndSet(current, current - amount)) {
                // Thành công
                return true
            }
            // Nếu compareAndSet fail, retry (có thể balance đã thay đổi)
        }
    }
    
    /**
     * Deposit money to account
     * @return true if successful
     */
    fun deposit(amount: Long): Boolean {
        if (amount <= 0) return false
        balance.addAndGet(amount)
        return true
    }
}

