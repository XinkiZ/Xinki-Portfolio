package com.xinki.portfolio.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * Fixes MyBatis-Plus 3.5.5 compatibility with Spring Framework 6.1+.
 * MyBatis-Plus 3.5.5 stores factoryBeanObjectType as String, but Spring 6.1 expects Class.
 * This processor converts String -> Class before any other BFPP reads the attribute.
 */
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