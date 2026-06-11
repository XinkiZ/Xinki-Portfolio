package com.xinki.portfolio.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    /** Fix MyBatis-Plus 3.5.5 compatibility with Spring Boot 3.2+: remove invalid factoryBeanObjectType.
     * Must run BEFORE DatabaseInitializationDependencyConfigurer via PriorityOrdered. */
    @Bean
    public static BeanDefinitionRegistryPostProcessor mybatisMapperFixer() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                for (String name : registry.getBeanDefinitionNames()) {
                    BeanDefinition bd = registry.getBeanDefinition(name);
                    if (bd.hasAttribute("factoryBeanObjectType")) {
                        Object val = bd.getAttribute("factoryBeanObjectType");
                        if (val instanceof String) {
                            try {
                                bd.setAttribute("factoryBeanObjectType", Class.forName((String) val));
                            } catch (ClassNotFoundException e) {
                                bd.removeAttribute("factoryBeanObjectType");
                            }
                        }
                    }
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {}
        };
    }
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
