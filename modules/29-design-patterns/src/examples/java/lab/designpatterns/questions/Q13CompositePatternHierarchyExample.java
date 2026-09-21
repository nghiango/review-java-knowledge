package lab.designpatterns.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Q13: Composite Pattern. Demonstrates treating individual objects and compositions of objects
 * uniformly.
 */
public class Q13CompositePatternHierarchyExample {

    public interface FileSystemComponent {
        long getSize();
    }

    public static class FileLeaf implements FileSystemComponent {
        private final long size;

        public FileLeaf(long size) {
            this.size = size;
        }

        @Override
        public long getSize() {
            return size;
        }
    }

    public static class DirectoryComposite implements FileSystemComponent {
        private final List<FileSystemComponent> children = new ArrayList<>();

        public void add(FileSystemComponent component) {
            children.add(component);
        }

        @Override
        public long getSize() {
            long total = 0;
            for (FileSystemComponent child : children) {
                total += child.getSize();
            }
            return total;
        }
    }

    public static void main(String[] args) {
        DirectoryComposite root = new DirectoryComposite();
        root.add(new FileLeaf(100));

        DirectoryComposite subDir = new DirectoryComposite();
        subDir.add(new FileLeaf(250));
        subDir.add(new FileLeaf(150));

        root.add(subDir);

        long totalSize = root.getSize(); // 500
        boolean uniformCalculation = (totalSize == 500); // true

        System.out.println("Q13 totalSize: " + totalSize + ", uniform: " + uniformCalculation);
    }
}
