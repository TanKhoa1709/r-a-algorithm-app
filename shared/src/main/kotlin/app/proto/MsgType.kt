package app.proto

/**
 * Message type enumeration for protocol identification
 */
enum class MsgType {
    REQUEST,
    REPLY,
    RELEASE,
    HEARTBEAT,
    DISCOVERY,
    STATUS_UPDATE
}

