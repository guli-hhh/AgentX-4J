package com.agentx4j.bazaar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 服务发现过滤条件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryFilter {

    /** 资源类型: "http" / "mcp" / null(全部) */
    private String type;

    /** 搜索关键词 */
    private String query;

    /** 标签过滤 */
    private List<String> tags;

    /** 最高价格（美元） */
    private BigDecimal maxPrice;

    /** 最低价格（美元） */
    private BigDecimal minPrice;

    /** 分页偏移 */
    private int offset;

    /** 分页大小 */
    private int limit;

    /** 排序方式: RELEVANCE / PRICE_ASC / PRICE_DESC / POPULARITY */
    private String sortBy;
}
