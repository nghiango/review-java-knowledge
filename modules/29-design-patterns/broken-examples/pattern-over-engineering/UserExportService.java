package lab.designpatterns.broken.overengineering;

import java.util.List;

public class UserExportService {

    private final AbstractExportFactoryProviderBridge exportBridge =
            new AbstractExportFactoryProviderBridge() {
                @Override
                public String generatePayload(ExportContextStrategyBuilder context) {
                    AbstractExportFactoryProviderBridge.ExportBridgeVisitor visitor =
                            new AbstractExportFactoryProviderBridge.DefaultExportVisitorBridge();

                    StringBuilder sb = new StringBuilder("id,username,email\n");
                    for (UserRecord r : context.getRecords()) {
                        sb.append(visitor.visitRecord(r)).append("\n");
                    }
                    return sb.toString();
                }
            };

    public String exportUsersToCsv(List<UserRecord> users) {
        ExportContextStrategyBuilder context = ExportContextStrategyBuilder.create().withRecords(users);
        return exportBridge.generatePayload(context);
    }
}
