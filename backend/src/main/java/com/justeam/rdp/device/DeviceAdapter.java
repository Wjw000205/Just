package com.justeam.rdp.device;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** 平台设备协议适配边界。厂商 SDK、Modbus、OPC UA 必须各自实现并通过联调，不能由通用 HTTP 接口冒充。 */
public interface DeviceAdapter {
    String protocol();
    boolean healthy(Map<String,Object> configuration);
    Sample poll(DeviceService.PollingTarget target);

    record Sample(String metricName, BigDecimal metricValue, String textValue, String unit,
                  String quality, Instant measuredTime, Map<String,Object> rawData) {}
}
