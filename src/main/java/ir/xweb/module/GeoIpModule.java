package ir.xweb.module;

import ir.xweb.util.Tools;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.util.HashMap;

public class GeoIpModule extends Module {

    public GeoIpModule(final Manager manager, final ModuleInfo info, final ModuleParam properties) {
        super(manager, info, properties);
    }

    private String getIndex(String language) {
        language += ',';

        for(String key:getProperties().keySet()) {
            if((key + ",").indexOf(language) > -1) {
                return getProperties().getString(key, null);
            }
        }

        return null;
    }

    @Override
    public void process(final ServletContext context, final HttpServletRequest request,
                        final HttpServletResponse response, final ModuleParam params,
                        final HashMap<String, FileItem> files) throws IOException {

        // try to load with cooke
        String language = null;

        final Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(Cookie c:cookies) {
                if("language".equals(c.getName())) {
                    language = c.getValue();
                    break;
                }
            }
        }

        if(language != null) {
            final String index = getIndex(language);
            if(index != null) {
                response.sendRedirect(request.getContextPath() + index);
                return;
            }
        }

        // if cookie not set
        language = getCountryCode(request);
        if(language != null) {
            final String index = getIndex(language);
            if(index != null) {
                response.addCookie(new Cookie("language", language));
                response.sendRedirect(request.getContextPath() + index);
                return;
            }
        }

        final String defaultIndex = getIndex("default");
        if(defaultIndex != null) {
            response.sendRedirect(request.getContextPath() + defaultIndex);
        }
    }

    public String getCountryCode(final HttpServletRequest request) {
        try {
            final URL url = new URL("http://freegeoip.net/csv/" + getClientIpAddr(request));

            final InputStream is = url.openStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            final String csv = reader.readLine();
            is.close();

            final String[] parts = csv.split(",");
            final String countryCode = parts[2];

            // is supposed to be like "US"
            if(countryCode.length() == 4) {
                return countryCode.substring(1, 3);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            // ignore
        }
        return null;
    }

    private String getClientIpAddr(final HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
