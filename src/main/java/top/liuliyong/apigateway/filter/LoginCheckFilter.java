package top.liuliyong.apigateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import top.liuliyong.account.client.AccountClient;
import top.liuliyong.account.common.response.AccountOperationResponse;
import top.liuliyong.account.common.response.StatusEnum;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

@Component
@Slf4j
public class LoginCheckFilter extends ZuulFilter {
    public LoginCheckFilter(AccountClient ssoAppController) {
        this.ssoAppController = ssoAppController;
    }

    private final AccountClient ssoAppController;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        String uri = request.getRequestURI();
        if (uri.equalsIgnoreCase("/account-server/account") && request.getMethod().equalsIgnoreCase("POST")) {
            return false;
        }
        if (uri.equalsIgnoreCase("/account-server/account/login")) {
            return false;
        }
        if (uri.equalsIgnoreCase("/application/routes")) {
            return false;
        }
        return true;
    }

    @Override
    public Object run() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();

        // 记录下请求内容
        log.info("URL : " + Objects.requireNonNull(request).getRequestURL().toString());
        log.info("HTTP_METHOD : " + request.getMethod());
        log.info("IP : " + request.getRemoteAddr());

        //判定是否已经登录
        String session_id = request.getHeader("session_id");
        if (session_id == null || session_id.trim().length() == 0) {
            log.warn("session_id is null !");
            requestContext.setSendZuulResponse(false);
            requestContext.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            requestContext.setResponseBody(JSON.toJSONString(new AccountOperationResponse(StatusEnum.LACK_OF_INFORMATION)));
            return null;
        }
        AccountOperationResponse checkResult = ssoAppController.logincheck(session_id);
        if (checkResult.getRtn() != 0) {
            requestContext.setSendZuulResponse(false);
            requestContext.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            requestContext.setResponseBody(JSON.toJSONString(new AccountOperationResponse(StatusEnum.SESSION_ID_OUTOFDATA)));
        }
        return null;
    }
}
