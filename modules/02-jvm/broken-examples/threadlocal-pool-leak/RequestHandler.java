package lab.jvm.broken.threadlocalpoolleak;

public final class RequestHandler {
    public String handle(String requestId, String userId) {
        RequestContext.setUser(userId);
        return "handled " + requestId + " for " + RequestContext.currentUser().orElse("anonymous");
    }

    public String handleAnonymous(String requestId) {
        return "handled " + requestId + " for " + RequestContext.currentUser().orElse("anonymous");
    }
}
