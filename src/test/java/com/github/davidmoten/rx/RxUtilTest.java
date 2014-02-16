package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static rx.Observable.from;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func0;

public class RxUtilTest {

	@Test
	public void testCount() {
		Observable<Integer> c = Observable.range(1, 100);
		Observable<Integer> c2 = RxUtil.count(c, new Action1<Long>() {
			@Override
			public void call(Long count) {
				System.out.println("finished counting " + count);
			}
		});
		long count = c2.count().toBlockingObservable().single();
		System.out.println("count=" + count);
	}

	@Test
	public void testDoWhenAllComplete2() {
		Observable<Integer> o = from(1, 2, 3);
		RxUtil.print(o);
		Observable<Observable<Integer>> o2 = Observable.from(from(1, 2, 3),
				from(4, 5, 6, 7, 8, 9));
		Observable<Observable<Integer>> o3 = RxUtil.doWhenAllComplete(o2,
				new Action0() {
					@Override
					public void call() {
						System.out.println("finished test 2");
					}
				});
		RxUtil.print(Observable.merge(o3).toList());
	}

	@Test
	public void testShare() {
		final PublishSubject<Integer> subject = PublishSubject.create();
		Observable<Integer> shared = RxUtil
				.share(new Func0<Observable<Integer>>() {

					@Override
					public Observable<Integer> call() {
						return subject;
					}

				});
		final Set<String> set = new HashSet<String>();
		Subscription sub1 = shared.subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer n) {
				set.add("1-" + n);
			}
		});
		subject.onNext(1);
		subject.onNext(2);
		assertEquals(2, set.size());
		assertTrue(set.contains("1-1"));
		assertTrue(set.contains("1-2"));
		Subscription sub2 = shared.subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer n) {
				set.add("2-" + n);
			}
		});
		subject.onNext(3);
		assertEquals(4, set.size());
		assertTrue(set.contains("1-1"));
		assertTrue(set.contains("1-2"));
		assertTrue(set.contains("1-3"));
		assertTrue(set.contains("2-3"));
		sub2.unsubscribe();
		subject.onNext(4);

		assertEquals(5, set.size());
		assertTrue(set.contains("1-1"));
		assertTrue(set.contains("1-2"));
		assertTrue(set.contains("1-3"));
		assertTrue(set.contains("2-3"));
		assertTrue(set.contains("1-4"));

		sub1.unsubscribe();
		subject.onNext(5);
		assertEquals(5, set.size());

		subject.onCompleted();
	}

	@Test
	public void testRetryAllowsSubscriptionAfterAllSubscriptionsUnsubsribed() {
		final AtomicInteger subsCount = new AtomicInteger(0);
		OnSubscribeFunc<String> onSubscribe = new OnSubscribeFunc<String>() {
			@Override
			public Subscription onSubscribe(Observer<? super String> observer) {
				subsCount.incrementAndGet();
				return new Subscription() {

					@Override
					public void unsubscribe() {
						subsCount.decrementAndGet();
					}
				};
			}
		};
		Observable<String> stream = Observable.create(onSubscribe);
		Observable<String> streamWithRetry = stream.retry();
		Subscription sub = streamWithRetry.subscribe();
		assertEquals(1, subsCount.get());
		sub.unsubscribe();
		assertEquals(0, subsCount.get());
		streamWithRetry.subscribe();
		assertEquals(1, subsCount.get());
	}
}
