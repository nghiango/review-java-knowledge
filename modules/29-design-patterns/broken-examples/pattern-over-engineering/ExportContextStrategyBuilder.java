package lab.designpatterns.broken.overengineering;

import java.util.List;

public class ExportContextStrategyBuilder {

    private List<UserRecord> records;

    public static ExportContextStrategyBuilder create() {
        return new ExportContextStrategyBuilder();
    }

    public ExportContextStrategyBuilder withRecords(List<UserRecord> records) {
        this.records = records;
        return this;
    }

    public List<UserRecord> getRecords() {
        return records;
    }
}
