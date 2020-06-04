package com.ruoyi.gateway.filter;

import com.ruoyi.common.core.utils.web.WebUtils;
import com.ruoyi.gateway.config.properties.IgnoreClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.gateway.service.ValidateCodeService;
import reactor.core.publisher.Mono;

/**
 * 验证码过滤器
 * 
 * @author ruoyi
 */
@Component
public class ValidateCodeFilter extends AbstractGatewayFilterFactory<Object>
{
    private final static String AUTH_URL = "/oauth/token";

    @Autowired
    private ValidateCodeService validateCodeService;

    @Autowired
    private IgnoreClientProperties ignoreClient;

    @Override
    public GatewayFilter apply(Object config)
    {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 非登录请求，不处理
            if (!StringUtils.containsIgnoreCase(request.getURI().getPath(), AUTH_URL))
            {
                return chain.filter(exchange);
            }
            try
            {
                // swagger的oauth2.0验证码放行操作
                String[] clientInfos = WebUtils.getClientId(request);
                if (ignoreClient.getClients().contains(clientInfos[0]))
                {
                    return chain.filter(exchange);
                }

                validateCodeService.checkCapcha(request.getQueryParams().getFirst("code"),
                        request.getQueryParams().getFirst("uuid"));
            }
            catch (Exception e)
            {
                ServerHttpResponse response = exchange.getResponse();
                return exchange.getResponse().writeWith(
                        Mono.just(response.bufferFactory().wrap(JSON.toJSONBytes(AjaxResult.error(e.getMessage())))));
            }
            return chain.filter(exchange);
        };
    }
}
