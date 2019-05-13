package top.liuliyong.apigateway.config;

import lombok.Getter;

/**
 * @Author liyong.liu
 * @Date 2019-05-10
 **/
@Getter
public enum NeedAdminAuthPathEnum {
    FIND_ALL_ACCOUNT("/account-server/account/findAllAccount", "GET"), FROZE_ACCOUNT("/account-server/account/frozeAccount", "GET"), UNFROZE_ACCOUNT("/account-server/account/unFrozeAccount", "GET"), DELETE_ACCOUNT("/account-server/account", "GET");


    private String path;
    private String method;

    NeedAdminAuthPathEnum(String path, String method) {
        this.path = path;
        this.method = method;
    }

    public static boolean isContains(String path, String method) {
        for (NeedAdminAuthPathEnum enumParam : NeedAdminAuthPathEnum.values()) {
            if (path.equalsIgnoreCase(enumParam.getPath()) && method.equalsIgnoreCase(enumParam.getMethod())) {
                return true;
            }
        }
        return false;
    }
}
