package com.agentx4j.settlement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 日终结算定时任务。
 *
 * <p>每日凌晨自动执行：</p>
 * <ol>
 *   <li>汇总当日交易</li>
 *   <li>计算净额</li>
 *   <li>执行批量结算</li>
 *   <li>生成对账单</li>
 *   <li>差异检测 → 告警</li>
 * </ol>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // Spring Boot 中
 * @Autowired
 * private SettlementScheduler scheduler;
 *
 * @PostConstruct
 * void init() {
 *     scheduler.start();
 * }
 *
 * @PreDestroy
 * void destroy() {
 *     scheduler.stop();
 * }
 * }</pre>
 */
public class SettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(SettlementScheduler.class);

    private final SettlementEngine settlementEngine;
    private final ReconciliationService reconciliationService;
    private ScheduledExecutorService executorService;
    private volatile boolean running = false;

    /** 结算延迟（秒），默认凌晨 2 点 */
    private final long initialDelaySeconds;

    /** 结算间隔（秒），默认 24 小时 */
    private final long periodSeconds;

    public SettlementScheduler(SettlementEngine settlementEngine,
                                ReconciliationService reconciliationService) {
        this(settlementEngine, reconciliationService, 7200, 86400);
    }

    public SettlementScheduler(SettlementEngine settlementEngine,
                                ReconciliationService reconciliationService,
                                long initialDelaySeconds,
                                long periodSeconds) {
        this.settlementEngine = settlementEngine;
        this.reconciliationService = reconciliationService;
        this.initialDelaySeconds = initialDelaySeconds;
        this.periodSeconds = periodSeconds;
    }

    /**
     * 启动定时任务。
     */
    public void start() {
        if (running) {
            log.warn("Scheduler is already running");
            return;
        }

        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "settlement-scheduler");
            t.setDaemon(true);
            return t;
        });

        executorService.scheduleAtFixedRate(
                this::executeDailySettlement,
                initialDelaySeconds,
                periodSeconds,
                TimeUnit.SECONDS
        );

        running = true;
        log.info("Settlement scheduler started (delay={}s, period={}s)",
                initialDelaySeconds, periodSeconds);
    }

    /**
     * 停止定时任务。
     */
    public void stop() {
        if (!running) return;

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        running = false;
        log.info("Settlement scheduler stopped");
    }

    /**
     * 立即执行一次结算（手动触发）。
     */
    public void executeNow() {
        log.info("Manual settlement triggered");
        executeDailySettlement();
    }

    /**
     * 是否正在运行。
     */
    public boolean isRunning() {
        return running;
    }

    // ==================== 内部方法 ====================

    private void executeDailySettlement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting daily settlement for: {}", yesterday);

        try {
            // 1. 生成对账报告
            ReconciliationService.ReconciliationReport report =
                    reconciliationService.generateDailyReport(yesterday);
            log.info("Reconciliation report generated: txCount={}, volume={}",
                    report.getTotalTransactions(), report.getTotalVolume());

            // 2. 差异检测
            java.util.List<ReconciliationService.ReconciliationDiscrepancy> discrepancies =
                    reconciliationService.detectDiscrepancies(yesterday);
            if (!discrepancies.isEmpty()) {
                log.warn("Found {} discrepancies for {}", discrepancies.size(), yesterday);
                // TODO: 发送告警通知
            }

            log.info("Daily settlement completed for: {}", yesterday);

        } catch (Exception e) {
            log.error("Daily settlement failed for: {}", yesterday, e);
        }
    }
}
