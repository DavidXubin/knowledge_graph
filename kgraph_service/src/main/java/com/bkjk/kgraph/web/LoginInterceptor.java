package com.bkjk.kgraph.web;

import com.bkjk.platform.exception.PlatformException;
import com.bkjk.platform.passport.interceptors.PassportAuthenticationInterceptor;
import com.bkjk.platform.passport.interceptors.PassportUserHolder;
import com.bkjk.platform.passport.interceptors.UcPassportAuthenticationInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class LoginInterceptor extends UcPassportAuthenticationInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        return super.preHandle(request, response, handler);
    }

    /*
    @Override
    protected void handleError(HttpServletRequest request, HttpServletResponse response, PlatformException exception) throws IOException {
        if (response != null) {
            writeResponse(response, "{\"errorCode\":403}");
        }
    }
     */

    private void writeResponse(HttpServletResponse response, String content) throws IOException {
        response.reset();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter pw = response.getWriter();
        // 返回 403 提示 没有对帐系统权限。请联系 G-DATA-PM@bkjk.com
        pw.write(content);
        pw.flush();
        pw.close();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        HttpSession session = request.getSession();

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        HttpSession session = request.getSession();
    }
}
