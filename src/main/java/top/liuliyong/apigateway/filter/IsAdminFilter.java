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
import top.liuliyong.account.dao.model.Account;
import top.liuliyong.apigateway.config.NeedAdminAuthPathEnum;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * @Author liyong.liu
 * @Date 2019-05-10
 **/
@Component
@Slf4j
public class IsAdminFilter extends ZuulFilter {
    public IsAdminFilter(AccountClient ssoAppController) {
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
        String currentMethod = request.getMethod();
        return NeedAdminAuthPathEnum.isContains(uri, currentMethod);
    }

    @Override
    public Object run() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        String session_id = Objects.requireNonNull(request).getHeader("session_id");
        if (session_id == null || session_id.trim().length() == 0) {
            log.warn("Session_id is null!");
            requestContext.setSendZuulResponse(false);
            requestContext.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            requestContext.setResponseBody(JSON.toJSONString(new AccountOperationResponse(StatusEnum.LACK_OF_INFORMATION)));

        }
        //验证该session_id是否有效并且是否是管理员
        AccountOperationResponse response = ssoAppController.logincheck(session_id);
        if (response.getRtn() == 0) {
            Account account = JSON.parseObject(JSON.toJSONString(response.getData()), Account.class);
            List<String> auth = account.getAccount_permission().parallelStream().filter(t -> t.equals("admin_auth")).collect(Collectors.toList());
            if (auth.size() == 0) {
                log.warn(String.format("Account %s has no admin auth!", account.toString()));
                requestContext.setSendZuulResponse(false);
                requestContext.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
                requestContext.setResponseBody(JSON.toJSONString(new AccountOperationResponse(StatusEnum.NO_AUTH)));
            }
        } else {
            //该session_id未登录
            log.warn("Not Online!");
            requestContext.setSendZuulResponse(false);
            requestContext.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            requestContext.setResponseBody(JSON.toJSONString(new AccountOperationResponse(StatusEnum.NOT_ONLINE)));
        }
        return null;
    }
}
