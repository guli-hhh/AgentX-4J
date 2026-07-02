package com.agentx4j.x402.server;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 结算覆盖 — 用于 UptoScheme 设置实际结算金额。
 *
 * <p>Server 端在业务逻辑完成后调用，覆盖默认的最大金额结算。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 在 Controller 或 Service 中
 * @X402Protected(price = "$0.05", scheme = BillingScheme.UPTO)
 * @GetMapping("/api/generate")
 * public GeneratedText generate(@RequestParam String prompt) {
 *     // 执行业务逻辑
 *     GeneratedText result = llmService.generate(prompt);
 *
 *     // 根据实际用量设置结算金额
 *     int tokensUsed = result.getTokenCount();
 *     BigDecimal actualCost = calculateCost(tokensUsed);
 *     SettlementOverride.set(actualCost);
 *
 *     return result;
 * }
 * }</pre>
 *
 * <p>支持三种格式：</p>
 * <ul>
 *   <li>原始原子单位: SettlementOverride.set("500") → 500 原子单位</li>
 *   <li>百分比: SettlementOverride.setPercent(50) → 授权上限的 50%</li>
 *   <li>美元价格: SettlementOverride.setDollar("0.003") → 转换为原子单位</li>
 * </ul>
 */
public final class SettlementOverride {

    private static final ThreadLocal<String> overrideAmount = new ThreadLocal<>();
    private static final int DEFAULT_TOKEN_DECIMALS = 6; // USDC

    private SettlementOverride() {}

    /**
     * 设置结算金额（原子单位）。
     *
     * @param amount 原子单位金额字符串（如 "500"）
     */
    public static void set(String amount) {
        overrideAmount.set(amount);
    }

    /**
     * 设置结算金额为授权上限的百分比。
     *
     * @param percent 百分比（0-100）
     */
    public static void setPercent(int percent) {
        overrideAmount.set(percent + "%");
    }

    /**
     * 设置结算金额（美元价格，自动转换为原子单位）。
     *
     * @param dollarAmount 美元金额字符串（如 "0.003"）
     */
    public static void setDollar(String dollarAmount) {
        BigDecimal dollars = new BigDecimal(dollarAmount.startsWith("$")
                ? dollarAmount.substring(1) : dollarAmount);
        BigDecimal atomicUnits = dollars
                .multiply(BigDecimal.TEN.pow(DEFAULT_TOKEN_DECIMALS))
                .setScale(0, RoundingMode.FLOOR);
        overrideAmount.set(atomicUnits.toBigInteger().toString());
    }

    /**
     * 设置结算金额（BigDecimal 美元价格）。
     */
    public static void setDollar(BigDecimal dollarAmount) {
        BigDecimal atomicUnits = dollarAmount
                .multiply(BigDecimal.TEN.pow(DEFAULT_TOKEN_DECIMALS))
                .setScale(0, RoundingMode.FLOOR);
        overrideAmount.set(atomicUnits.toBigInteger().toString());
    }

    /**
     * 获取当前线程的结算覆盖金额。
     *
     * @return 结算金额字符串，如果未设置则返回 null
     */
    public static String get() {
        return overrideAmount.get();
    }

    /**
     * 检查是否设置了结算覆盖。
     */
    public static boolean isSet() {
        return overrideAmount.get() != null;
    }

    /**
     * 清除当前线程的结算覆盖。
     */
    public static void clear() {
        overrideAmount.remove();
    }

    /**
     * 解析结算金额为原子单位。
     *
     * <p>如果设置了覆盖，返回覆盖值；否则返回授权上限。</p>
     *
     * @param maxAuthorizedAmount 授权上限（原子单位）
     * @return 实际结算金额（原子单位）
     */
    public static String resolve(String maxAuthorizedAmount) {
        String override = overrideAmount.get();
        if (override == null) {
            return maxAuthorizedAmount;
        }

        // 百分比格式: "50%"
        if (override.endsWith("%")) {
            int percent = Integer.parseInt(override.substring(0, override.length() - 1));
            BigDecimal max = new BigDecimal(maxAuthorizedAmount);
            return max.multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                    .toBigInteger()
                    .toString();
        }

        // 原始原子单位
        return override;
    }
}
