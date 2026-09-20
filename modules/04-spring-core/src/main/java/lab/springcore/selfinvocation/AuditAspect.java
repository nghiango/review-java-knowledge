package lab.springcore.selfinvocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final List<String> auditLogs = new CopyOnWriteArrayList<>();

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();
        auditLogs.add(
                "Action: "
                        + audited.action()
                        + " Args: "
                        + java.util.Arrays.toString(joinPoint.getArgs()));
        return result;
    }

    public List<String> getAuditLogs() {
        return List.copyOf(auditLogs);
    }

    public void clear() {
        auditLogs.clear();
    }
}
