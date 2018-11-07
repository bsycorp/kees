package com.bsycorp.kees.data;

import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

//Used to wrap SecureRandom to make it insecure, so we can generate deterministic RSA keys, only to be used for test
public class InsecureRandom extends SecureRandom {

    private Random random = new Random();

    public InsecureRandom() {
    }

    public InsecureRandom(byte[] seed) {
        random.setSeed(seed.hashCode());
    }

    @Override
    public String getAlgorithm() {
        throw new RuntimeException("Unsupported");
    }

    @Override
    public synchronized void setSeed(byte[] seed) {
        random.setSeed(seed.hashCode());
    }

    @Override
    public void setSeed(long seed) {
        if (random == null) {
            super.setSeed(seed);
        } else {
            random.setSeed(seed);
        }
    }

    @Override
    public void nextBytes(byte[] bytes) {
        random.nextBytes(bytes);
    }

    @Override
    public byte[] generateSeed(int numBytes) {
        throw new RuntimeException("Unsupported");
    }

    @Override
    public int nextInt() {
        return random.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    @Override
    public long nextLong() {
        return random.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    @Override
    public float nextFloat() {
        return random.nextFloat();
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }

    @Override
    public synchronized double nextGaussian() {
        return random.nextGaussian();
    }

    @Override
    public IntStream ints(long streamSize) {
        return random.ints(streamSize);
    }

    @Override
    public IntStream ints() {
        return random.ints();
    }

    @Override
    public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
        return random.ints(streamSize, randomNumberOrigin, randomNumberBound);
    }

    @Override
    public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        return random.ints(randomNumberOrigin, randomNumberBound);
    }

    @Override
    public LongStream longs(long streamSize) {
        return random.longs(streamSize);
    }

    @Override
    public LongStream longs() {
        return random.longs();
    }

    @Override
    public LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
        return random.longs(streamSize, randomNumberOrigin, randomNumberBound);
    }

    @Override
    public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        return random.longs(randomNumberOrigin, randomNumberBound);
    }

    @Override
    public DoubleStream doubles(long streamSize) {
        return random.doubles(streamSize);
    }

    @Override
    public DoubleStream doubles() {
        return random.doubles();
    }

    @Override
    public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
        return random.doubles(streamSize, randomNumberOrigin, randomNumberBound);
    }

    @Override
    public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        return random.doubles(randomNumberOrigin, randomNumberBound);
    }
}
