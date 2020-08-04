package bobking.icu.mall.config;

import bobking.icu.mall.component.JwtAuthenticationTokenFilter;
import bobking.icu.mall.component.RestAuthenticationEntryPoint;
import bobking.icu.mall.component.RestfulAccessDeniedHandler;
import bobking.icu.mall.dto.AdminUserDetails;
import bobking.icu.mall.mbg.model.UmsAdmin;
import bobking.icu.mall.mbg.model.UmsPermission;
import bobking.icu.mall.service.UmsAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private UmsAdminService adminService;
    @Autowired
    private RestfulAccessDeniedHandler restfulAccessDeniedHandler;
    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    /**
     * 通过重载，配置user-details服务
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService())
                .passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //禁用csrf防护功能
        http.csrf()
                .disable()
                //基于token，所以不需要session
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                //配置请求级别的安全性细节
                .authorizeRequests()
                //对静态资源不设限
                .antMatchers(HttpMethod.GET,
                        "/",
                        "/*html",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js",
                        "/swagger-resources/**",
                        "/v2/api-docs/**"
                )
                .permitAll()
                //对admin的登录和注册不设限
                .antMatchers("/admin/login", "/admin/register")
                .permitAll()
                .antMatchers(HttpMethod.OPTIONS) //跨域请求会先进行一次options请求
                .permitAll()
//                .antMatchers("/**")
//                .permitAll()
                //没有定义的其他请求要求认证用户才可访问
                .anyRequest()
                .authenticated();
        //禁用缓存
        http.headers().cacheControl();
        //添加JWT Filter
        http.addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        //添加自定义未授权和未登录结果返回
        http.exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler)
                .authenticationEntryPoint(restAuthenticationEntryPoint);
    }

    /**
     * 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 自定义一个UserDetailsService
     */
    @Bean
    public UserDetailsService userDetailsService() {
        //这里实际上是使用lambda表达式实现了一个匿名内部类，该内部类重载了loadUserByUsername(String username)方法
        return username -> {
            UmsAdmin admin = adminService.getAdminByUsername(username);
            if (admin != null) {
                List<UmsPermission> permissionList = adminService.getPermissionList(admin.getId());
                return new AdminUserDetails(admin, permissionList);
            }
            throw new UsernameNotFoundException("用户名或密码错误");
        };
    }

    /**
     * 在用户名和密码校验前添加的过滤器，如果有jwt的token，会自行根据token信息进行登录。
     */
    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter() {
        return new JwtAuthenticationTokenFilter();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
