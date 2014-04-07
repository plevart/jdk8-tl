/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author peter
 */
public class FibonacciPrimes {

    static Future<Set<BigInteger>> fibonacciPrimes(CompletableFuture<BigInteger> f0,
                                                   CompletableFuture<BigInteger> f1,
                                                   int n) {
        if (n < 0) throw new IllegalArgumentException("n should be >= 0");

        Set<BigInteger> result = new ConcurrentSkipListSet<>();
        AtomicInteger c = new AtomicInteger(n + 1);
        CompletableFuture<Set<BigInteger>> future = new CompletableFuture<>();

        Consumer<BigInteger> addIfPrime = i -> {
            if (i.isProbablePrime(100)) result.add(i);
            if (c.decrementAndGet() == 0) future.complete(result);
        };

        f0.thenAcceptAsync(addIfPrime);
        if (n > 0) {
            f1.thenAcceptAsync(addIfPrime);
            for (int i = 2; i <= n; i++) {
                f1 = f0.thenCombine(f0 = f1, BigInteger::add);
                f1.thenAcceptAsync(addIfPrime);
            }
        }

        return future;
    }

    public static void main(String[] args) throws Exception {
        CompletableFuture<BigInteger> f0 = new CompletableFuture<>();
        CompletableFuture<BigInteger> f1 = new CompletableFuture<>();

        int n = 10000;
        Future<Set<BigInteger>> primes = fibonacciPrimes(f0, f1, n);

        f0.complete(BigInteger.ZERO);
        f1.complete(BigInteger.ONE);

        System.out.println("Primes among 1st " + (n + 1) + " fibonacci numbers:");
        for (BigInteger prime : primes.get()) {
            System.out.println(prime);
        }
    }
}
