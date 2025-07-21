package com.wind.security.captcha;

import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.locks.LockFactory;
import com.wind.common.locks.WindLock;
import lombok.AllArgsConstructor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于锁支持的验证码管理器
 *
 * @author wuxp
 * @date 2025-04-15 16:24
 **/
@AllArgsConstructor
public class CaptchaManagerLocksWrapper implements CaptchaManager {

    private static final AtomicInteger LOCK_LEASE_TIME = new AtomicInteger(5 * 1000);

    private static final String GEN_LOCK_KEY_PREFIX = "captcha-gen-%s-%s-%s";

    private static final String VERIFY_LOCK_KEY_PREFIX = "captcha-verify-%s-%s-%s";

    private final CaptchaManager delegate;

    private final LockFactory lockFactory;

    @Override
    public Captcha generate(Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene, String owner) {
        WindLock lock = lockFactory.apply(String.format(GEN_LOCK_KEY_PREFIX, type, useScene, owner));
        try {
            AssertUtils.state(lock.tryLock(500, LOCK_LEASE_TIME.get(), TimeUnit.MICROSECONDS), () -> BaseException.friendly("captcha gen get lock " +
                    "failure"));
            return delegate.generate(type, useScene, owner);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BaseException(DefaultExceptionCode.COMMON_FRIENDLY_ERROR, exception.getMessage(), exception);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void verify(String expected, Captcha.CaptchaType type, Captcha.CaptchaUseScene useScene, String owner) {
        WindLock lock = lockFactory.apply(String.format(VERIFY_LOCK_KEY_PREFIX, type, useScene, owner));
        try {
            AssertUtils.isTrue(lock.tryLock(500, LOCK_LEASE_TIME.get(), TimeUnit.MICROSECONDS), "captcha verify get lock failure");
            delegate.verify(expected, type, useScene, owner);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, exception.getMessage(), exception);
        } finally {
            lock.unlock();
        }

    }

    /**
     * 设置锁的过期时间
     *
     * @param leaseTime 锁的过期时间
     */
    public static void setLockLeaseTime(int leaseTime) {
        LOCK_LEASE_TIME.set(leaseTime);
    }
}
