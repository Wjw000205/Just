package com.justeam.rdp.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Resolves a client address only from a proxy header when the deployment explicitly trusts its ingress. */
@Component
public class ClientIpResolver {
    private final RdpProperties properties;

    public ClientIpResolver(RdpProperties properties) { this.properties = properties; }

    public String resolve(HttpServletRequest request) {
        String remote=request.getRemoteAddr()==null?"unknown":request.getRemoteAddr();
        if(!properties.security().trustProxyHeaders())return remote;
        String forwarded=request.getHeader("X-Real-IP");
        return validIpLiteral(forwarded)?forwarded.trim():remote;
    }

    private boolean validIpLiteral(String raw){
        if(raw==null)return false;
        String value=raw.trim();
        if(value.length()<2||value.length()>64||!value.matches("[0-9A-Fa-f:.]+"))return false;
        if(value.indexOf(':')>=0){try{java.net.InetAddress.getByName(value);return true;}catch(Exception ignored){return false;}}
        String[] octets=value.split("\\.",-1);if(octets.length!=4)return false;
        for(String octet:octets){try{if(octet.isEmpty()||octet.length()>3||Integer.parseInt(octet)>255)return false;}catch(NumberFormatException ignored){return false;}}
        return true;
    }
}
