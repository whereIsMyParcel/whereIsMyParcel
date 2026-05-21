package com.sparta.whereismyparcel.hub.infrastructure.config;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;
import com.sparta.whereismyparcel.hub.domain.repository.HubRepository;
import com.sparta.whereismyparcel.hub.presentation.dto.request.CreateHubRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class HubDataInitializer implements ApplicationRunner {

    private final HubRepository hubRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (hubRepository.count() > 0) {
            return;
        }

        List<CreateHubRequest> hubs = List.of(
            new CreateHubRequest("서울특별시 센터", "서울특별시 중구 세종대로 110", 37.5665, 126.9780),
            new CreateHubRequest("부산광역시 센터", "부산광역시 연제구 중앙대로 1001", 35.1796, 129.0756),
            new CreateHubRequest("대구광역시 센터", "대구광역시 중구 공평로 88", 35.8714, 128.6014),
            new CreateHubRequest("인천광역시 센터", "인천광역시 남동구 정각로 29", 37.4563, 126.7052),
            new CreateHubRequest("광주광역시 센터", "광주광역시 서구 내방로 111", 35.1595, 126.8526),
            new CreateHubRequest("대전광역시 센터", "대전광역시 서구 둔산로 100", 36.3504, 127.3845),
            new CreateHubRequest("울산광역시 센터", "울산광역시 남구 중앙로 201", 35.5384, 129.3114),
            new CreateHubRequest("세종특별자치시 센터", "세종특별자치시 도움6로 11", 36.4800, 127.2890),
            new CreateHubRequest("경기도 센터", "경기도 수원시 영통구 도청로 30", 37.2636, 127.0286),
            new CreateHubRequest("강원특별자치도 센터", "강원특별자치도 춘천시 중앙로 1", 37.8854, 127.7298),
            new CreateHubRequest("충청북도 센터", "충청북도 청주시 상당구 상당로 82", 36.6358, 127.4913),
            new CreateHubRequest("충청남도 센터", "충청남도 홍성군 홍북읍 충남대로 21", 36.6588, 126.6728),
            new CreateHubRequest("전북특별자치도 센터", "전라북도 전주시 완산구 효자로 225", 35.8242, 127.1480),
            new CreateHubRequest("전라남도 센터", "전라남도 무안군 삼향읍 오룡길 1", 34.8160, 126.4629),
            new CreateHubRequest("경상북도 센터", "경상북도 안동시 풍천면 도청대로 455", 36.5760, 128.5056),
            new CreateHubRequest("경상남도 센터", "경상남도 창원시 의창구 중앙대로 300", 35.2376, 128.6924),
            new CreateHubRequest("제주특별자치도 센터", "제주특별자치도 제주시 문연로 6", 33.4890, 126.4983)
        );

        hubs.forEach(request -> hubRepository.save(Hub.create(request)));
    }
}
