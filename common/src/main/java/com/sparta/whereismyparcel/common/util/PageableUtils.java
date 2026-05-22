package com.sparta.whereismyparcel.common.util;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableUtils {

	private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 30, 50);
	private static final int DEFAULT_PAGE_SIZE = 10;

	private PageableUtils() {
	}

	/**
	 * 허용된 페이지 크기(10·30·50)와 정렬 필드를 강제합니다.
	 * 허용 외 size → 10 고정, 허용 외 sort 필드 → defaultSort 적용.
	 */
	public static Pageable normalize(Pageable pageable, Set<String> allowedSortFields, Sort defaultSort) {
		int size = ALLOWED_PAGE_SIZES.contains(pageable.getPageSize()) ? pageable.getPageSize() : DEFAULT_PAGE_SIZE;

		List<Sort.Order> validOrders = pageable.getSort().stream()
				.filter(order -> allowedSortFields.contains(order.getProperty()))
				.toList();

		Sort sort = validOrders.isEmpty() ? defaultSort : Sort.by(validOrders);

		return PageRequest.of(pageable.getPageNumber(), size, sort);
	}
}
