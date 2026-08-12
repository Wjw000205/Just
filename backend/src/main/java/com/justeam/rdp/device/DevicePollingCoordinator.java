package com.justeam.rdp.device;

import com.justeam.rdp.audit.AuditService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DevicePollingCoordinator {
    private final DeviceService devices;private final Map<String,DeviceAdapter> adapters;private final AuditService audit;
    public DevicePollingCoordinator(DeviceService devices,List<DeviceAdapter> adapters,AuditService audit){
        this.devices=devices;this.adapters=adapters.stream().collect(Collectors.toUnmodifiableMap(a->a.protocol().toUpperCase(java.util.Locale.ROOT),Function.identity()));this.audit=audit;
    }
    @Scheduled(initialDelayString="${rdp.device.poll-initial-delay-ms:10000}",fixedDelayString="${rdp.device.poll-delay-ms:5000}")
    public void pollEnabled(){for(DeviceService.PollingTarget target:devices.claimPollingTargets(50))poll(target);}
    public void pollDevice(long id){poll(devices.claimPollingTarget(id));}
    private void poll(DeviceService.PollingTarget target){DeviceAdapter adapter=adapters.get(target.protocol().toUpperCase(java.util.Locale.ROOT));
        if(adapter==null){devices.adapterFailed(target,"未安装协议适配器："+target.protocol());return;}
        try{if(!adapter.healthy(target.connectionConfig()))throw new IllegalStateException("适配器健康检查失败");DeviceAdapter.Sample sample=adapter.poll(target);devices.ingestAdapter(target.id(),UUID.randomUUID(),new DeviceService.MeasurementBody(sample.metricName(),sample.metricValue(),sample.textValue(),sample.unit(),sample.quality(),sample.measuredTime(),sample.rawData()),adapter.protocol());}
        catch(Exception ex){devices.adapterFailed(target,ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage());audit.recordAs(null,"adapter:"+target.protocol(),"POLL_FAILED","DEVICE","设备自动采集失败",Map.of("deviceId",target.id(),"deviceCode",target.deviceCode(),"error",String.valueOf(ex.getMessage())));}}
}
