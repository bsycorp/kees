package com.bsycorp.kees;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Locker {
    private ConcurrentHashMap<String, Lock> locks;

    public Locker(){
        locks = new ConcurrentHashMap<>();
    }

    public Lock getLock(String key) {
        Lock initValue = new ReentrantLock();
        //returns the current lock or null if was absent
        Lock lock = locks.putIfAbsent(key, initValue);
        if (lock == null) {
            lock = initValue;
        }
        return lock;
    }

}
