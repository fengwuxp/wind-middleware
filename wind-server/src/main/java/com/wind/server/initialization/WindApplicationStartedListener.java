package com.wind.server.initialization;

import com.wind.common.spring.SpringApplicationContextUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.OrderComparator;
import org.springframework.util.StopWatch;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 应用 Started 事件监听
 *
 * @author wuxp
 * @date 2023-10-22 07:49
 **/
@Slf4j
@AllArgsConstructor
public class WindApplicationStartedListener implements ApplicationListener<ApplicationStartedEvent> {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(@Nonnull ApplicationStartedEvent event) {
        if (INITIALIZED.get()) {
            return;
        }
        INITIALIZED.set(true);
        // 标记应用已启动
        SpringApplicationContextUtils.markStarted();
        // 执行系统初始化器
        execSystemInitializers(event.getApplicationContext());
    }

    private void execSystemInitializers(ApplicationContext context) {
        log.info("begin execute SystemInitializer");
        StopWatch watch = new StopWatch();
        watch.start("system-initialization-task");
        try {
            List<SystemInitializer> initializers = new ArrayList<>(context.getBeansOfType(SystemInitializer.class)
                    .values());
            OrderComparator.sort(initializers);
            initializers.stream()
                    .filter(SystemInitializer::requireInitialize)
                    .forEach(SystemInitializer::initialize);
        } catch (Exception exception) {
            log.error("execute SystemInitializer error", exception);
        }
        watch.stop();
        log.info("SystemInitializer execute end, use times = {} seconds", watch.getTotalTimeSeconds());
    }
}
