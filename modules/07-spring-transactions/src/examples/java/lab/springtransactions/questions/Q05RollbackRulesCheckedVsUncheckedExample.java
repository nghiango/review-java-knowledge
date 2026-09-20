package lab.springtransactions.questions;

public class Q05RollbackRulesCheckedVsUncheckedExample {

    record RollbackRule(String exceptionType, boolean isUnchecked, boolean rollsBackByDefault) {}

    public static void main(String[] args) {
        // Unchecked exceptions (RuntimeException, Error) trigger automatic rollback by default
        RollbackRule unchecked = new RollbackRule("IllegalArgumentException", true, true);

        // Checked exceptions (Exception, IOException) DO NOT roll back by default (they commit!)
        RollbackRule checked = new RollbackRule("OrderValidationException", false, false);

        boolean uncheckedRollsBack = unchecked.rollsBackByDefault(); // true
        boolean checkedCommits = !checked.rollsBackByDefault(); // true (commits by default!)

        System.out.println(
                "Unchecked rolls back by default: "
                        + uncheckedRollsBack
                        + ", Checked commits by default: "
                        + checkedCommits);
    }
}
