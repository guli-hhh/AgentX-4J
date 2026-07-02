package com.agentx4j.core.enums;

/**
 * 风险等级枚举。
 *
 * <p>基于风险评分（0-100）划分的四个等级：</p>
 * <ul>
 *   <li>LOW (0-30): 正常</li>
 *   <li>MEDIUM (31-60): 关注</li>
 *   <li>HIGH (61-80): 审核</li>
 *   <li>CRITICAL (81-100): 阻断</li>
 * </ul>
 */
public enum RiskLevel {

    LOW(0, 30),
    MEDIUM(31, 60),
    HIGH(61, 80),
    CRITICAL(81, 100);

    private final int minScore;
    private final int maxScore;

    RiskLevel(int minScore, int maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    /**
     * 根据风险评分获取风险等级。
     *
     * @param score 风险评分 (0-100)
     * @return 对应的风险等级
     */
    public static RiskLevel fromScore(int score) {
        for (RiskLevel level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }
        return score < 0 ? LOW : CRITICAL;
    }
}
