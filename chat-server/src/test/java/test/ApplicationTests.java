package test;

import com.yanghui.im.ServerApplication;
import com.yanghui.im.bean.User;
import com.yanghui.im.server.DistributedSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServerApplication.class)
public class ApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * String
     */
    @Test
    public void redisTest() {
        // redis存储数据
        String key = "address";
        redisTemplate.opsForValue().set(key, "sz-lib");
//        // 获取数据
        String value = (String) redisTemplate.opsForValue().get(key);
//        if(value == null){
//            System.out.println("缓存中key为" + key + "的值为null");
//        }
        System.out.println("获取缓存中key为" + key + "的值为：" + value);
//        redisTemplate.expire(key,5,TimeUnit.SECONDS);
//
//        User user = new User();
//        user.setUsername("lalala");
//        user.setSex(18);
//        user.setId(1L);
//        String userKey = "lalala";
//        redisTemplate.opsForValue().setAttribute(userKey, user);
//        User newUser = (User) redisTemplate.opsForValue().getAttribute(userKey);
//        System.out.println("获取缓存中key为" + userKey + "的值为：" + newUser);
    }

    /**
     * list
     */
    @Test
    public void testList(){
        String listKey = "list";
        redisTemplate.opsForList().rightPushAll(listKey,1,2,1,5);
        redisTemplate.opsForList().rightPush(listKey,3);
        User user = new User();
        user.setUid("1");
        user.setNickName("redis");
        redisTemplate.opsForList().rightPush(listKey,user);
        List list = redisTemplate.opsForList().range(listKey, 0, -1);
        for(Object i : list){
            System.out.print(i+" ");
        }
        System.out.println();
    }

    /**
     * list结构
     * 注意：list为空时，整个key会被删除，使用keys将找不到该key
     */
    @Test
    public void testList2(){
        String listKey = "list";
        //设置key失效时间,也可使用expireAt方法
//        redisTemplate.expire(listKey,5, TimeUnit.SECONDS);
        //删除list中所有元素的方法,使用trim,start大于end,且都大于等于0
//        redisTemplate.opsForList().trim(listKey,1,0);
//        Set keys = redisTemplate.keys("list*");
//        for(Object i : keys){
//            System.out.print(i+" ");
//        }
        System.out.println();
//        Object left = redisTemplate.opsForList().leftPop(listKey);
//        System.out.println("leftPop弹出的第一个元素："+left);
        Object right = redisTemplate.opsForList().rightPop(listKey);
//        System.out.println("rightPop弹出的最后一个元素："+right);
        List list = redisTemplate.opsForList().range(listKey, 0, -1);
        for(Object i : list){
            System.out.print(i+" ");
        }
        System.out.println();
    }
    /**
     * list阻塞队列
     */
    @Test
    public void testBlockList() throws InterruptedException {
        String listKey = "list";
        redisTemplate.opsForList().trim(listKey,1,0);
        long blockSeconds = 10;
        System.out.println("阻塞获取list中头部元素中...");
        Thread popThread = new Thread(() -> {
            //注意如果阻塞命令的阻塞时长大于配置的连接超时时长(如未配置，lettuce给的默认超时时长为60s)，会抛出RedisCommandTimeoutException异常
            //但是只要命令成功提交给了redis服务器，阻塞的时间到了，redis服务器还是会执行阻塞命令，只是java代码里无法获取到响应结果罢了
            Object leftPop = redisTemplate.opsForList().leftPop(listKey, blockSeconds, TimeUnit.SECONDS);
            System.out.println("获取到元素："+leftPop);
            List list2 = redisTemplate.opsForList().range(listKey, 0, -1);
            for(Object i : list2){
                System.out.print(i+" ");
            }
            System.out.println();
        });
        popThread.start();
        Thread pushThread = new Thread(() -> {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(blockSeconds-1));
            redisTemplate.opsForList().rightPushAll(listKey,2,3,5);
        });
        pushThread.start();
        popThread.join();
        pushThread.join();
//        redisTemplate.opsForList().leftPushAll(listKey,2);
//        List list = redisTemplate.opsForList().range(listKey, 0, -1);
//        for(Object i : list){
//            System.out.print(i+" ");
//        }
//        redisTemplate.opsForList().remove(listKey,1,2);
    }

    /**
     * redis自增
     * 多指令存在的并发问题
     */
    @Test
    public void concurrentTest() throws InterruptedException {
        String num = "num";
        redisTemplate.opsForValue().set(num, 0);
        System.out.println(redisTemplate.opsForValue().get(num));
        int threadNum = 100;
        ExecutorService pool = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(threadNum);
        for(int i = 0; i < threadNum;i++) {
            try{
                pool.execute(() -> {
                    try {
                        Integer n = (Integer) redisTemplate.opsForValue().get(num);
                        ++n;
                        redisTemplate.opsForValue().set(num, n);
                    } finally {
                        latch.countDown();
                    }
                });
            }catch (Throwable e){
                latch.countDown();
            }
        }
        latch.await();
        System.out.println(redisTemplate.opsForValue().get(num));
    }

    /**
     * redis自增
     * 多指令改用incr/decr,利用redis单指令原子性解决自增并发问题
     */
    @Test
    public void incrTest() throws InterruptedException {
        String num = "num";
        redisTemplate.opsForValue().set(num, 0);
        System.out.println(redisTemplate.opsForValue().get(num));
        int threadNum = 100;
        ExecutorService pool = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(threadNum);
        for(int i = 0; i < threadNum;i++){
            try{
                pool.execute(()->{
                    try{
                        redisTemplate.opsForValue().increment(num);
                    }finally {
                        latch.countDown();
                    }
                });
            }catch (Throwable e){
                latch.countDown();
            }
        }
        latch.await();
        System.out.println(redisTemplate.opsForValue().get(num));
    }

    public Set<DistributedSession> getSessionsByUserId(String userId) {
        redisTemplate.opsForSet().add(userId, new DistributedSession("1000",1, userId));
        redisTemplate.opsForSet().add(userId, new DistributedSession("1001",1, userId));
        redisTemplate.opsForSet().add(userId, new DistributedSession("1002",3, userId));
        Set<DistributedSession> sets = redisTemplate.opsForSet().members(userId);
        if(CollectionUtils.isEmpty(sets)){
            return null;
        }
        return sets;
//        List<LocalSession> list1 = localSessionMap.values()
//                .stream()
//                .filter(s -> s.getUser().getUid().equals(userId))
//                .collect(Collectors.toList());
//        return list1;
    }

}