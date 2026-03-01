package cn.smxy.forum.config;

import cn.smxy.forum.filter.JWTFilter;
import cn.smxy.forum.service.Impl.AccessDeniedHandlerImpl;
import cn.smxy.forum.service.Impl.AuthenticationEntryPointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SpringSecurity的配置类要求继承WebSecurityConfigurerAdapter
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JWTFilter jwtFilter;
    @Autowired
    private AuthenticationEntryPointImpl authenticationEntryPoint;
    @Autowired
    private AccessDeniedHandlerImpl accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 1. 启用CORS并放行OPTIONS请求
                .cors().and()  // 启用CorsConfig配置
                // 2. 关闭CSRF和Session
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 3. 异常处理
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .and()
                // 4. 权限规则
                .authorizeRequests()
                // 放行预检请求、登录、注册、静态资源等
                .antMatchers(HttpMethod.OPTIONS).permitAll()  // 关键：放行OPTIONS
                .antMatchers("/login", "/register","/getCaptchaImage").anonymous()
                .antMatchers(
                        HttpMethod.GET,
                        "/", "/*.html", "/**/*.html", "/**/*.css", "/**/*.js", "/profile/**"
                ).permitAll()
                .antMatchers(
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/*/api-docs",
                        "/druid/**",
                        "/upload/**",
                        "/ws/**"
                ).permitAll()
                // 其他请求需认证
                .anyRequest().authenticated()
                .and()
                // 5. 添加JWT过滤器
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }


    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
