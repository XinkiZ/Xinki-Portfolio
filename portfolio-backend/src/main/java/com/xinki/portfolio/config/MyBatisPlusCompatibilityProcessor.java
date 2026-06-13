package com.xinki.portfolio.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/**
 * Fixes MyBatis-Plus 3.5.5 compatibility with Spring Framework 6.2+.
 * MyBatis-Plus stores factoryBeanObjectType as String, but Spring 6.2 expects Class.
 * 
 * Must be a regular BeanFactoryPostProcessor (NOT BeanDefinitionRegistryPostProcessor)
 * because MyBatis MapperScannerConfigurer is a non-ordered BDRPP that runs in the
 * reiterate loop AFTER all Ordered BDRPPs. This processor runs as a BFPP after ALL
 * BDRPP phases complete, ensuring mapper beans are already registered.
 */
@Component
public class MyBatisPlusCompatibilityProcessor implements BeanFactoryPostProcessor, PriorityOrdered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry registry) {
            for (String name : registry.getBeanDefinitionNames()) {
                BeanDefinition bd = registry.getBeanDefinition(name);
                if (bd.hasAttribute("factoryBeanObjectType")) {
                    Object val = bd.getAttribute("factoryBeanObjectType");
                    if (val instanceof String className) {
                        try {
                            bd.setAttribute("factoryBeanObjectType", Class.forName(className));
                        } catch (ClassNotFoundException e) {
                            bd.removeAttribute("factoryBeanObjectType");
                        }
                    }
                }
            }
        }
    }
}