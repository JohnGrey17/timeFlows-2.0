package example.timeflows.model;

public enum OvertimeStatus {
    CHECKING,
    APPROVED_MANAGER,
    APPROVED_ADMIN,
    DECLINED,
    /**
     * @deprecated Legacy status retained only for backward-compatible reads.
     */
    @Deprecated
    PENDING,
    /**
     * @deprecated Legacy status retained only for backward-compatible reads.
     */
    @Deprecated
    APPROVED,
    /**
     * @deprecated Legacy status retained only for backward-compatible reads.
     */
    @Deprecated
    REJECTED
}
