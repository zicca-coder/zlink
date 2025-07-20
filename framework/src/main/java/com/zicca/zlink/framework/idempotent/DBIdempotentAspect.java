package com.zicca.zlink.framework.idempotent;

import com.zicca.zlink.framework.execption.ServiceException;
import com.zicca.zlink.framework.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Aspect
@RequiredArgsConstructor
public class DBIdempotentAspect {

    private final IdempotentService idempotentService;

    @Around("@annotation(com.zicca.zlink.framework.idempotent.DBIdempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        DBIdempotent dbIdempotent = getDbIdempotentAnnotation(joinPoint);
        String uniqueKey = SpELUtil.parseKey(dbIdempotent.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs()).toString();

        IdempotentStatus status = idempotentService.tryInsertKey(uniqueKey);
        if (status == IdempotentStatus.ALREADY_CONSUMED) {
            // 已消费
            return null;
        }
        if (status == IdempotentStatus.CONSUMING) {
            throw new ServiceException("消息正在处理中");
        }
        Object result;
        try {
            result = joinPoint.proceed();
            // 业务执行成功后标记为已消费
            // 注意：这里不需要手动删除key，因为如果事务回滚，
            // 存储过程插入的记录也会一起回滚
            idempotentService.markConsumed(uniqueKey);
        } catch (Throwable e) {
            // 业务执行失败时，不需要手动删除key
            // 因为整个事务会回滚，存储过程插入的记录也会被回滚
            // 这样就解决了原来的问题：业务失败但幂等记录仍然存在的情况
            throw e;
        }
        return result;
    }


    private static DBIdempotent getDbIdempotentAnnotation(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(signature.getName(), signature.getParameterTypes());
        return targetMethod.getAnnotation(DBIdempotent.class);
    }


}
