package com.sparta.whereismyparcel.hub.presentation.controller.util;

import com.sparta.whereismyparcel.hub.domain.exception.InvalidPageSizeException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginationController {

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 30, 50);

    public static void validatePageSize(Pageable pageable) {
        if (!ALLOWED_PAGE_SIZES.contains(pageable.getPageSize())) {
            throw new InvalidPageSizeException();
        }
    }
}
