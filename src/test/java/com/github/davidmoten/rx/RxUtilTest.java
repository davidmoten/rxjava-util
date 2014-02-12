package com.github.davidmoten.rx;

import static rx.Observable.from;

import org.junit.Test;

import rx.Observable;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

import com.github.davidmoten.rx.RxUtil;

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

}
