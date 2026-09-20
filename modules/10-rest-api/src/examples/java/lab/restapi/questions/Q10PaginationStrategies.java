package lab.restapi.questions;

import java.util.List;
import java.util.stream.IntStream;

public class Q10PaginationStrategies {

    public static void main(String[] args) {
        List<Integer> dataset = IntStream.rangeClosed(1, 100).boxed().toList();

        // Strategy 1: Offset-Based (page number and page size)
        // Complexity: O(offset + limit). High offsets cause performance degradation in DBs.
        int page = 2;
        int size = 10;
        int offset = page * size; // 20
        List<Integer> offsetPage = dataset.stream().skip(offset).limit(size).toList();
        int firstItemOffset = offsetPage.get(0); // 21

        // Strategy 2: Keyset / Cursor-Based (where id > lastSeenId limit size)
        // Complexity: O(limit) via indexed B-Tree seek. Immune to deep offset degradation.
        int lastSeenId = 20;
        List<Integer> cursorPage =
                dataset.stream().filter(id -> id > lastSeenId).limit(size).toList();
        int firstItemCursor = cursorPage.get(0); // 21

        boolean sameResults = offsetPage.equals(cursorPage); // true

        System.out.println("Offset first item: " + firstItemOffset); // Offset first item: 21
        System.out.println("Cursor first item: " + firstItemCursor); // Cursor first item: 21
        System.out.println(
                "Strategies produce equivalent content: "
                        + sameResults); // Strategies produce equivalent content: true
    }
}
