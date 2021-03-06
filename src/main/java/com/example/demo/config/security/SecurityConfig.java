package com.example.demo.config.security;

import com.example.demo.config.jwt.filters.JwtFilter;
import com.example.demo.config.jwt.filters.JwtRequestMatcher;
import com.example.demo.config.jwt.handlers.JwtAuthenticationFailureHandler;
import com.example.demo.config.jwt.providers.JwtProvider;
import com.example.demo.config.security.filters.CustomFilterInvocationSecurityMetadataSource;
import com.example.demo.config.security.filters.CustomVote;
import com.example.demo.config.security.filters.FormLoginFilter;
import com.example.demo.config.security.handlers.FormLoginAuthenticationFailureHandler;
import com.example.demo.config.security.handlers.FormLoginAuthenticationSuccessHandler;
import com.example.demo.config.security.providers.FormLoginProvider;
import com.example.demo.domain.resource.ResourceMapper;
import com.example.demo.domain.role.RoleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Created By iljun
 * User : iljun
 * Date : 18. 5. 1
 * Time: 오후 12:39
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder getPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper getObjectMapper(){
        return new ObjectMapper();
    }

    @Bean
    public AuthenticationManager getAuthenticationManager() throws Exception {
        return super.authenticationManagerBean();
    }

    @Autowired
    private FormLoginAuthenticationSuccessHandler formLoginAuthenticationSuccessHandler;

    @Autowired
    private FormLoginAuthenticationFailureHandler formLoginAuthenticationFailureHandler;

    @Autowired
    private FormLoginProvider formLoginProvider;

    @Autowired
    private JwtAuthenticationFailureHandler jwtAuthenticationFailureHandler;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RoleMapper roleMapper;

    protected FormLoginFilter getFormLoginFilter() throws Exception{
        FormLoginFilter filter = new FormLoginFilter("/formLogin",formLoginAuthenticationSuccessHandler,formLoginAuthenticationFailureHandler);
        filter.setAuthenticationManager(super.authenticationManagerBean());
        return filter;
    }

    protected JwtFilter getJwtFilter() throws Exception{
        JwtRequestMatcher jwtRequestMatcher = new JwtRequestMatcher(Arrays.asList("/formLogin"),"/**");
        JwtFilter jwtFilter = new JwtFilter(jwtRequestMatcher,jwtAuthenticationFailureHandler);
        jwtFilter.setAuthenticationManager(super.authenticationManagerBean());

        return jwtFilter;
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        List<AccessDecisionVoter<? extends Object>> decisionVoters
                = Arrays.asList(
                new CustomVote());
        return new UnanimousBased(decisionVoters);
    }

    @Bean
    protected FilterSecurityInterceptor getFilterInvocationSecurityMetadataSource() throws Exception{
        FilterSecurityInterceptor filterSecurityInterceptor = new FilterSecurityInterceptor();
        filterSecurityInterceptor.setAuthenticationManager(super.authenticationManagerBean());//authentication Manger
        filterSecurityInterceptor.setSecurityMetadataSource(new CustomFilterInvocationSecurityMetadataSource(roleMapper));//대상 정보
        filterSecurityInterceptor.setAccessDecisionManager(accessDecisionManager());//판단 주체
        return filterSecurityInterceptor;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .authenticationProvider(this.formLoginProvider)
                .authenticationProvider(this.jwtProvider);

    }

    @Override
    public void configure(WebSecurity web) throws Exception {
//        super.configure(web);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http
                .csrf().disable();

        http
                .headers().frameOptions().disable();

        http
                .addFilterBefore(getFormLoginFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(getJwtFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(getFilterInvocationSecurityMetadataSource(),FilterSecurityInterceptor.class);

    }

}
