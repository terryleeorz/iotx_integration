package com.aliyun.city.example;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.cloudapi.sdk.model.ApiResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.google.common.collect.Maps;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author libin.lb
 * @version 1.0.0
 * @date 2018/12/17 上午11:29
 */
@Controller
public class LoginController {

    private SyncApiClient syncClient;

    public LoginController() {
        IoTApiClientBuilderParams builderParams = new IoTApiClientBuilderParams();
        builderParams.setAppKey("请输入真实的AppKey");
        builderParams.setAppSecret("请输入真实的AppSecret");
        syncClient = new SyncApiClient(builderParams);
    }

    @RequestMapping("/index")
    public String index(Model model) {
        model.addAttribute("singlePerson","Tom");
        return "index";
    }

    @RequestMapping("/login")
    public String login(HttpServletRequest request) {
        boolean login = isLogin(request);
        if (!login) {
            return "redirect:http://linkcity.aliplus.com/api/tac/authorize?responseType=code&clientId=xxxxxxxx&state=xyz&redirectUrl=http%3A%2F%2Flocalhost%3A8080%2Fcallback";
        }
        return "index";
    }

    @RequestMapping("/callback/logout")
    public String logout(HttpServletRequest request) {
        String loginUser = (String)request.getSession().getAttribute("loginUser");
        System.out.println(loginUser);
        return "index";
    }

    @RequestMapping("/callback")
    public String callback(HttpServletRequest request, String code, String state) {
        System.out.println(code);
        System.out.println(state);

        try {
            String data = getAccessToken(code);
            JSONObject jsonObject = JSON.parseObject(data);
            Integer retCode = (Integer)jsonObject.get("code");
            if (retCode != null && retCode != 200) {
                System.out.println("Request Failed. Result:"+ jsonObject);
                return "index";
            }
            JSONObject retData = (JSONObject)jsonObject.get("data");
            String accessToken = (String)retData.get("accessToken");
            String userId = (String)retData.get("userId");

            User user = getUserInfo(accessToken, userId);
            request.getSession().setAttribute("loginUser", user.getId());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "index";
    }

    private String getAccessToken(String code) {
        String data = null;
        try {
            IoTApiRequest request = new IoTApiRequest();
            request.setApiVer("0.1.2");
            request.setId(UUID.randomUUID().toString());
            HashMap<String, Object> params = Maps.newHashMap();
            params.put("authCode", code);
            params.put("grantType", "authorization_code");
            params.put("clientId", "xxxxxxxx");
            params.put("redirectUrl", "http://localhost:8080/callback");
            request.setParams(params);
            ApiResponse apiResponse = syncClient.postBody("api.citylink.aliplus.com", "/auth/token/get", request);
            data = new String(apiResponse.getBody(), "utf-8");
            System.out.println(data);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return data;
    }

    private User getUserInfo(String accessToken, String userId) throws UnsupportedEncodingException {
        IoTApiRequest req2 = new IoTApiRequest();
        req2.setApiVer("0.1.2");
        req2.setId(UUID.randomUUID().toString());
        HashMap<String, Object> paramsMap = Maps.newHashMap();
        paramsMap.put("accessToken", accessToken);
        paramsMap.put("userId", userId);
        paramsMap.put("clientId", "5tb6mSGqb5zduS21723FJ8fg");
        req2.setParams(paramsMap);
        ApiResponse resp = syncClient.postBody("api.citylink.aliplus.com", "/auth/userinfo/get", req2);
        String ret = new String(resp.getBody(), "utf-8");
        System.out.println(ret);
        JSONObject jsonObject = JSON.parseObject(ret);
        Integer retCode = (Integer)jsonObject.get("code");
        if (retCode != null && retCode != 200) {
            System.out.println("Request Failed. Result:"+ jsonObject);
            throw new RuntimeException("Request Failed.");
        }
        JSONObject retData = (JSONObject)jsonObject.get("data");
        System.out.println(retData);
        User user = new User();
        user.setId((String)retData.get("userId"));
        user.setName((String)retData.get("userName"));
        return user;
    }

    private boolean isLogin(HttpServletRequest request) {
        return false;
    }
}

class User{
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}