package com.example.demo.config;

import org.javalite.activejdbc.Base;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import javax.sql.DataSource;
import java.io.IOException;

@Component
public class DbFilter implements Filter {

    @Autowired(required = false)
    private DataSource dataSource;

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String user;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name}")
    private String driver;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            if (dataSource != null) {
                Base.open(dataSource);
            } else {
                Base.open(driver, url, user, password);
            }
            chain.doFilter(request, response);
        } finally {
            Base.close();
        }
    }
}