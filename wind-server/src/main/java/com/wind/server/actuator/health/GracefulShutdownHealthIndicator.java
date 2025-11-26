package com.wind.server.actuator.health;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
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
    private final File markFile;

    private final AtomicBoolean health = new AtomicBoolean(true);

    private final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(1, new CustomizableThreadFactory("graceful-shutdown-health"));

    public GracefulShutdownHealthIndicator() {
        this("/tmp/8899");
    }

    private GracefulShutdownHealthIndicator(String markFilePath) {
        this.markFile = new File(markFilePath);
        scheduled.setRemoveOnCancelPolicy(true);
        monitor();
    }

    @Override
    public void destroy() {
        scheduled.shutdown();
    }

    @Override
    public @Nullable Health health() {
        return health.get() ? Health.up().build() : Health.down().build();
    }

    private void monitor() {
        scheduled.scheduleWithFixedDelay(() -> {
            try {
                health.set(!markFile.exists());
                if (!health.get()) {
                    log.info("health down");
                }
            } catch (Exception e) {
                log.error("monitor failed", e);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

}
