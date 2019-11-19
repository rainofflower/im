package com.yanghui.im.server;

import com.yanghui.im.distributed.ServiceRouter;
import io.netty.util.concurrent.Future;
import lombok.Data;

/**
 * 分布式session
 */
@Data
public class DistributedSession extends AbstractSession{

    private long nodeId;

    private String userId;

    private transient ServiceRouter serviceRouter;

    public DistributedSession(String sessionId, long nodeId, String userId){
        setSessionId(sessionId);
        this.nodeId = nodeId;
        this.userId = userId;
    }

    @Override
    public Future writeAndFlush(Object pkg) {
        if(serviceRouter == null){
            return null;
        }
        return serviceRouter.writeAndFlush(this.nodeId,pkg);
    }

}