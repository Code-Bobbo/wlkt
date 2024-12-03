package com.itheima;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

public class ThreadPoolPerformanceTest {
    // 最大执行次数
    public static final int maxCount = 1000;

    public static void main(String[] args) throws InterruptedException {
        // 线程测试代码
        ThreadPerformanceTest();

        // 线程池测试代码
        ThreadPoolPerformanceTest1();
    }

    /**
     * 线程池性能测试
     */
    private static void ThreadPoolPerformanceTest1() throws InterruptedException {
        // 开始时间
        long stime = System.currentTimeMillis();
        // 业务代码
        ThreadPoolExecutor tp = new ThreadPoolExecutor(16, 16, 0,
                TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        for (int i = 0; i < maxCount; i++) {
            tp.execute(new PerformanceRunnable());
        }
        tp.shutdown();
        tp.awaitTermination(1, TimeUnit.SECONDS);  // 等待线程池执行完成
        // 结束时间
        long etime = System.currentTimeMillis();
        // 计算执行时间
        System.out.printf("线程池执行时长：%d 毫秒.", (etime - stime));
        System.out.println();
    }

    /**
     * 线程性能测试
     */
    private static void ThreadPerformanceTest() {
        // 开始时间
        long stime = System.currentTimeMillis();
        // 执行业务代码
        for (int i = 0; i < maxCount; i++) {
            Thread td = new Thread(new PerformanceRunnable());
            td.start();
            try {
                td.join(); // 确保线程执行完成
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 结束时间
        long etime = System.currentTimeMillis();
        // 计算执行时间
        System.out.printf("线程执行时长：%d 毫秒.", (etime - stime));
        System.out.println();
    }
    // 业务执行类
    static class PerformanceRunnable implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < maxCount; i++) {
                long num = i * i + i;
            }
        }
    }
}
