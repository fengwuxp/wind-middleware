package com.wind.server.actuate.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.io.File;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 优雅停机支持
 *
 * @author wuxp
 * @date 2023-10-08 13:42
 **/
@Slf4j
public class GracefulShutdownHealthIndicator implements HealthIndicator, DisposableBean {

    /**
     * 用于标记是否进入停机状态
     */
    private static final File MARK_FILE = new File("/tmp/8899");

    private final AtomicBoolean health = new AtomicBoolean(true);

    private final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(1,
            new CustomizableThreadFactory("graceful-shutdown-health"));

    public GracefulShutdownHealthIndicator() {
        monitor();
    }

    private void monitor() {
        scheduled.schedule(() -> {
            try {
                health.set(!MARK_FILE.exists());
            } finally {
                monitor();
            }
            if (!health.get()) {
                log.info("health down");
            }
            // 每隔 3s 执行一次
        }, 3, TimeUnit.SECONDS);
    }

    @Override
    public Health health() {
        return health.get() ? Health.up().build() : Health.down().build();
    }

    @Override
    public void destroy() {
        scheduled.shutdown();
    }
}
