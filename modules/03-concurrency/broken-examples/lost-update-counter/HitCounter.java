package lab.concurrency.broken.lostupdate;

public class HitCounter {
    private long totalHits;

    public void recordHit() {
        totalHits++;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void reset() {
        totalHits = 0;
    }
}
