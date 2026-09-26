package lab.designpatterns.questions;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates the Memento Pattern for externalizing and restoring internal state of an object
 * without violating encapsulation, commonly used in transactional rollback buffers and undo/redo engines.
 */
public class Q24MementoStateRollbackExample {

    public record EditorMemento(String stateSnapshot) {}

    public static class DocumentEditor {
        private String content;

        public void setContent(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public EditorMemento createMemento() {
            return new EditorMemento(this.content);
        }

        public void restore(EditorMemento memento) {
            this.content = memento.stateSnapshot();
        }
    }

    public static void main(String[] args) {
        DocumentEditor editor = new DocumentEditor();
        editor.setContent("Version 1: Draft");

        EditorMemento snapshot = editor.createMemento();

        editor.setContent("Version 2: Flawed Edit");
        // Rollback to saved memento
        editor.restore(snapshot);

        boolean restoredToOriginal = "Version 1: Draft".equals(editor.getContent()); // true
        System.out.println("Document editor state successfully rolled back: " + restoredToOriginal);
    }
}
