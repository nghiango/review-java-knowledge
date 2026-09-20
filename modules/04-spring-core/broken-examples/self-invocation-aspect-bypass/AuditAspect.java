package lab.springcore.broken.selfinvocation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        System.out.println("[AUDIT LOG] Action: " + audited.action() + " on " + joinPoint.getSignature());
        return joinPoint.proceed();
    }
}
