package org.bndtools.remoteinstall.dto;

import org.osgi.dto.DTO;

public final class RemoteRuntimeConfiguration extends DTO { 

    public String name;
    public String host;
    public int    port;
    public int    timeout;

}
