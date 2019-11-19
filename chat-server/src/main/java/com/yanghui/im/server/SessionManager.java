package com.yanghui.im.server;

import com.yanghui.im.bean.User;
import com.yanghui.im.distributed.Node;
import com.yanghui.im.distributed.ServiceRouter;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@Service
public class SessionManager implements Manager{

    //会话集合
    private final ConcurrentMap<String, LocalSession> localSessionMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private Node node;

    @Autowired
    private ServiceRouter serviceRouter;

    @Value("${service.session.timeout}")
    private long sessionTimeOut;

    /**
     * 保存session到localSessionMap
     * 同时保存到分布式缓存中
     * 1.一份存入string结构中，以sessionId为key,并设置过期时间，实现session过期机制
     * 2.另存一份到set中，以userId为key，实现根据uid查询session，然后根据session判断数据应该转发给集群中的那些节点
     */
    public void addSession(LocalSession s) {
        String sessionId = s.getSessionId();
        localSessionMap.put(sessionId, s);
        String uid = s.getUser().getUid();
        DistributedSession distributedSession = new DistributedSession(sessionId, node.getId(), uid);
        redisTemplate.opsForValue().set(sessionId, distributedSession, sessionTimeOut, TimeUnit.MINUTES);
        redisTemplate.opsForSet().add(uid, distributedSession);
        log.info("用户登录:id= " + uid
                + "   在线总数: " + localSessionMap.size());

    }

    /**
     *
     * 获取本地session
     * @param sessionId
     * @return session
     */
    public Session getLocalSession(String sessionId){
        return localSessionMap.get(sessionId);
    }

    /**
     * 获取session对象
     * 先从本地查找session，如果找不到，再到缓存中查询
     */
    public Session getSession(String sessionId) {
        Session localSession = getLocalSession(sessionId);
        if(localSession != null){
            return localSession;
        } else {
            Object deserializeObj = redisTemplate.opsForValue().get(sessionId);
            if(deserializeObj != null){
                return (DistributedSession)deserializeObj;
            }
            return null;
        }
    }

    /**
     * session是否有效
     */
    public boolean isValid(String sessionId) {
        return redisTemplate.opsForValue().get(sessionId) == null ? false : true;
    }

    /**
     * 根据用户id，去缓存中获取所有的session
     */
    public Set<DistributedSession> getSessionsByUserId(String userId) {
        Set<DistributedSession> sets = redisTemplate.opsForSet().members(userId);
        //这里为每个session设置相同的serviceRouter是为了照顾session接口的writeAndFlush方法，可去掉。。
        if(!CollectionUtils.isEmpty(sets)){
            for(DistributedSession session : sets){
                session.setServiceRouter(serviceRouter);
            }
        }
        return sets;
//        List<LocalSession> list1 = localSessionMap.values()
//                .stream()
//                .filter(s -> s.getUser().getUid().equals(userId))
//                .collect(Collectors.toList());
//        return list1;
    }

    /**
     * 删除session
     * @param session
     * @param expired 要删除的session是否过期
     */
    public void removeSession(Session session, boolean expired) {
        String sessionId = session.getSessionId();
        try {
            if (localSessionMap.containsKey(sessionId)) {
                //本地session
                LocalSession s = localSessionMap.get(sessionId);
                ChannelFuture f = s.close();
                f.addListener((ChannelFuture future) -> {
                    if (!future.isSuccess()) {
                        log.error("本地channel关闭失败");
                    }
                });
                localSessionMap.remove(sessionId);
                log.info("用户下线:id= " + s.getUser().getUid()
                        + "   在线总数: " + localSessionMap.size());
            }
            if (!expired) {
                //session未过期执行删除session操作时同时也删除缓存中的session
                redisTemplate.delete(sessionId);
            }
        }finally {
            //以下为移除缓存中set里的session
            if(session instanceof LocalSession){
                redisTemplate.opsForSet().remove(new DistributedSession(sessionId,node.getId(),((LocalSession) session).getUser().getUid()));
            }
            if(session instanceof DistributedSession){
                redisTemplate.opsForSet().remove(session);
            }
        }
    }


    public boolean hasLogin(User user) {
        Iterator<Map.Entry<String, LocalSession>> it =
                localSessionMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LocalSession> next = it.next();
            String sessionId = next.getValue().getSessionId();
            if (sessionId.equals(user.getSessionId())) {
                return true;
            }
        }

        return false;
    }


}
