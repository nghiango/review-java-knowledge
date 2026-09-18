package lab.corejava.resourceprocessing;

public record ImportFailure(int lineNumber, String sourceRow, String reason) {}
