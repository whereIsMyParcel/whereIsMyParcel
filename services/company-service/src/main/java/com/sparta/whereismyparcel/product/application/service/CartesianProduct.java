package com.sparta.whereismyparcel.product.application.service;

import com.sparta.whereismyparcel.product.domain.entity.ProductOptionValue;

import java.util.ArrayList;
import java.util.List;

public class CartesianProduct {
    // 데카르트 곱 자동 생성

    public static List<List<ProductOptionValue>> of(List<List<ProductOptionValue>> lists) {
        // 1. [[화이트, 260,] [블랙, 260]] 형태로 넣을 리스트 생성
        List<List<ProductOptionValue>> result = new ArrayList<>();

        // 2. 입력값이 비어있거나 없으면 리턴
        if (lists == null || lists.isEmpty()) {
            return result;
        }
        // 3. 재귀 시작
        // 인자 설명 (원본 2차원 리스트, 0번 행부터 시작, 데이터를 담을 일시적 리스트 생성, 결과값 리스트)
        generateCombinations(lists, 0, new ArrayList<>(), result);
        return result;
    }

    // 재귀 함수 진행 메소드
    // 지속적으로 자신을 호출해 메모리 스택에 올림
    private static void generateCombinations(List<List<ProductOptionValue>> lists, int depth,
                                             List<ProductOptionValue> current, List<List<ProductOptionValue>> result) {
        // 현재의 깊이가 원본 리스트의 총 개수와 일치하면
        // 즉 lists.size()와 같다면 맨 끝
        if (depth == lists.size()) {
            // 끝에 도달 했을때 리스트를 열어 완성된 리스트를 결과값 리스트에 저장
            result.add(new ArrayList<>(current));
            return;
        }

        for (ProductOptionValue value : lists.get(depth)) {
            // 현재 층의 값을 리스트에 담음
            current.add(value);
            // 다시 자신을 호출
            // 인덱스를 1증가시켜 다음 층으로 들어감
            generateCombinations(lists, depth + 1, current, result);

            // 다음 층을 가서 더이상 진행할 메소득 없으면 돌아와 이전의 값을 지움
            current.remove(current.size() - 1); // 백트래킹 처리
        }
    }
}
