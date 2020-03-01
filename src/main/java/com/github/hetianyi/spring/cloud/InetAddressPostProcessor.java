package com.github.hetianyi.spring.cloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.hetianyi.common.util.InetUtil;
import com.github.hetianyi.common.util.StringUtil;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.stereotype.Component;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

/**
 * 处理并影响spring cloud应用注册到注册中心的IP地址。<br/>
 * 使用需要设置环境变量（application.yml和bootstrap.yml无效）:<br/>
 * networkFilterBy = ipAddress|interfaceName<br/>
 * 和<br/>
 * preferredNetwork<br/><br/>
 * 如果networkFilterBy=ipAddress，那么preferredNetwork是特定IP地址前缀，如192.168.<br/>
 * 如果networkFilterBy=interfaceName，那么preferredNetwork是网卡名称，如eth0.<br/>
 */
public class InetAddressPostProcessor
        implements BeanPostProcessor, EnvironmentPostProcessor, Ordered {

    public static final Logger log = LoggerFactory.getLogger(InetAddressPostProcessor.class);

    public static final String proKey = "spring.cloud.inetutils.preferredNetworks";

    private volatile boolean isConfigured = false;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        if (isConfigured) {
            return;
        }

        synchronized (this) {
            isConfigured = true;
            String networkFilterBy = StringUtil.trimSafe(System.getenv("networkFilterBy"));
            String preferredNetwork = StringUtil.trimSafe(System.getenv("preferredNetwork"));
            if (StringUtil.isNullOrEmpty(networkFilterBy) || StringUtil.isNullOrEmpty(preferredNetwork)) {
                if (log.isDebugEnabled()) {
                    log.debug("env parameter \"networkFilterBy\" or \"preferredNetwork\" not provided");
                }
                return;
            }

            List<String> valueList = new ArrayList<>(1);
            if ("ipAddress".equals(networkFilterBy)) {
                valueList.addAll(InetUtil.filterByAddress(Arrays.asList(preferredNetwork)));
            } else if ("interfaceName".equals(networkFilterBy)) {
                valueList.addAll(InetUtil.filterByInterfaceNames(Arrays.asList(preferredNetwork)));
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("invalid env parameter \"networkFilterBy\": {}", networkFilterBy);
                }
                return;
            }
            Map<String, Object> preferredNetworks = Maps.newHashMap();
            preferredNetworks.put(proKey, valueList);
            PropertySource<Map<String, Object>> oldSystemEnvironmentPropertySource =
                    (PropertySource<Map<String, Object>>) environment.getPropertySources()
                            .get(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            Map<String, Object> newSystemEnvironmentPropertySource = new HashMap<>(oldSystemEnvironmentPropertySource
                    .getSource().size() + 1);
            newSystemEnvironmentPropertySource.putAll(oldSystemEnvironmentPropertySource.getSource());
            if (newSystemEnvironmentPropertySource.containsKey(proKey)) {
                log.warn("properties already provided: {} = {}", proKey, newSystemEnvironmentPropertySource.get(proKey));
            }
            newSystemEnvironmentPropertySource.putAll(preferredNetworks);

            SystemEnvironmentPropertySource systemEnvironmentPropertySource =
                    new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            Collections.unmodifiableMap(newSystemEnvironmentPropertySource));
            environment.getPropertySources()
                    .replace(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, systemEnvironmentPropertySource);
        }
    }

    @Override
    public int getOrder() {
        return ConfigFileApplicationListener.DEFAULT_ORDER - 2;
    }
}
