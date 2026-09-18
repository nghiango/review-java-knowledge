package lab.corejava.questions;

import java.util.List;

@SuppressWarnings("unused")
public final class Q12ShallowVsDeepImmutabilityExample {
    private Q12ShallowVsDeepImmutabilityExample() {}

    public static class MutableTag {
        public String value;

        public MutableTag(String value) {
            this.value = value;
        }
    }

    public record Document(String title, List<MutableTag> tags) {}

    public static void main(String[] args) {
        MutableTag tag = new MutableTag("draft");
        Document doc = new Document("Spec", List.of(tag)); // List.of is shallowly unmodifiable

        tag.value = "published"; // mutates state inside referenced element!

        String currentTagValue =
                doc.tags().get(0).value; // "published" (deep immutability was violated)
    }
}
