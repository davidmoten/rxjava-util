package com.github.davidmoten.rx;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class RxUtilTest {
   

    // @Test
    public void test() throws InterruptedException {
        Action1<Long> work = new Action1<Long>() {

            @Override
            public void call(Long n) {
                try {
                    System.out.println("starting " + n);
                    Thread.sleep(10000);
                    System.out.println("finished " + n);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        };
        PublishSubject<Long> subject = PublishSubject.create();
        Observable
                .merge(subject, Observable.just(-1L),
                        Observable.interval(30000, TimeUnit.MILLISECONDS)).doOnNext(work)
                .subscribe();
        Thread.sleep(1000);
        subject.onNext(-2L);
        subject.onNext(-3L);
        Thread.sleep(10000);

    }

    

}
