package lab.jpahibernate.questions;

public class Q12CascadeAndOrphanRemoval {

    public static void main(String[] args) {
        // CascadeType.REMOVE: Triggers DELETE on child only when the parent entity itself is
        // deleted via em.remove(parent).
        // orphanRemoval = true: Triggers DELETE on child when parent is deleted AND whenever child
        // is removed from parent's collection (e.g. parent.getChildren().remove(child)).
        boolean cascadeRemoveDeletesWhenChildRemovedFromCollection = false; // false
        boolean orphanRemovalDeletesWhenChildRemovedFromCollection = true; // true

        System.out.println(
                "CascadeType.REMOVE deletes on collection remove: "
                        + cascadeRemoveDeletesWhenChildRemovedFromCollection); // CascadeType.REMOVE
        // deletes on
        // collection remove:
        // false
        System.out.println(
                "orphanRemoval deletes on collection remove: "
                        + orphanRemovalDeletesWhenChildRemovedFromCollection); // orphanRemoval
        // deletes on
        // collection remove:
        // true
    }
}
