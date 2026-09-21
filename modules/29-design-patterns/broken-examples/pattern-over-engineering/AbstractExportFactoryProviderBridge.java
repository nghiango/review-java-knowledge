package lab.designpatterns.broken.overengineering;

import java.util.List;

public interface AbstractExportFactoryProviderBridge {

    String generatePayload(ExportContextStrategyBuilder context);

    interface ExportBridgeVisitor {
        String visitRecord(UserRecord record);
    }

    class DefaultExportVisitorBridge implements ExportBridgeVisitor {
        @Override
        public String visitRecord(UserRecord record) {
            return record.id() + "," + record.username() + "," + record.email();
        }
    }
}
