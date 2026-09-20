package lab.concurrency.lockordering;

public record TransferResult(
        boolean success, String message, long fromBalanceCents, long toBalanceCents) {}
