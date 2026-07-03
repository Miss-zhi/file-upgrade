package com.qiwenshare.search.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.search.dto.SearchRequestDTO;
import com.qiwenshare.search.service.SearchService;
import com.qiwenshare.search.vo.SearchHealthVO;
import com.qiwenshare.search.vo.SearchResultVO;
import com.qiwenshare.search.service.SearchIndexService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索 API 控制器。
 *
 * <p>提供文件搜索和健康检查端点。关键词校验由 {@link SearchRequestDTO} 的
 * {@code @NotBlank + @Size} 注解 + {@code @Valid} 自动处理。</p>
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SearchIndexService searchIndexService;

    /**
     * 搜索文件。
     *
     * @param dto            搜索请求参数（由 @Valid 自动校验）
     * @param authentication 当前认证信息
     * @return 搜索结果
     */
    @GetMapping
    public RestResult<SearchResponse> search(@Valid SearchRequestDTO dto, Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        SearchService.SearchResult result = searchService.search(dto, userId);

        return RestResult.success(new SearchResponse(result.total(), result.items()));
    }

    /**
     * 搜索服务健康检查。
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public RestResult<SearchHealthVO> health() {
        boolean healthy = searchIndexService.isHealthy();
        SearchHealthVO vo = new SearchHealthVO(healthy, healthy ? "ES 可用" : "ES 不可用");
        if (healthy) {
            return RestResult.success(vo);
        } else {
            return RestResult.error("SEARCH_UNAVAILABLE", "搜索服务不可用");
        }
    }

    /**
     * 搜索响应封装。
     *
     * @param total 匹配总数
     * @param items 结果列表（明确的 List<SearchResultVO> 类型）
     */
    public record SearchResponse(long total, List<SearchResultVO> items) {
    }
}
