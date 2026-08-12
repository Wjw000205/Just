package com.justeam.rdp.device;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SimulatorDeviceAdapter implements DeviceAdapter {
    @Override public String protocol(){return "SIMULATOR";}
    @Override public boolean healthy(Map<String,Object> configuration){return configuration!=null;}
    @Override public Sample poll(DeviceService.PollingTarget target){
        Map<String,Object> config=target.connectionConfig();
        String metric=String.valueOf(config.getOrDefault("metricName","simulatedValue"));
        String unit=String.valueOf(config.getOrDefault("unit","unit"));
        BigDecimal base=decimal(config.get("baseValue"),BigDecimal.valueOf(50));
        BigDecimal amplitude=decimal(config.get("amplitude"),BigDecimal.ONE);
        double wave=Math.sin(System.currentTimeMillis()/5000.0);
        BigDecimal value=base.add(amplitude.multiply(BigDecimal.valueOf(wave))).setScale(6,java.math.RoundingMode.HALF_UP);
        Map<String,Object> raw=new LinkedHashMap<>();raw.put("adapter","SIMULATOR");raw.put("deviceCode",target.deviceCode());raw.put("generatedAt",Instant.now().toString());
        return new Sample(metric,value,null,unit,"GOOD",Instant.now(),raw);
    }
    private BigDecimal decimal(Object value,BigDecimal fallback){try{return value==null?fallback:new BigDecimal(String.valueOf(value));}catch(Exception ex){return fallback;}}
}
