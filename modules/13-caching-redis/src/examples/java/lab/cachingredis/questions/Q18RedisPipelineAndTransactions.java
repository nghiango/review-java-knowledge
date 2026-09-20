package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q18: What is the difference between Redis Pipelines, MULTI/EXEC Transactions, and Lua Scripts?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q18RedisPipelineAndTransactions {

    public static void main(String[] args) {
        Map<String, String> executionMechanisms =
                Map.of(
                        "Pipeline",
                        "Batches multiple commands in a single network socket write, reducing network RTT. Not atomic; other commands can interleave.",
                        "MULTI/EXEC",
                        "Queues commands on server. Executes sequentially without interleaving. Does NOT support rollback on runtime error.",
                        "Lua Script (EVAL)",
                        "Atomic script executed on Redis engine. Can evaluate intermediate results and branch logic (if-else) atomically.");

        boolean pipelineReducesRtt =
                executionMechanisms.get("Pipeline").contains("network RTT"); // true
        boolean redisTransactionsHaveNoRollback =
                executionMechanisms.get("MULTI/EXEC").contains("Does NOT support rollback"); // true
        boolean luaExecutesAtomicallyWithBranching =
                executionMechanisms.get("Lua Script (EVAL)").contains("branch logic"); // true
    }
}
