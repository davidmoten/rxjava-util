package com.github.davidmoten.rx;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static rx.Observable.from;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class RxUtilTest {

	@Test
	public void testDoWhenAllComplete2() {
		Observable<Integer> o = from(asList(1, 2, 3));
		RxUtil.print(o);
		Observable<Observable<Integer>> o2 = from(asList(from(asList(1, 2, 3)),
				from(asList(4, 5, 6, 7, 8, 9))));
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
		Observable<Integer> shared = RxUtil.share(subject);
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
						Observable.interval(30000, TimeUnit.MILLISECONDS))
				.doOnNext(work).subscribe();
		Thread.sleep(1000);
		subject.onNext(-2L);
		subject.onNext(-3L);
		Thread.sleep(10000);

	}

	@Test
	public void test2() throws InterruptedException {
		Observable.range(1, 1000).observeOn(Schedulers.newThread()).take(500)
				.doOnNext(new Action1<Integer>() {

					@Override
					public void call(Integer n) {
						try {
							Thread.sleep(20000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}).subscribe();
		Thread.sleep(300000);
	}

	@Test
	public void testUsing() {
		Func0<InputStream> resourceFactory = () -> {
			try {
				return new FileInputStream("target");
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		};
		Func1<InputStream, Observable<String>> observableFactory = is -> Observable
				.just("boo");
		Action1<InputStream> onTerminate = is -> {try {
			is.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}};
		RxUtil.using(resourceFactory, observableFactory, onTerminate);
	}
	
	
}
